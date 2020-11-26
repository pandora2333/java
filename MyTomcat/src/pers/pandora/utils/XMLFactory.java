package pers.pandora.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


public class XMLFactory {
    private static SAXParser sax;
//    private static Map<String,MapContent> context;
    private static boolean isMap;
    private static  String servletClass;
    private static Map<String,String> urlMapping;
    static {
        urlMapping = new ConcurrentHashMap<>();
        try {
            sax = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("解析工厂初始化异常！");
        }
    }
    //节点解析
    public static Map<String,String> parse(String file){
        try {
            sax.parse(new File(file),new XMLHandler());
            return urlMapping;
        } catch (Exception e) {
            System.out.println("文件解析异常!");
            System.out.println(e);
        }
        return null;
    }
    static  class XMLHandler extends DefaultHandler {
        private String tag;
        private MapContent temp;
        private String servletName;
        @Override
        public void startDocument() throws SAXException {
            System.out.println("配置文件解析开始...");
//            context = new ConcurrentHashMap<>(16);
        }

        @Override
        public void endDocument() throws SAXException {
            System.out.println("配置文件解析结束...");
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if(qName!=null) {
                if (qName.equals("servlet")) {
                    tag = "servlet";
                    temp = new MapContent();
                    isMap = false;
                }
                
                if (qName.equals("servlet-class")) {
                    tag = "servlet-class";
                    isMap = false;
                }
                if (qName.equals("servlet-name")) {
                    tag = "servlet-name";
                    isMap = false;
                }
                if (qName.equals("servlet-mapping")) {
                    tag = "servlet-mapping";
                    isMap = true;
                }
                if (qName.equals("url-patterns")) {
                    tag = "url-patterns";
                    isMap = true;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
        	if(qName!=null){
        			if(tag!=null&&tag.equals("servlet-class")&&servletClass!=null){
//        			    temp.setClassName(servletClass);
//        				context.put(servletName,temp);
             		}
        
        	}
            tag = null;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if(tag!=null) {
                String meta = new String(ch, start, length);
                if(tag.equals("servlet-name")){
                    servletName = meta;
                }
                	if(tag.equals("servlet-class")){
                        servletClass = meta;

                }
                	if(tag.equals("url-patterns")){
//                	    if(context.get(servletName)!=null) {
//                            context.get(servletName).getUrls().add(meta);
//                        }else{
//                	        temp.getUrls().add(meta);
//                        }
                        urlMapping.put(meta,servletClass);
                    }
               
            }
        }
    }  
    
//    @Test
//    public void test(){
//    	parse("WebRoot/WEB-INF/web.xml");//WebRoot/WEB-INF/
//    }
}
