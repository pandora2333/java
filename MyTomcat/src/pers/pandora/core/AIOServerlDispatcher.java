package pers.pandora.core;

import pers.pandora.constant.LOG;
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
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class AIOServerlDispatcher extends Dispatcher implements CompletionHandler<Integer, Attachment> {

    private Attachment att;

    private String msg;

    private long remain;

    private boolean fileFail;

    @Override
    public void completed(Integer result, Attachment att) {
        server = att.getServer();
        this.att = att;
        if (result < 0) {
            //The identifier at the end of the packet indicates that the TCP request packet has been sent
            //The TCP it can not be closed immediately. The browser may be reused next time, so it needs to be kept alive
            return;
        }
        fileFail = false;
        att.setKeepTime(Instant.now());
        setKeepAlive(att.isKeep());
        ByteBuffer buffer = att.getReadBuffer();
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
        } catch (UnsupportedEncodingException e) {
            logger.error(LOG.LOG_PRE + "handleUploadFile" + LOG.LOG_POS, server.getServerName(), LOG.EXCEPTION_DESC, e);
        }
        if (remain <= 0 || !fileFail) {
            if (!fileFail) {
                request.getUploadFiles().clear();
            }
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
            remain = 0;
        }
        buffer.clear();
        server.slavePool.submit(() -> {
            try {
                //for the size is over 1M files to wait a time for receiving all datas,the time should determined by bandwidth
                Thread.sleep(server.getWaitReceivedTime());
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
            String contentType = request.getHeads().get(HTTPStatus.CONTENTTYPE);
            if (StringUtils.isNotEmpty(contentType)) {
                k = contentType.indexOf(HTTPStatus.FILEMARK);
                if (k >= 0) fileSeparator = contentType.substring(k + HTTPStatus.FILEMARK.length());
                if (StringUtils.isNotEmpty(fileSeparator)) {
                    request.setFileDesc(fileSeparator);
                }
            }
            String dataSize = request.getHeads().get(HTTPStatus.CONTENTLENGTH);
            remain = StringUtils.isNotEmpty(dataSize) ? Long.valueOf(dataSize) : 0;
        }
        remain += -limit + i;
        if (StringUtils.isNotEmpty(request.getFileDesc())) {
            request.setMultipart(true);
            String fileDesc, sideWindow;
            fileDesc = HTTPStatus.MUPART_DESC_LINE + request.getFileDesc();
            sideWindow = HTTPStatus.CRLF + fileDesc;
            int len = HTTPStatus.MUPART_DESC_LINE.length() + request.getFileDesc().length();
            if (i + len <= limit && fileDesc.equals(new String(data, i, len, request.getCharset()))) {
                i += len + HTTPStatus.LINE_SPLITER;
                String query, varName, varValue, fileName, fileType;
                byte[] fileData;
                List<Object> objects;
                boolean isFile;
                for (; i < limit && !fileFail; ) {
                    isFile = false;
                    for (j = i; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
                    if (j == limit) {
                        fileFail = true;
                        break;
                    }
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
                            if (j == limit) {
                                fileFail = true;
                                break;
                            }
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
                                fileData = new byte[k];
                                System.arraycopy(data, i, fileData, 0, k);
                                Tuple<String, String, byte[]> file = new Tuple<>(fileName, fileType, fileData);
                                request.getUploadFiles().put(varName, file);//the same varname just save one file
                            } else fileFail = true;
                            i = j + HTTPStatus.LINE_SPLITER;
                        } else {//form value
                            i += HTTPStatus.LINE_SPLITER;
                            for (j = i; j < limit && data[j] != HTTPStatus.CRLF; j++) ;
                            if (j == limit) {
                                fileFail = true;
                                break;
                            }
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
                    } else fileFail = true;
                    i += len + HTTPStatus.LINE_SPLITER;
                    if (i > limit) fileFail = true;
                    if (i == limit || (i + 4 == limit && HTTPStatus.MUPART_DESC_LINE.equals(new String(data, i, 2))))
                        break;
                }
            } else fileFail = true;
        } else if (StringUtils.isNotEmpty(msg)) msg += HTTPStatus.CRLF + new String(data, i, limit - i);
        //For the server, a TCP connection can process multiple requests in batches, but only one request can be processed simultaneously
        return remain <= 0;
    }

    @Override
    public void failed(Throwable t, Attachment att) {
        //WritePendingException or Connection_Closed_Exception
        server.close(att, this);
    }

    @Override
    protected void pushClient(byte[] content, java.io.File staticFile) {
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
            FileInputStream in = null;
            if (staticFile != null) {
                by = att.getWriteBuffer();
                try {
                    in = new FileInputStream(staticFile);
                    FileChannel fin = in.getChannel();
                    while (fin.read(by) != -1) {
                        by.flip();
                        try {
                            if (att.getClient().isOpen()) {
                                att.getClient().write(by).get();
                            } else {
                                return;
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            //The write operation is abnormal. The last IO operation of the underlying tcpsocket is still occurring, and the Current IO operation is interrupted
                            logger.error(LOG.LOG_PRE + "pushClient read I/O" + LOG.LOG_POS,
                                    server.getServerName(), staticFile.getAbsolutePath(),
                                    LOG.EXCEPTION_DESC, e);
                            break;
                        } finally {
                            by.compact();
                        }
                    }
                } catch (IOException e) {
                    //ignore
                    //ClosedException
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }
    }

}
