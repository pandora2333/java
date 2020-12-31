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

public final class Dom4JUtil {

    public static Document getDocument(final String path) throws DocumentException {
        final SAXReader reader = new SAXReader();
        // ignore dtd
        reader.setEntityResolver(new IgnoreDTDEntityResolver());
//        reader.setFeature("mbg.dtd", false);
        return reader.read(new File(path));
    }

    @Deprecated
    public static void update(final Document doc,final String path,String encoding) throws IOException {
        if(doc == null || !StringUtil.isNotEmpty(path)){
            return;
        }
        final OutputFormat format = OutputFormat.createPrettyPrint();
        if(!StringUtil.isNotEmpty(encoding)){
            encoding = LOG.DEFAULTENCODING;
        }
        format.setEncoding(encoding);
        final XMLWriter writer = new XMLWriter(new FileWriter(path), format);
        writer.write(doc);
        writer.close();
    }
}
