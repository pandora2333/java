package pers.pandora.exception;
//数组下标不合法异常
public class IllgealIndexException extends RuntimeException{
    public IllgealIndexException(String error){
        super(error);
    }
}
