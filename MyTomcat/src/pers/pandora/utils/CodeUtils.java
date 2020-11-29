package pers.pandora.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CodeUtils {

    private static Logger logger = LogManager.getLogger(CodeUtils.class);
    //默认md5
    public static final String DEFAULTENCODE = "md5";

    private static final char ZERO = '0';

    public static String hashEncode(String plainText, String salt, String hashEncode) {
        if (!StringUtils.isNotEmpty(hashEncode)) {
            hashEncode = DEFAULTENCODE;
        }
        if (StringUtils.isNotEmpty(salt)) {
            plainText = salt + plainText + salt;
        }
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance(hashEncode).digest(
                    plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            logger.error(LOG.LOG_PRE + "hashEncode" + LOG.LOG_POS, CodeUtils.class.getName(), LOG.EXCEPTION_DESC, e);
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = ZERO + md5code;
        }
        return md5code;
    }
}
