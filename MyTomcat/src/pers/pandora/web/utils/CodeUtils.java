package pers.pandora.web.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pers.pandora.common.constant.LOG;
import pers.pandora.common.utils.StringUtils;
import pers.pandora.web.constant.HTTPStatus;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class CodeUtils {

    private static final Logger logger = LogManager.getLogger(CodeUtils.class.getName());
    //default md5
    public static final String DEFAULTENCODE = "md5";
    //WS default code
    public static final String SHA1 = "sha1";

    private static final char ZERO = '0';

    public static String hashEncode(String plainText, final String salt, String hashEncode, String charset) {
        if (!StringUtils.isNotEmpty(hashEncode)) {
            hashEncode = DEFAULTENCODE;
        }
        if (!StringUtils.isNotEmpty(charset)) {
            charset = HTTPStatus.DEFAULTENCODING;
        }
        if (StringUtils.isNotEmpty(salt)) {
            plainText = salt + plainText + salt;
        }
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance(hashEncode).digest(
                    plainText.getBytes(Charset.forName(charset)));
        } catch (NoSuchAlgorithmException e) {
            logger.error(LOG.LOG_PRE + "hashEncode" + LOG.LOG_POS, CodeUtils.class.getName(), LOG.EXCEPTION_DESC, e);
        }
        assert secretBytes != null;
        String md5code = new BigInteger(1, secretBytes).toString(16);
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = ZERO + md5code;
        }
        return md5code;
    }

    public static String sha1AndBase64(final String msg, String charset) {
        if (!StringUtils.isNotEmpty(msg)) {
            return msg;
        }
        if (!StringUtils.isNotEmpty(charset)) {
            charset = HTTPStatus.DEFAULTENCODING;
        }
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance(SHA1).digest(
                    msg.getBytes(Charset.forName(charset)));
        } catch (NoSuchAlgorithmException e) {
            logger.error(LOG.LOG_PRE + "sha1AndBase64" + LOG.LOG_POS, CodeUtils.class.getName(), LOG.EXCEPTION_DESC, e);
        }
        return Base64.getEncoder().encodeToString(secretBytes);
    }
}
