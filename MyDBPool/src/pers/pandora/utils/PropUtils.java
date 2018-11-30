package pers.pandora.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
//dbpoool专用
public class PropUtils {
    private static final Properties properties = new Properties();
    static {
        try {
            properties.load(new FileInputStream("src/dbpool.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String parse(String key){
        if(key!=null&&!key.equals("")){
            return String.valueOf(properties.get(key));
        }
        return "null";
    }

}
