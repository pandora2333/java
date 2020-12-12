package pers.pandora.core;

public interface DataBaseCoder {

    String encodeUserName(String origin);

    String decodeUserName(String encode);

    String encodePassword(String origin);

    String decodePassword(String encode);

    String encodeUrl(String origin);

    String decodeUrl(String encode);
}
