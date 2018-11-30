package pers.pandora.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//读取线程池初始化文件
public class PropertiesUtils {
    private static final Properties properties =new Properties();
    public static String parse(String key,String file){
        try {
            properties.load(new FileInputStream(file));
           return ""+properties.get(key);
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println("资源文件不存在!");
        }
        return "null";
    }
}
