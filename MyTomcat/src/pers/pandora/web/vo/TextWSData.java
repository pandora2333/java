package pers.pandora.web.vo;

import pers.pandora.web.constant.HTTPStatus;

import java.nio.charset.Charset;

public final class TextWSData extends WSData {

    private String charset = HTTPStatus.DEFAULTENCODING;

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return charset;
    }

    public String getMessage() {
        return new String(data, Charset.forName(charset));
    }

    public void setMessage(String message) {
        data = message.getBytes(Charset.forName(charset));
    }
}
