package pers.pandora.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import pers.pandora.constant.LOG;

public class Dom4JUtil {

    public static Document getDocument(String path) throws DocumentException {
        SAXReader reader = new SAXReader();
        // ignore dtd
        reader.setEntityResolver(new IgnoreDTDEntityResolver());
//        reader.setFeature("mbg.dtd", false);
        return reader.read(new File(path));
    }

    @Deprecated
    public static void update(Document doc, String path,String encoding) throws IOException {
        if(doc == null || !StringUtils.isNotEmpty(path)){
            return;
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        if(!StringUtils.isNotEmpty(encoding)){
            encoding = LOG.DEFAULTENCODING;
        }
        format.setEncoding(encoding);
        XMLWriter writer = new XMLWriter(new FileWriter(path), format);
        writer.write(doc);
        writer.close();
    }
}
