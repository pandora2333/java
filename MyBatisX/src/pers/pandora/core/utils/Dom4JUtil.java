package pers.pandora.core.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * 自定义解析xml文档的工具类
 * @author pandora
 *
 */
public class Dom4JUtil {

    public static Document getDocument(String path){
        SAXReader reader = new SAXReader();
        reader.setEntityResolver(new IgnoreDTDEntityResolver()); // ignore dtd
        try {
//            reader.setFeature("mbg.dtd", false);
            return reader.read(new File(path));
        } catch (Exception e) {
            System.out.println("文件未知");
        }
        return null;
    }
    @Deprecated
    public static boolean update(Document doc,String path){
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("utf-8");
        try {
            XMLWriter writer = new XMLWriter(new FileWriter(path), format);
            writer.write(doc);
            writer.close();
            return true;
        } catch (IOException e) {
            System.out.println("文件未知");
        }
        return false;
    }
}
