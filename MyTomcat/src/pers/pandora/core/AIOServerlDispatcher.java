package pers.pandora.core;

import pers.pandora.constant.LOG;
import pers.pandora.vo.Attachment;
import pers.pandora.vo.Tuple;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.utils.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class AIOServerlDispatcher extends Dispatcher implements CompletionHandler<Integer, Attachment> {

    Attachment att;

    @Override
    public void completed(Integer result, Attachment att) {
        if (att.isReadMode()) {
            server = att.getServer();
            this.att = att;
            ByteBuffer buffer = att.getBuffer();
            buffer.flip();
            byte bytes[] = new byte[buffer.limit()];
            buffer.get(bytes);
            //HTTP资源预处理
            initRequest(bytes);
            String msg = null;
            //firefox对于较大文件会分片发送，即使buffer没满，带宽足够，而chrome会尽可能的一次发送所有数据
            //对于conten-length的长度所指内容是对于文件分隔符之间的所有字段及文件内容值以及其它表单字段所有值以及两者间换行分隔符
            //某些时候浏览器不发送文件只发送文件分隔符，推测与服务器环境有关（带宽），对于隐私模式下的chrome，上传文件不发送文件数据,在头部信息中有文件分隔符,而firefox却可以发送
            try {
                msg = handleUploadFile(bytes);
            } catch (UnsupportedEncodingException e) {
                logger.error(LOG.LOG_PRE + "handleUploadFile" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
            }
            try {
                dispatcher(msg);
            } catch (Exception e) {
                logger.error(LOG.LOG_PRE + "dispatcher" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
            }
            att.setReadMode(false);
            try {
                //资源回收处理，关闭连接前 (比如自主关闭或者浏览器突然关闭窗口）
                handleRequestCompleted();
                logger.info(LOG.LOG_PRE + "is closed!", this.getClass().getName(), att.getClient().getRemoteAddress());
                if (att.getClient().isOpen()) {
                    att.getClient().close();
                }
            } catch (IOException e) {
                logger.error(LOG.LOG_PRE + "close client" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
            }
        } else {
            try {
                if (att.getClient().isOpen()) {
                    att.getClient().close();
                }
            } catch (IOException e) {
                logger.error(LOG.LOG_PRE + "close client" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
            }
        }
    }

    //firefox
    //---------------------------302099603737780221121456441554
    //-----------------------------302099603737780221121456441554
    //-----------------------------302099603737780221121456441554--
    //chrome
    //----WebKitFormBoundarysB2AvyXaNzrZAIau
    //------WebKitFormBoundarysB2AvyXaNzrZAIau
    //------WebKitFormBoundarysB2AvyXaNzrZAIau--
    private String handleUploadFile(byte[] data) throws UnsupportedEncodingException {
        //text/plain 固定发送 WebKitFormBoundaryvZnw9hoqtB3dl2ak ? 浏览器chrome,firefox一样
        //对于jpg文件，xffxd8 --- xffxd9
        String msg = new String(data, request.getCharset());
//        logger.info(String.format("收到来自客户端的数据: %s", msg));
        int j = msg.indexOf(HTTPStatus.FILEMARK), k, l, start, end, sideLen, len;
        String head = msg;
        if (j >= 0) {
            request.setMultipart(true);
            j += HTTPStatus.FILEMARK.length();
            int jj = j;
            for (; j < msg.length() && msg.charAt(j) != HTTPStatus.CRLF; j++) ;
            //拿到文件分隔符
            String fileDesc = msg.substring(jj, j - HTTPStatus.LINE_SPLITER + 1);
            String tmp = msg.substring(0, j);
            int offset = tmp.getBytes(request.getCharset()).length;
            msg = msg.substring(j);
            j = msg.indexOf(fileDesc);
            if (j >= 0) {
                head = tmp + msg.substring(0, j - HTTPStatus.MUPART_DESC_LINE.length() - HTTPStatus.LINE_SPLITER);
            }
            boolean isFile = false;
            String part, varName, fileName, contentType, sideWindow, varValue;
            String fileType[];
            byte[] fileData;
            List<Object> objects;
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
                        if ((jj = part.indexOf(HTTPStatus.FILENAME, jj)) > 0) {//二进制文件
                            jj += HTTPStatus.FILENAME.length() + 1;
                            for (k = jj; k < part.length() && part.charAt(k) != HTTPStatus.FILENAMETAIL; k++) ;
                            fileName = part.substring(jj, k);
                            k += HTTPStatus.LINE_SPLITER;
                            for (; k < part.length() && part.charAt(k) != HTTPStatus.CRLF; k++) ;
                            for (l = ++k; k < part.length() && part.charAt(k) != HTTPStatus.CRLF; k++) ;
                            //得到文件类型
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
                                        for (end = start; end < data.length; end++) {
                                            if (new String(data, end, sideLen, request.getCharset()).equals(sideWindow)) {
                                                offset = end + sideLen + HTTPStatus.LINE_SPLITER;
                                                end = end - HTTPStatus.LINE_SPLITER + 1;//在windows中String,即使是一个字符\n也会被解析成\r\n
                                                isFile = true;
                                                break;
                                            }
                                        }
                                    }
                                    len = end - start;
                                    if (len >= 0 && end <= data.length) {
                                        //copy file data
                                        fileData = new byte[len];
                                        System.arraycopy(data, start, fileData, 0, len);
                                        Tuple<String, String, byte[]> file = new Tuple<>(fileName, fileType[1].trim(), fileData);
                                        request.getUploadFiles().put(varName, file);//文件varname名字相同的只保留一份
                                    }
                                }
                            }
                        } else {//二进制表单变量
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
        return head;
    }

    @Override
    public void failed(Throwable t, Attachment att) {
        try {
            logger.error(LOG.LOG_PRE + "accept" + LOG.LOG_POS, att.getClient().getRemoteAddress(), LOG.EXCEPTION_DESC, t);
        } catch (IOException e) {
            logger.error("Not Get Client Remote IP:" + LOG.LOG_PRE, t);
        }
    }

    @Override
    protected void pushClient(String content, java.io.File staticFile) {
        if (content != null) {
            att.getBuffer().clear();
            att.getBuffer().put(content.getBytes(Charset.forName(response.getCharset())));
            att.getBuffer().flip();
            att.getClient().write(att.getBuffer());
            att.getBuffer().clear();
            //Files.readAllBytes(Patrhs.get("./WebRoot" + staticFile)
            FileInputStream in = null;
            if (staticFile != null) {
                try {
                    in = new FileInputStream(staticFile);
                    FileChannel fin = in.getChannel();
                    ByteBuffer by = ByteBuffer.allocateDirect(server.getFileBuffer());
                    while (fin.read(by) != -1) {
                        by.flip();
                        try {
                            att.getClient().write(by).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        by.clear();
                    }
                } catch (IOException e) {
                    logger.error(LOG.LOG_PRE + "pushClient read I/O" + LOG.LOG_POS, this.getClass().getName(), staticFile.getAbsolutePath(),
                            LOG.EXCEPTION_DESC, e);
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.error(LOG.LOG_PRE + "pushClient I/O Stream close" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
                    }
                }
                att.setReadMode(false);
                att.getClient().write(att.getBuffer(), att, this);
            }
        }
    }

}
