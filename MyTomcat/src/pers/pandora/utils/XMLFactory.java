package pers.pandora.utils;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import pers.pandora.constant.LOG;
import pers.pandora.vo.MapContent;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


public class XMLFactory {

    private static Logger logger = LogManager.getLogger(XMLFactory.class);

    public static final String SERVLET = "servlet";

    public static final String SERVLET_CLASS = "servlet-class";

    public static final String SERVLET_NAME = "servlet-name";

    public static final String SERVLET_MAPPING = "servlet-mapping";

    public static final String URL_PATTERNS = "url-patterns";

    private SAXParser sax;

    private Map<String, MapContent> context;

    private String servletClass;

    private boolean isMap;

    private Map<String, String> urlMapping;

    public XMLFactory() {
        urlMapping = new ConcurrentHashMap<>();
        try {
            sax = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception e) {
            logger.error(LOG.LOG_PRE + "init()" + LOG.LOG_POS, this, LOG.EXCEPTION_DESC, e);
        }
    }

    //Node resolution
    public Map<String, String> parse(String file) {
        try {
            sax.parse(new File(file), new XMLHandler());
            return urlMapping;
        } catch (Exception e) {
            logger.error(LOG.LOG_PRE + "parse" + LOG.LOG_POS, this, LOG.EXCEPTION_DESC, e);
        }
        return null;
    }

    class XMLHandler extends DefaultHandler {
        private String tag;
        private MapContent temp;
        private String servletName;

        @Override
        public void startDocument() {
            logger.debug("XML file init for loading...");
            context = new ConcurrentHashMap<>(16);
        }

        @Override
        public void endDocument() {
            logger.debug("XML file init for completed");
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName != null) {
                if (qName.equals(SERVLET)) {
                    tag = SERVLET;
                    temp = new MapContent();
                    isMap = false;
                }

                if (qName.equals(SERVLET_CLASS)) {
                    tag = SERVLET_CLASS;
                    isMap = false;
                }
                if (qName.equals(SERVLET_NAME)) {
                    tag = SERVLET_NAME;
                    isMap = false;
                }
                if (qName.equals(SERVLET_MAPPING)) {
                    tag = SERVLET_MAPPING;
                    isMap = true;
                }
                if (qName.equals(URL_PATTERNS)) {
                    tag = URL_PATTERNS;
                    isMap = true;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (qName != null) {
                if (tag != null && tag.equals(SERVLET_CLASS) && servletClass != null) {
                    temp.setClassName(servletClass);
                    context.put(servletName, temp);
                }

            }
            tag = null;
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (tag != null) {
                String meta = new String(ch, start, length);
                if (tag.equals(SERVLET_NAME)) {
                    servletName = meta;
                }
                if (tag.equals(SERVLET_CLASS)) {
                    servletClass = meta;

                }
                if (tag.equals(URL_PATTERNS)) {
                    if (context.get(servletName) != null) {
                        context.get(servletName).getUrls().add(meta);
                        urlMapping.put(meta, context.get(servletName).getClassName());
                    } else {
                        temp.getUrls().add(meta);
                        urlMapping.put(meta, temp.getClassName());
                    }
                }

            }
        }
    }

}
