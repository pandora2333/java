package pers.pandora.core;

import pers.pandora.constant.LOG;
import pers.pandora.exception.OverMaxUpBitsException;
import pers.pandora.vo.Tuple;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.utils.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static javax.swing.UIManager.put;

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
            //The TCP it can not be closed immediately. The browser may be reused next time, so it needs to be kept alive
            return;
        }
        att.setKeepTime(Instant.now());
        setKeepAlive(att.isKeep());
        final ByteBuffer buffer = att.getReadBuffer();
        //model change:write -> read
        buffer.flip();
        //set JSON_TYPE parser
        request.setJsonParser(server.getJsonParser());
        response.setJsonParser(server.getJsonParser());
        //pre handle HTTP resource
        initRequest(buffer);
        //conten-length = all body data bytes after blank line(\n)
        try {
            initRequest(buffer.array(), buffer.position(), buffer.limit());
        } catch (Exception e) {
            logger.error(LOG.LOG_PRE + "handleUploadFile" + LOG.LOG_POS, server.getServerName(), LOG.EXCEPTION_DESC, e);
            if (e instanceof OverMaxUpBitsException) return;
        }
        if (remain <= 0) {
            try {
                dispatcher(msg);
            } catch (RuntimeException e) {
                logger.error(LOG.LOG_PRE + "dispatcher" + LOG.LOG_POS, server.getServerName(), LOG.EXCEPTION_DESC, e);
            }
            //after HTTP request completed, and before close the tcp connection
            handleRequestCompleted();
            request.reset();
            response.reset();
            msg = null;
            fileData = null;
            remain = 0;
        }
        buffer.clear();
        server.slavePool.submit(() -> {
            try {
                completed(att.getClient().read(buffer).get(), att);
            } catch (InterruptedException | ExecutionException e) {
                //AsyncClosedException,and when channel is closed,it must cause
                //ignore
            }
        });
    }

    private boolean initRequest(byte[] data, int i, int limit) throws UnsupportedEncodingException {
        int j, k, subLen = -HTTPStatus.LINE_SPLITER + 1;
        if (remain == 0) {
            j = 0;
            for (; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
            msg = new String(data, i, j - i + subLen, request.getCharset());
            String key, value, fileSeparator = null;
            for (j++, i = j; j < limit; i = ++j) {
                key = null;
                for (; j < limit && data[j] != HTTPStatus.CRLF; j++)
                    if (key == null && data[j] == HTTPStatus.COLON) {
                        key = new String(data, i, j - i, request.getCharset());
                        i = j + 1;
                    }
                value = new String(data, i, j - i + subLen, request.getCharset());
                if (StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value))
                    request.getHeads().put(key, value.trim());
                if (j + HTTPStatus.LINE_SPLITER < limit && data[j + HTTPStatus.LINE_SPLITER] == HTTPStatus.CRLF) {
                    i = j + HTTPStatus.LINE_SPLITER + 1;
                    break;
                }
            }
            //cookie and session init
            boolean initSession = false;
            String cookie = request.getHeads().get(HTTPStatus.COOKIE_MARK);
            if (StringUtils.isNotEmpty(cookie)) {
                request.initCookies(cookie);
                initSession = true;
            }
            if (!initSession && (request.getSession() == null || !request.checkSessionInvalid(request.getSession().getSessionID()))) {
                request.initSession();
            }
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
                OverMaxUpBitsException exception = new OverMaxUpBitsException(Server.OVERMAXUPBITS);
                failed(exception, att);
                throw exception;
            }
        }
        int len = limit - i;
        byte[] tmpData;
        if (len > 0) {
            tmpData = new byte[(fileData != null ? fileData.length : 0) + len];
            if (fileData != null) System.arraycopy(fileData, 0, tmpData, 0, fileData.length);
            System.arraycopy(data, i, tmpData, fileData != null ? fileData.length : 0, len);
            fileData = tmpData;
        }
        remain -= len;
        if (StringUtils.isNotEmpty(request.getFileDesc()) && remain <= 0) {
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
            boolean isFile;
            for (; i < limit; ) {
                isFile = false;
                for (j = i; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
                query = new String(data, i, j - i + subLen, request.getCharset());
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
                        fileType = new String(data, i, j - i + subLen, request.getCharset()).trim();
                        i = j + 1 + HTTPStatus.LINE_SPLITER;
                        for (j = i; j < limit; j++) {
                            if (data[j] != HTTPStatus.CRLF) continue;
                            if (new String(data, j, sideWindow.length(), request.getCharset()).equals(sideWindow)) {
                                j += subLen;
                                isFile = true;
                                break;
                            }
                        }
                        if (isFile) {
                            k = j - i;
                            //copy file data
                            tmpData = new byte[k];
                            System.arraycopy(data, i, tmpData, 0, k);
                            Tuple<String, String, byte[]> file = new Tuple<>(fileName, fileType, tmpData);
                            List<Tuple<String, String, byte[]>> tuple = request.getUploadFiles().computeIfAbsent(varName, k1 -> new ArrayList<>(1));
                            tuple.add(file);
                        }
                        i = j + HTTPStatus.LINE_SPLITER;
                    } else {//form value
                        i += HTTPStatus.LINE_SPLITER;
                        for (j = i; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
                        varValue = new String(data, i, j - i + subLen, request.getCharset());
                        objects = request.getParams().get(varName);
                        if (objects != null) {
                            objects.add(varValue);
                        } else {
                            objects = new ArrayList<>(1);
                            objects.add(varValue);
                            request.getParams().put(varName, objects);
                        }
                        i = j + 1;
                    }
                }
                i += len + HTTPStatus.LINE_SPLITER;
                if (i == limit || (i + 4 == limit && HTTPStatus.MUPART_DESC_LINE.equals(new String(data, i, 2)))) break;
            }
        } else if (!StringUtils.isNotEmpty(request.getFileDesc()))
            msg += HTTPStatus.CRLF + new String(data, i, limit - i);
        //For the server, a TCP connection can process multiple requests in batches, but only one request can be processed simultaneously
        return remain <= 0;
    }

    @Override
    public void failed(Throwable t, Attachment att) {
        //WritePendingException or Connection_Closed_Exception
        server.close(att, this);
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
                if (att.getClient().isOpen()) {
                    att.getClient().write(by).get();
                } else {
                    return;
                }
            } catch (InterruptedException | ExecutionException e) {
                //ignore
                //ClosedException
            }
            by.compact();
            //Files.readAllBytes(Patrhs.get("./WebRoot" + staticFile)
            FileChannel fin;
            FileInputStream in = null;
            if (staticFile != null) {
                by = att.getWriteBuffer();
                boolean part = response.getCode() == HTTPStatus.CODE_206;
                try {
                    if (part) {
                        fin = ins.get(staticFile.getAbsolutePath());
                        if (fin == null) {
                            fin = (in = new FileInputStream(staticFile)).getChannel();
                            ins.put(staticFile.getAbsolutePath(), fin);
                        }
                    } else {
                        fin = (in = new FileInputStream(staticFile)).getChannel();
                    }
                    assert fin != null;
                    if (!part) {
                        while (fin.read(by) != -1) {
                            by.flip();
                            if (!write(by, staticFile.getAbsolutePath())) {
                                break;
                            }
                            by.compact();
                        }
                    } else {
                        by = ByteBuffer.allocateDirect((int) response.getLen());
                        fin.read(by, response.getStart());
                        by.flip();
                        write(by, staticFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    //ignore
                    //ClosedException
                }
                if (in != null && (!part || response.getEnd() == response.getTotal() - 1)) {
                    try {
                        in.close();
                        if (part) {
                            ins.remove(staticFile.getAbsolutePath());
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
            //The write operation is abnormal. The last IO operation of the underlying tcpsocket is still occurring, and the Current IO operation is interrupted
            logger.error(LOG.LOG_PRE + "I/O write" + LOG.LOG_POS,
                    server.getServerName(), file,
                    LOG.EXCEPTION_DESC, e);
            return false;
        }
        return true;
    }

}
