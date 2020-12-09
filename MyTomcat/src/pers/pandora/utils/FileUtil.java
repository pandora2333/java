package pers.pandora.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class FileUtil {

    public static byte[] deflate(byte[] data){
        if(data.length == 0){
            return data;
        }
        final Deflater compresser = new Deflater();
        byte[] output = new byte[data.length * 2];
        compresser.setInput(data);
        compresser.finish();
        int len = compresser.deflate(output);
        compresser.end();
        byte[] ret = new byte[len];
        System.arraycopy(output,0,ret,0,len);
        return ret;
    }

    public static byte[] inflate(byte[] data) throws DataFormatException {
        if(data.length == 0){
            return data;
        }
        final Inflater decompresser = new Inflater();
        decompresser.setInput(data);
        byte[] output = new byte[data.length * 2];
        int len = decompresser.inflate(output);
        decompresser.end();
        byte[] ret = new byte[len];
        System.arraycopy(output,0,ret,0,len);
        return ret;
    }

//    public static void main(String[] args) {
//        try {
//            // Encode a String into bytes
//            String inputString = "我是Pandora，找啥更好呢?";
//            byte[] input = inputString.getBytes(StandardCharsets.UTF_8);
//            System.out.println(input.length);
//            // Decompress the bytes
//            byte[] a = deflate(input);
//            System.out.println(a.length);
//            byte[] b = inflate(a);
//            // Decode the bytes into a String
//            String outputString = new String(b, 0, b.length, StandardCharsets.UTF_8);
//            System.out.println(outputString);
//
//      } catch(DataFormatException ex) {
//          ex.printStackTrace();
//      }
//    }
}
