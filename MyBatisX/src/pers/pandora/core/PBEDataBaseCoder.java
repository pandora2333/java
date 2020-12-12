package pers.pandora.core;

import pers.pandora.utils.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PBE:Packaging of DES/3DES/AES symmetric encryption algorithm
 * Level: PBE>AES>3DES>DES
 */
public class PBEDataBaseCoder implements DataBaseCoder {
    //56,CBC,PKCS5Padding
    private static final String PBE = "PBEWITHMD5andDES";
    //Encrypted random string or password
    private static final char[] PASSWORD = {'P', 'a', 'n', 'd', 'o', 'r', 'a', 'O', 'R', 'M', 'S', 'e', 'c', 'u', 'r', 'e', 'K', 'e', 'y'};

    private static Cipher encoder;

    private static Cipher decoder;

    private static Key key;

    static {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(PASSWORD);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE);
            key = factory.generateSecret(pbeKeySpec);
            //new SecureRandom().generateSeed(8);
            PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(new byte[]{12, 23, -63, -23, -86, -87, 33, -9}, 100);
            encoder = Cipher.getInstance(PBE);
            decoder = Cipher.getInstance(PBE);
            encoder.init(Cipher.ENCRYPT_MODE, key, pbeParameterSpec);
            decoder.init(Cipher.DECRYPT_MODE, key, pbeParameterSpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException e) {
            //ignore
        }
    }

    public String encode(String origin) {
        if (!StringUtils.isNotEmpty(origin)) {
            return origin;
        }
        byte[] res = null;
        try {
            res = encoder.doFinal(origin.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            //ignore
        }
        return Base64.getEncoder().encodeToString(res);
    }

    public String decode(String encode) {
        if (!StringUtils.isNotEmpty(encode)) {
            return encode;
        }
        byte[] res = Base64.getDecoder().decode(encode.getBytes());
        try {
            return new String(decoder.doFinal(res));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            //ignore
        }
        return encode;
    }

    @Override
    public String encodeUserName(String origin) {
        return encode(origin);
    }

    @Override
    public String decodeUserName(String encode) {
        return decode(encode);
    }

    @Override
    public String encodePassword(String origin) {
        return encode(origin);
    }

    @Override
    public String decodePassword(String encode) {
        return decode(encode);
    }

    @Override
    public String encodeUrl(String origin) {
        return encode(origin);
    }

    @Override
    public String decodeUrl(String encode) {
        return decode(encode);
    }

//    public static void main(String[] args) {
//        long start = System.currentTimeMillis();
//        //UQOoRbubgUQ=
//        PBEDataBaseCoder coder = new PBEDataBaseCoder();
////        System.out.println("user:" + coder.encode("root"));
//        System.out.println("user:"+ coder.decode(coder.encode("root")));
////        System.out.println("password:" + coder.encode("123456"));
//        System.out.println("password:"+coder.decode(coder.encode("123456")));
////        System.out.println("url:" + coder.encode("jdbc:mysql://localhost:3306/dd?characterEncoding=utf-8&useSSL=true"));
//        System.out.println("url:"+coder.decode(coder.encode("jdbc:mysql://localhost:3306/dd?characterEncoding=utf-8&useSSL=true")));
//        System.out.println("spend time: " + (System.currentTimeMillis() - start) + " ms");
//    }
}
