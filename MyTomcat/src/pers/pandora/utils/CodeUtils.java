package pers.pandora.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.LOG;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CodeUtils {

    private static Logger logger = LogManager.getLogger(CodeUtils.class);
    //default md5
    public static final String DEFAULTENCODE = "md5";

    private static final char ZERO = '0';

    public static String hashEncode(String plainText, String salt, String hashEncode, String charset) {
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
        String md5code = new BigInteger(1, secretBytes).toString(16);
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = ZERO + md5code;
        }
        return md5code;
    }

    public static String sha1AndBase64(String msg, String charset) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("sha1").digest(
                    msg.getBytes(Charset.forName(charset)));
        } catch (NoSuchAlgorithmException e) {
            logger.error(LOG.LOG_PRE + "sha1AndBase64" + LOG.LOG_POS, CodeUtils.class.getName(), LOG.EXCEPTION_DESC, e);
        }
        return Base64.getEncoder().encodeToString(secretBytes);
    }
}
