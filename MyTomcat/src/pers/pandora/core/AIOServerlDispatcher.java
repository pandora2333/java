package pers.pandora.core;

import pers.pandora.constant.LOG;
import pers.pandora.exception.OverMaxUpBitsException;
import pers.pandora.vo.Tuple;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.utils.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public final class AIOServerlDispatcher extends Dispatcher implements CompletionHandler<Integer, Attachment> {

    private Attachment att;

    private String msg;

    private long remain;

    private byte[] fileData;
    //support http 206
    private Map<String, FileChannel> ins = new HashMap<>(4);

    @Override
    public void completed(Integer result, Attachment att) {
        server = att.getServer();
        this.att = att;
        if (result < 0) {
            //The identifier at the end of the packet indicates that the TCP request packet has been sent
            //The TCP it can not be closed immediately. The browser may be reused next time, so it needs to be kept alive if it is keepAlive pattern
            att.setKeepTime(Instant.now());
            att.setUsed(false);
            return;
        }
        att.setUsed(true);
        final ByteBuffer buffer = att.getReadBuffer();
        //model change:write -> read
        buffer.flip();
        //set JSON_TYPE parser
        request.setJsonParser(server.jsonParser);
        response.setJsonParser(server.jsonParser);
        //pre handle HTTP resource
        initRequest(buffer);
        boolean exceptedOne = false;
        //content-length = all body data bytes after blank line(\n)
        //It's supported pipeLining for HTTP ,but at least,it has one complete HTTP request header,and it will be processed
        while (buffer.position() < buffer.limit()) {
            try {
                initRequest(buffer.array(), buffer.position(), buffer.limit());
            } catch (Exception e) {
                logger.error(LOG.LOG_PRE + "handleUploadFile" + LOG.LOG_POS, server.serverName, LOG.EXCEPTION_DESC, e);
                if (e instanceof OverMaxUpBitsException) {
                    reset();
                    return;
                }
            }
            if (remain == 0) {
                try {
                    dispatcher(msg);
                } catch (RuntimeException e) {
                    logger.error(LOG.LOG_PRE + "dispatcher" + LOG.LOG_POS, server.serverName, LOG.EXCEPTION_DESC, e);
                }
                //after HTTP request completed, and before close the tcp connection
                handleRequestCompleted();
                if (server.close(att, this, null)) {
                    return;
                }
                request.reset();
                response.reset();
                reset();
            } else if (remain < 0) {
                if (exceptedOne) {
                    exceptedOne = att.isKeep();
                    //If the HTTP header is incomplete more than twice, we have reason to believe that the receivebuffer of the current HTTP request is too small to receive the HTTP header completely
                    att.setKeep(false);
                    try {
                        server.close(att, this, exceptedOne ? att.getClient().getRemoteAddress().toString() : null);
                    } catch (IOException e) {
                        //ignore
                    }
                    return;
                }
                exceptedOne = true;
                reset();
            }
        }
        buffer.clear();
        server.slavePool.submit(() -> {
            try {
                completed(att.getClient().read(buffer).get(), att);
            } catch (InterruptedException | ExecutionException e) {
                //AsynchronousCloseException,and when channel is closed,it must cause when tcp-gc or browser shutdown input stream
                //ignore
            }
        });
    }

    private void reset() {
        remain = 0;
        msg = null;
        fileData = null;
    }

    private void initRequest(byte[] data, int i, int limit) {
        int j, k, subLen = -HTTPStatus.LINE_SPLITER + 1;
        final Charset charset = Charset.forName(request.getCharset());
        if (remain == 0) {
            boolean ok = false;
            j = 0;
            for (; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
            msg = new String(data, i, j - i + subLen, charset);
            //currently only handle HTTP/1.0 and HTTP/1.1,and it's not supported for SSL/TLS
            if (!msg.endsWith(HTTPStatus.HTTP1_1) && !msg.endsWith(HTTPStatus.HTTP1_0)) {
                remain = -1;
                return;
            }
            String key, value, fileSeparator = null;
            for (j++, i = j; j < limit; i = ++j) {
                key = null;
                for (; j < limit && data[j] != HTTPStatus.CRLF; j++)
                    if (key == null && data[j] == HTTPStatus.COLON) {
                        key = new String(data, i, j - i, charset);
                        i = j + 1;
                    }
                value = new String(data, i, j - i + subLen, charset);
                if (StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value))
                    request.getHeads().put(key, value.trim());
                if (j + HTTPStatus.LINE_SPLITER < limit && data[j + HTTPStatus.LINE_SPLITER] == HTTPStatus.CRLF) {
                    i = j + HTTPStatus.LINE_SPLITER + 1;
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                //The request header information is incomplete. This request is discarded
                remain = -1;
                att.setReadBuffer((ByteBuffer) att.getReadBuffer().position(limit));
                return;
            }
            //long connection or short connection
            if (!att.isKeep()) {
                String connection = request.getHeads().get(HTTPStatus.CONNECTION);
                if (StringUtils.isNotEmpty(connection) && !connection.equalsIgnoreCase(HTTPStatus.CLOSE)) {
                    try {
                        server.addClients(att.getClient().getRemoteAddress().toString(), att);
                        att.setKeep(true);
                        setKeepAlive(true);
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
            //cookie and session init
            boolean initSession = false;
            String cookie = request.getHeads().get(HTTPStatus.COOKIE_MARK);
            if (StringUtils.isNotEmpty(cookie)) {
                request.initCookies(cookie);
                initSession = true;
            }
            if (!initSession && (request.getSession() == null || !request.checkSessionInvalid(request.getSession().getSessionID())))
                request.initSession();
            //handle files and variable
            final String contentType = request.getHeads().get(HTTPStatus.CONTENTTYPE);
            if (StringUtils.isNotEmpty(contentType)) {
                k = contentType.indexOf(HTTPStatus.FILEMARK);
                if (k >= 0) fileSeparator = contentType.substring(k + HTTPStatus.FILEMARK.length());
                if (StringUtils.isNotEmpty(fileSeparator)) {
                    request.setFileDesc(fileSeparator);
                    request.setMultipart(true);
                }
            }
            final String dataSize = request.getHeads().get(HTTPStatus.CONTENTLENGTH);
            remain = StringUtils.isNotEmpty(dataSize) ? Long.valueOf(dataSize) : 0;
            //Directly refuse to receive data after exceeding the maximum transmission bit
            if (remain > server.getMaxUpBits()) {
                final OverMaxUpBitsException exception = new OverMaxUpBitsException(Server.OVERMAXUPBITS);
                String ip = null;
                try {
                    ip = att.getClient().getRemoteAddress().toString();
                } catch (IOException e) {
                    //ignore
                }
                att.setKeep(false);
                server.close(att, this, ip);
                throw exception;
            }
        }
        int len = (int) Math.min(limit - i, remain);
        byte[] tmpData;
        if (len > 0) {
            tmpData = new byte[(fileData != null ? fileData.length : 0) + len];
            if (fileData != null) System.arraycopy(fileData, 0, tmpData, 0, fileData.length);
            System.arraycopy(data, i, tmpData, fileData != null ? fileData.length : 0, len);
            fileData = tmpData;
        }
        att.setReadBuffer((ByteBuffer) att.getReadBuffer().position((int) Math.min(i + remain, limit)));
        remain -= len;
        if (StringUtils.isNotEmpty(request.getFileDesc()) && remain == 0) {
            i = 0;
            limit = fileData.length;
            data = fileData;
            String fileDesc, sideWindow;
            fileDesc = HTTPStatus.MUPART_DESC_LINE + request.getFileDesc();
            sideWindow = HTTPStatus.CRLF + fileDesc;
            len = HTTPStatus.MUPART_DESC_LINE.length() + request.getFileDesc().length();
            i += len + HTTPStatus.LINE_SPLITER;
            String query, varName, varValue, fileName, fileType;
            List<Object> objects;
            Tuple<String, String, byte[]> file;
            List<Tuple<String, String, byte[]>> tuple;
            boolean isFile;
            final Map<String, List<Object>> params = request.getParams();
            final Map<String, List<Tuple<String, String, byte[]>>> uploadFiles = request.getUploadFiles();
            for (; i < limit; ) {
                isFile = false;
                for (j = i; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
                query = new String(data, i, j - i + subLen, charset);
                i = j + 1;
                k = query.indexOf(HTTPStatus.MUPART_NAME);
                if (k > 0) {
                    k += HTTPStatus.MUPART_NAME.length() + 1;
                    for (j = k; j < query.length() && query.charAt(j) != HTTPStatus.FILENAMETAIL; j++) ;
                    varName = query.substring(k, j);
                    k = query.indexOf(HTTPStatus.FILENAME);
                    if (k > 0) {//file
                        j = k + HTTPStatus.FILENAME.length() + 1;
                        for (k = j; k < query.length() && query.charAt(k) != HTTPStatus.FILENAMETAIL; k++) ;
                        fileName = query.substring(j, k);
                        i += HTTPStatus.CONTENTTYPE.length() + 1;
                        for (j = i; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
                        //get file type
                        fileType = new String(data, i, j - i + subLen, charset).trim();
                        i = j + 1 + HTTPStatus.LINE_SPLITER;
                        for (j = i; j < limit; j++) {
                            if (data[j] != HTTPStatus.CRLF) continue;
                            if (new String(data, j, sideWindow.length(), charset).equals(sideWindow)) {
                                j += subLen;
                                isFile = true;
                                break;
                            }
                        }
                        if (isFile) {
                            k = j - i;
                            if (StringUtils.isNotEmpty(fileName)) {
                                //copy file data
                                tmpData = new byte[k];
                                System.arraycopy(data, i, tmpData, 0, k);
                                file = new Tuple<>(fileName, fileType, tmpData);
                                tuple = uploadFiles.computeIfAbsent(varName, k1 -> new LinkedList<>());
                                tuple.add(file);
                            }
                        }
                        i = j + HTTPStatus.LINE_SPLITER;
                    } else {//form value
                        i += HTTPStatus.LINE_SPLITER;
                        for (j = i; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
                        varValue = new String(data, i, j - i + subLen, charset);
                        objects = params.get(varName);
                        if (objects != null) objects.add(varValue);
                        else {
                            objects = new LinkedList<>();
                            objects.add(varValue);
                            params.put(varName, objects);
                        }
                        i = j + 1;
                    }
                }
                i += len + HTTPStatus.LINE_SPLITER;
                if (i == limit || (i + 2 + HTTPStatus.LINE_SPLITER == limit && HTTPStatus.MUPART_DESC_LINE.equals(new String(data, i, 2))))
                    break;
            }
        } else if (!StringUtils.isNotEmpty(request.getFileDesc()))
            msg += HTTPStatus.CRLF + new String(data, i, limit - i);
    }

    @Override
    public void failed(Throwable t, Attachment att) {
        //ignore
    }

    @Override
    protected void pushClient(final byte[] content, final java.io.File staticFile) {
        if (content != null) {
            if (att.getWriteBuffer().capacity() < content.length) {
                att.setWriteBuffer(ByteBuffer.allocateDirect(content.length));
            }
            ByteBuffer by = att.getWriteBuffer();
            by.put(content);
            by.flip();
            try {
                att.getClient().write(by).get();
            } catch (InterruptedException | ExecutionException e) {
                //ignore
                //ClosedChannelException,it comes from browser's input stream was closed or tcp-gc collection
            }
            by.clear();
            //Files.readAllBytes(Patrhs.get("./WebRoot" + staticFile)
            FileChannel fin = null;
            if (staticFile != null && att.getClient().isOpen()) {
                final boolean part = response.getCode() == HTTPStatus.CODE_206;
                final String file = staticFile.getAbsolutePath();
                try {
                    if (part) {
                        fin = ins.get(file);
                        if (fin == null) {
                            fin = new FileInputStream(staticFile).getChannel();
                            ins.put(file, fin);
                        }
                    } else {
                        fin = new FileInputStream(staticFile).getChannel();
                    }
                    assert fin != null;
                    if (!part) {
                        while (fin.read(by) != -1) {
                            by.flip();
                            if (!write(by, file)) {
                                break;
                            }
                            by.clear();
                        }
                    } else {
                        if (response.getLen() != by.capacity()) {
                            if (by.capacity() < response.getLen()) {
                                att.setWriteBuffer(ByteBuffer.allocateDirect((int) response.getLen()));
                                by = att.getWriteBuffer();
                            } else {
                                by = ByteBuffer.allocateDirect((int) response.getLen());
                            }
                        }
                        fin.read(by, response.getStart());
                        by.flip();
                        write(by, file);
                    }
                } catch (IOException e) {
                    //ignore
                    //ClosedChannelException,it comes from browser's input stream was closed or tcp-gc collection
                }
                if (fin != null && (!part || response.getEnd() == response.getTotal() - 1)) {
                    try {
                        fin.close();
                        if (part) {
                            ins.remove(file);
                        }
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }
    }

    public boolean write(final ByteBuffer by, final String file) {
        try {
            if (att.getClient().isOpen()) {
                att.getClient().write(by).get();
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            //The write operation is abnormal. The last IO operation of the underlying tcp-socket is still occurring, and the Current IO operation is interrupted
            logger.error(LOG.LOG_PRE + "I/O write" + LOG.LOG_POS,
                    server.getServerName(), file,
                    LOG.EXCEPTION_DESC, e);
            return false;
        }
        return true;
    }

}
