package pers.pandora.handler;

import pers.pandora.bean.Attachment;
import pers.pandora.servlet.Dispatcher;
import pers.pandora.servlet.Request;
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

public final class AIOServerlHandler extends Dispatcher implements CompletionHandler<Integer, Attachment> {

    Attachment att;

    @Override
    public void completed(Integer result, Attachment att) {
        if (att.isReadMode()) {
            this.att = att;
            ByteBuffer buffer = att.getBuffer();
            buffer.flip();
            byte bytes[] = new byte[buffer.limit()];
            buffer.get(bytes);
            String msg = new String(bytes).trim();
            setRequestMappingHandler(att.getRequestMappingHandler());
            //firefox对于较大文件会分片发送，即使buffer没满，带宽足够，而chrome会尽可能的一次发送所有数据
            //对于conten-length的长度所指内容是对于文件分隔符之间的所有字段及文件内容值以及其它表单字段所有值以及两者间换行分隔符
            //某些时候浏览器不发送文件只发送文件分隔符，推测与服务器环境有关（带宽），对于隐私模式下的chrome，上传文件不发送文件数据,在头部信息中有文件分隔符,而firefox却可以发送
//            System.out.println("收到来自客户端的数据: " + msg);
            try {
                msg = handleUploadFile(msg, bytes);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                dispatcher(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
            att.setReadMode(false);
            try {
                System.out.println(att.getClient().getRemoteAddress() + " closed!");
                att.getClient().close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        } else {
            try {
                att.getClient().close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
//        request.reset();
//        response.reset();
    }

    //firefox
    //---------------------------302099603737780221121456441554
    //-----------------------------302099603737780221121456441554
    //-----------------------------302099603737780221121456441554--
    //chrome
    //----WebKitFormBoundarysB2AvyXaNzrZAIau
    //------WebKitFormBoundarysB2AvyXaNzrZAIau
    //------WebKitFormBoundarysB2AvyXaNzrZAIau--
    private String handleUploadFile(String msg, byte[] data) throws UnsupportedEncodingException {
        //text/plain 固定发送 WebKitFormBoundaryvZnw9hoqtB3dl2ak ? 浏览器chrome,firefox一样
        int j = msg.indexOf(Request.FILEMARK), k;
        String head = msg;
        if (j >= 0) {
            request.setMultipart(true);
            j += Request.FILEMARK.length();
            int jj = j;
            for (; j < msg.length() && msg.charAt(j) != Request.CRLF; j++) ;
            //拿到文件分隔符
            String fileDesc = msg.substring(jj, j - Request.LINE_SPLITER + 1);
            String tmp = msg.substring(0, j);
            int offset = tmp.getBytes(request.getCharset()).length;
            msg = msg.substring(j);
            j = msg.indexOf(fileDesc);
            if (j >= 0) {
                head = tmp + msg.substring(0, j - Request.MUPART_DESC_LINE.length() - Request.LINE_SPLITER);
            }
            while (j >= 0) {
                j += fileDesc.length() + Request.LINE_SPLITER;
                offset += msg.substring(0, j).getBytes(request.getCharset()).length;
                msg = msg.substring(j);
                j = msg.indexOf(fileDesc);
                if (j >= 0) {
                    String part = msg.substring(0, j - Request.MUPART_DESC_LINE.length() - Request.LINE_SPLITER + 1);
                    jj = part.indexOf(Request.FILENAME);
                    if (jj > 0) {//二进制文件
                        jj += Request.FILENAME.length() + 1;
                        for (k = jj; k < part.length() && part.charAt(k) != Request.FILENAMETAIL; k++) ;
                        String fileName = part.substring(jj, k);
                        k += Request.LINE_SPLITER;
                        for (; k < part.length() && part.charAt(k) != Request.CRLF; k++) ;
                        int l;
                        for (l = ++k; k < part.length() && part.charAt(k) != Request.CRLF; k++) ;
                        //得到文件类型
                        String contentType = part.substring(l, k - Request.LINE_SPLITER + 1);
                        if (StringUtils.isNotEmpty(contentType)) {
                            String fileType[] = contentType.split(Request.HEAD_INFO_SPLITER);
                            if (fileType.length == 2) {
                                request.setFileType(fileType[1].trim());
                                int end = j - Request.MUPART_DESC_LINE.length() - Request.LINE_SPLITER;
                                int start = k + 1 + Request.LINE_SPLITER;
                                if (fileType[1].trim().equals(Request.TEXT_PLAIN)) {
                                    start += part.substring(k - Request.LINE_SPLITER + 1).indexOf(contentType) + contentType.length();
                                    for (; end > start && part.charAt(end) != Request.CRLF; end--) ;
                                    end = end - Request.LINE_SPLITER + 1;
                                    end = offset + part.substring(0, end).getBytes(request.getCharset()).length;
                                } else {
                                    end = data.length - msg.substring(j).getBytes(request.getCharset()).length - Request.MUPART_DESC_LINE_2.getBytes(request.getCharset()).length - 2*Request.LINE_SPLITER+1;
                                }
                                //copy file data
                                start = offset + part.substring(0, start).getBytes(request.getCharset()).length;
                                byte[] fileData = new byte[end - start];
                                System.arraycopy(data, start, fileData, 0, fileData.length);
                                request.setFileName(fileName);
                                request.setFileData(fileData);
                            }
                        }
                    } else if ((jj = part.indexOf(Request.MUPART_NAME)) > 0) {//二进制表单变量
                        jj += Request.MUPART_NAME.length() + 1;
                        for (k = jj; k < part.length() && part.charAt(k) != Request.FILENAMETAIL; k++) ;
                        String varName = part.substring(jj, k);
                        k += Request.LINE_SPLITER;
                        String varValue = part.substring(k);
                        List<Object> objects = request.getParams().get(varName);
                        if (objects != null) {
                            objects.add(varValue);
                        } else {
                            objects = new ArrayList<>();
                            objects.add(varValue.trim());
                            request.getParams().put(varName, objects);
                        }
                    }
                    offset += msg.substring(0, j).getBytes(request.getCharset()).length;
                    msg = msg.substring(j);
                    j = msg.indexOf(fileDesc);
                }
            }
        }
        return head;
    }

    @Override
    public void failed(Throwable exc, Attachment attachment) {
        System.out.println("连接断开");
    }

    @Override
    protected void pushClient(String content, String staticFile) {
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
                    in = new FileInputStream(new java.io.File(ROOTPATH + staticFile));
                    FileChannel fin = in.getChannel();
                    ByteBuffer by = ByteBuffer.allocateDirect(2048);
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
                    e.printStackTrace();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                att.setReadMode(false);
                att.getClient().write(att.getBuffer(), att, this);
            }
        }
    }

}
