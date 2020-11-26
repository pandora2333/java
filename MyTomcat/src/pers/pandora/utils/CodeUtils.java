package pers.pandora.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CodeUtils {
    //默认md5
    public static final String DEFAULTENCODE = "md5";
    private static final String ZERO = "0";
    public static String hashEncode(String plainText,String salt,String hashEncode) {
        if(!StringUtils.isNotEmpty(hashEncode)){
            hashEncode = DEFAULTENCODE;
        }
        if(StringUtils.isNotEmpty(salt)){
            plainText = salt + plainText + salt;
        }
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance(hashEncode).digest(
                    plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有这个md5算法！");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = ZERO + md5code;
        }
        return md5code;
    }
}
