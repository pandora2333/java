package pers.pandora.core;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import pers.pandora.constant.ENTITY;
import pers.pandora.constant.LOG;
import pers.pandora.constant.SQL;
import pers.pandora.constant.XML;
import pers.pandora.utils.Dom4JUtil;
import pers.pandora.utils.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SqlSession {

    private static Logger logger = LogManager.getLogger(SqlSession.class);

    private String mapper;

    private MapperProxyHandler mapperProxyHandler;

    public SqlSession(String mapper, MapperProxyHandler mapperProxyHandler) {
        this.mapper = mapper;
        this.mapperProxyHandler = mapperProxyHandler;
    }

    public <T> T createMapper(Class<T> tClass) {
        try {
            return createMapperProxy(mapper, tClass);
        } catch (Exception e) {
            logger.error("createMapper" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
        }
        return null;
    }

    //Return Mapper Proxy
    private <T> T createMapperProxy(String mapperFile, Class<T> template) throws Exception {
        Document doc = Dom4JUtil.getDocument(ENTITY.ROOTPATH + mapperFile);
        String proxyClass = doc.getRootElement().attributeValue(XML.NAMESPACE);
        List<Element> selects = doc.getRootElement().elements(SQL.SELECT);
        List<Element> inserts = doc.getRootElement().elements(SQL.INSERT);
        List<Element> updates = doc.getRootElement().elements(SQL.UPDATE);
        List<Element> deletes = doc.getRootElement().elements(SQL.DELETE);
        List<DynamicSql> proxys = new ArrayList<>();
        selects.forEach(element -> {
            String select = element.attributeValue(SQL.ID);
            String resultType = element.attributeValue(XML.RESULTTYPE);
            String sql = element.getTextTrim();
            DynamicSql selectSql = null;
            if (StringUtils.isNotEmpty(sql)) {
                selectSql = new DynamicSql(SQL.SELECT, select, resultType, sql);
            }
            if (selectSql != null) {
                proxys.add(selectSql);
            }
        });
        inserts.forEach(element -> {
            String insert = element.attributeValue(SQL.ID);
            String sql = element.getTextTrim();
            DynamicSql insertSql = null;
            if (StringUtils.isNotEmpty(sql)) {
                insertSql = new DynamicSql(SQL.INSERT, insert, null, sql);
                String useAutoKey = element.attributeValue(XML.USEGENERATEDKKEYS);
                if (StringUtils.isNotEmpty(useAutoKey)) {
                    insertSql.setUseGeneratedKey(Boolean.valueOf(useAutoKey));
                    insertSql.setPkName(element.attributeValue(XML.KEPPROPERTY));
                }
            }
            if (insertSql != null) {
                proxys.add(insertSql);
            }
        });
        deletes.forEach(element -> {
            String delete = element.attributeValue(SQL.ID);
            String sql = element.getTextTrim();
            DynamicSql deleteSql = null;
            if (StringUtils.isNotEmpty(sql)) {
                deleteSql = new DynamicSql(SQL.DELETE, delete, null, sql);
            }
            if (deleteSql != null) {
                proxys.add(deleteSql);
            }
        });
        updates.forEach(element -> {
            String update = element.attributeValue(SQL.ID);
            String sql = element.getTextTrim();
            DynamicSql updateSql = null;
            if (StringUtils.isNotEmpty(sql)) {
                updateSql = new DynamicSql(SQL.UPDATE, update, null, sql);
            }
            if (updateSql != null) {
                proxys.add(updateSql);
            }
        });
        try {
            List<DynamicSql> makeMethod = new LinkedList<>();
            Class tClass = Class.forName(proxyClass);
            if (tClass.isInterface()) {
                for (Method method : tClass.getDeclaredMethods()) {
                    for (DynamicSql dynamicSql : proxys) {
                        if (dynamicSql.getId().equals(method.getName())) {
                            makeMethod.add(dynamicSql);
                        }
                    }
                }
                return mapperProxyHandler.parseMethod(makeMethod, template);
            } else {
                logger.warn("bad configuration file" + LOG.LOG_PRE, LOG.ERROR_DESC);
            }
        } catch (ClassNotFoundException e) {
            logger.error("createMapperProxy" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
        }
        return null;
    }
}
