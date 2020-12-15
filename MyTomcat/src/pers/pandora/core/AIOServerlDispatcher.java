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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class AIOServerlDispatcher extends Dispatcher implements CompletionHandler<Integer, Attachment> {

    Attachment att;

    @Override
    public void completed(Integer result, Attachment att) {
        server = att.getServer();
        this.att = att;
        att.setKeepAlive(true);
        if (result < 0) {
            return;
        }
        ByteBuffer buffer = att.getBuffer();
        //model change:write -> read
        buffer.flip();
        //set JSON parser
        request.setJsonParser(server.getJsonParser());
        response.setJsonParser(server.getJsonParser());
        //pre handle HTTP resource
        initRequest(buffer);
        String msg = null;
        //conten-length = all body data bytes after blank line(\n)
        try {
            msg = handleUploadFile(buffer.array(), buffer.position(), buffer.limit());
        } catch (UnsupportedEncodingException e) {
            logger.error(LOG.LOG_PRE + "handleUploadFile" + LOG.LOG_POS, server.getServerName(), LOG.EXCEPTION_DESC, e);
        }
        try {
            dispatcher(msg);
        } catch (RuntimeException e) {
            logger.error(LOG.LOG_PRE + "dispatcher" + LOG.LOG_POS, server.getServerName(), LOG.EXCEPTION_DESC, e);
        }
        //after HTTP request completed, and before close the tcp connection
        handleRequestCompleted();
        response.reset();
        buffer.clear();
        att.setKeepAlive(false);
        server.slavePool.submit(() -> {
            try {
                //for the size is over 1M files to wait a time for receiving all datas,the time should determined by bandwidth
                Thread.sleep(server.getWaitReceivedTime());
                completed(att.getClient().read(buffer).get(), att);
            } catch (InterruptedException | ExecutionException e) {
                //writePendingExceptiion
                failed(e, att);
            }
        });
    }

    //firefox
    //---------------------------302099603737780221121456441554
    //-----------------------------302099603737780221121456441554
    //-----------------------------302099603737780221121456441554--
    //chrome
    //----WebKitFormBoundarysB2AvyXaNzrZAIau
    //------WebKitFormBoundarysB2AvyXaNzrZAIau
    //------WebKitFormBoundarysB2AvyXaNzrZAIau--
    private String handleUploadFile(byte[] data, int i, int limit) throws UnsupportedEncodingException {
        //text/plain send the constant string WebKitFormBoundaryvZnw9hoqtB3dl2ak as the separator for chrome,firefox
        //for jpg file ,it has the highlight mark: xffxd8 --- xffxd9
        String msg = new String(data, i, limit, request.getCharset());
//        logger.info("receive datas from the client:"+LOG.LOG_PRE, msg);
        int j = msg.indexOf(HTTPStatus.FILEMARK), k, l, start, end, sideLen, len, offset = 0, jj;
        String head = msg, fileDesc, tmp = LOG.NO_CHAR;
        if (msg.startsWith(HTTPStatus.GET) || msg.startsWith(HTTPStatus.POST) ||
                msg.startsWith(HTTPStatus.PUT) || msg.startsWith(HTTPStatus.DELETE) || msg.startsWith(HTTPStatus.OPTIONS)) {
            request.reset();
        }
        if (j >= 0) {
            j += HTTPStatus.FILEMARK.length();
            jj = j;
            for (; j < msg.length() && msg.charAt(j) != HTTPStatus.CRLF; j++) ;
            //get file separator
            fileDesc = msg.substring(jj, j - HTTPStatus.LINE_SPLITER + 1);
            request.setFileDesc(fileDesc);
            tmp = msg.substring(0, j);
            offset = tmp.getBytes(request.getCharset()).length;
            msg = msg.substring(j);
        }
        if (StringUtils.isNotEmpty(request.getFileDesc())) {
            request.setMultipart(true);
            fileDesc = request.getFileDesc();
            boolean isFile = false;
            String part, varName, fileName, contentType, sideWindow, varValue;
            String fileType[];
            byte[] fileData;
            List<Object> objects;
            j = msg.indexOf(fileDesc);
            if (j >= 0) {
                head = tmp + msg.substring(0, Math.max(0, j - HTTPStatus.MUPART_DESC_LINE.length() - HTTPStatus.LINE_SPLITER));
            }
            while (j >= 0) {
                j += fileDesc.length() + HTTPStatus.LINE_SPLITER;
                if (!isFile) {
                    offset += msg.substring(0, j).getBytes(request.getCharset()).length;
                }
                msg = msg.substring(j);
                j = msg.indexOf(fileDesc);
                isFile = false;
                if (j >= 0) {
                    part = msg.substring(0, j - HTTPStatus.MUPART_DESC_LINE.length() - HTTPStatus.LINE_SPLITER + 1);
                    jj = part.indexOf(HTTPStatus.MUPART_NAME);
                    if (jj > 0) {
                        jj += HTTPStatus.MUPART_NAME.length() + 1;
                        for (k = jj; k < part.length() && part.charAt(k) != HTTPStatus.FILENAMETAIL; k++) ;
                        varName = part.substring(jj, k);
                        if ((jj = part.indexOf(HTTPStatus.FILENAME, jj)) > 0) {//binary data
                            jj += HTTPStatus.FILENAME.length() + 1;
                            for (k = jj; k < part.length() && part.charAt(k) != HTTPStatus.FILENAMETAIL; k++) ;
                            fileName = part.substring(jj, k);
                            k += HTTPStatus.LINE_SPLITER;
                            for (; k < part.length() && part.charAt(k) != HTTPStatus.CRLF; k++) ;
                            for (l = ++k; k < part.length() && part.charAt(k) != HTTPStatus.CRLF; k++) ;
                            //get file type
                            contentType = part.substring(l, k - HTTPStatus.LINE_SPLITER + 1);
                            if (StringUtils.isNotEmpty(contentType)) {
                                fileType = contentType.split(HTTPStatus.HEAD_INFO_SPLITER);
                                if (fileType.length == 2) {
                                    end = j - HTTPStatus.MUPART_DESC_LINE.length() - HTTPStatus.LINE_SPLITER;
                                    start = k + 1 + HTTPStatus.LINE_SPLITER;
                                    if (fileType[1].trim().equals(HTTPStatus.TEXT_PLAIN)) {
                                        end = end - HTTPStatus.LINE_SPLITER + 1;
                                        end = offset + part.substring(0, end).getBytes(request.getCharset()).length + part.substring(end, end + 1).
                                                getBytes(request.getCharset()).length;
                                        start = offset + part.substring(0, start).getBytes(request.getCharset()).length;
                                    } else {
                                        start = offset + part.substring(0, start).getBytes(request.getCharset()).length;
                                        sideWindow = HTTPStatus.CRLF + HTTPStatus.MUPART_DESC_LINE + fileDesc;
                                        sideLen = sideWindow.getBytes(request.getCharset()).length;
                                        for (end = start; end < limit; end++) {
                                            if (new String(data, end, sideLen, request.getCharset()).equals(sideWindow)) {
                                                offset = end + sideLen + HTTPStatus.LINE_SPLITER;
                                                end = end - HTTPStatus.LINE_SPLITER + 1;//in windows,\n should be the \r\n
                                                isFile = true;
                                                break;
                                            }
                                        }
                                    }
                                    len = end - start;
                                    if (len >= 0 && end <= limit) {
                                        //copy file data
                                        fileData = new byte[len];
                                        System.arraycopy(data, start, fileData, 0, len);
                                        Tuple<String, String, byte[]> file = new Tuple<>(fileName, fileType[1].trim(), fileData);
                                        request.getUploadFiles().put(varName, file);//the same varname just save one file
                                    }
                                }
                            }
                        } else {//binary form data for variables
                            k += HTTPStatus.LINE_SPLITER;
                            varValue = part.substring(k);
                            objects = request.getParams().get(varName);
                            if (objects != null) {
                                objects.add(varValue);
                            } else {
                                objects = new ArrayList<>();
                                objects.add(varValue.trim());
                                request.getParams().put(varName, objects);
                            }
                        }
                    }
                    if (!isFile) {
                        offset += msg.substring(0, j).getBytes(request.getCharset()).length;
                    }
                    msg = msg.substring(j);
                    j = msg.indexOf(fileDesc);
                }
            }
        }
        return request.isFlag() ? HTTPStatus.POST : head;
    }

    @Override
    public void failed(Throwable t, Attachment att) {
        //WritePendingException or Connection_Closed_Exception
        server.close(att, this);
    }

    @Override
    protected void pushClient(byte[] content, java.io.File staticFile) {
        if (content != null) {
            att.getBuffer().clear();
            att.getBuffer().put(content);
            att.getBuffer().flip();
            att.getClient().write(att.getBuffer());
            att.getBuffer().clear();
            //Files.readAllBytes(Patrhs.get("./WebRoot" + staticFile)
            FileInputStream in = null;
            if (staticFile != null) {
                try {
                    in = new FileInputStream(staticFile);
                    FileChannel fin = in.getChannel();
                    ByteBuffer by = ByteBuffer.allocateDirect(server.getResponseBuffer());
                    while (fin.read(by) != -1) {
                        by.flip();
                        try {
                            att.getClient().write(by).get();
                        } catch (InterruptedException | ExecutionException e) {
                            logger.error(LOG.LOG_PRE + "pushClient read I/O" + LOG.LOG_POS,
                                    server.getServerName(), staticFile.getAbsolutePath(),
                                    LOG.EXCEPTION_DESC, e);
                        }
                        by.compact();
                    }
                } catch (IOException e) {
                    logger.error(LOG.LOG_PRE + "pushClient read I/O" + LOG.LOG_POS, server.getServerName(), staticFile.getAbsolutePath(),
                            LOG.EXCEPTION_DESC, e);
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.error(LOG.LOG_PRE + "pushClient I/O Stream close" + LOG.LOG_POS,
                                server.getServerName(), LOG.EXCEPTION_DESC, e);
                    }
                }
                att.getBuffer().clear();
            }
        }
    }

}
