package pers.pandora.handler;

import pers.pandora.bean.Attachment;
import pers.pandora.servlet.Dispatcher;
import pers.pandora.servlet.Request;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

public class AIOServerlHandler extends Dispatcher implements CompletionHandler<Integer, Attachment> {
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
            //firefox对于较大文件会分片发送，即使buffer没满，带宽足够，而chrome会尽可能的一次发送所有数据
            //对于conten-length的长度所指内容是对于文件分隔符之间的所有字段及文件内容值以及其它表单字段所有值以及两者间换行分隔符
            //对于隐私模式下的chrome，上传文件不发送文件数据,在头部信息中有文件分隔符,而firefox却可以发送
//            System.out.println("收到来自客户端的数据: " + msg);
            handleUploadFile(msg, bytes);
            try {
                dispatcher(msg, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            att.setReadMode(false);
            try {
                System.out.println(att.getClient().getRemoteAddress() + " closed!");
                response.clear();
                request.clear();
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
    }

    //firefox
    //---------------------------302099603737780221121456441554
    //-----------------------------302099603737780221121456441554
    //-----------------------------302099603737780221121456441554--
    //chrome
    //----WebKitFormBoundarysB2AvyXaNzrZAIau
    //------WebKitFormBoundarysB2AvyXaNzrZAIau
    //------WebKitFormBoundarysB2AvyXaNzrZAIau--
    //基于windows调制分隔符
    private void handleUploadFile(String msg, byte[] data) {
        int j = msg.indexOf(Request.FILEMARK);
        if (j >= 0) {
            j += Request.FILEMARK.length();
            int jj = j;
            for (; j < msg.length() && msg.charAt(j) != Request.CRLF; j++) ;
            String fileDesc = msg.substring(jj, j - 1);
            int i = 0;//msg.indexOf(Request.CONTENTLENGTH)+Request.CONTENTLENGTH.length()+1;
//            int len = 0;
//            for(;i < msg.length() && Character.isDigit(msg.charAt(i));i++) len = len*10 + msg.charAt(i)-Request.ZERO;
//            if(len >  0){
            int k = msg.indexOf(Request.FILENAME, j);
            k += Request.FILENAME.length() + 1;
            j = k;
            for (; k < msg.length() && msg.charAt(k) != Request.FILENAMETAIL; k++) ;
            i = msg.indexOf(Request.CONTENTTYPE, j);
            for (; i < msg.length() && msg.charAt(i) != Request.CRLF; i++) ;
            int len = data.length - i - 3 - fileDesc.length() - 8;
            byte[] fileData = new byte[len];
            System.arraycopy(data, i + 3, fileData, 0, len);
            request.setFileName(msg.substring(j, k));
            request.setFileData(fileData);
//            }
        }
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
