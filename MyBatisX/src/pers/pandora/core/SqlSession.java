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
import pers.pandora.utils.StringUtil;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SqlSession {

    private static Logger logger = LogManager.getLogger(SqlSession.class.getName());

    private String mapper;

    private MapperProxyHandler mapperProxyHandler;

    public SqlSession(final String mapper, final MapperProxyHandler mapperProxyHandler) {
        this.mapper = mapper;
        this.mapperProxyHandler = mapperProxyHandler;
    }

    public <T> T createMapper(final Class<T> tClass) {
        try {
            return createMapperProxy(mapper, tClass);
        } catch (Exception e) {
            logger.error("createMapper" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
        }
        return null;
    }

    //Return Mapper Proxy
    private <T> T createMapperProxy(final String mapperFile, final Class<T> template) throws Exception {
        final Document doc = Dom4JUtil.getDocument(ENTITY.ROOTPATH + mapperFile);
        final String proxyClass = doc.getRootElement().attributeValue(XML.NAMESPACE);
        final List<Element> selects = doc.getRootElement().elements(SQL.SELECT);
        final List<Element> inserts = doc.getRootElement().elements(SQL.INSERT);
        final List<Element> updates = doc.getRootElement().elements(SQL.UPDATE);
        final List<Element> deletes = doc.getRootElement().elements(SQL.DELETE);
        final Map<String, DynamicSql> proxys = new HashMap<>(8);
        selects.forEach(element -> {
            String select = element.attributeValue(SQL.ID);
            String resultType = element.attributeValue(XML.RESULTTYPE);
            String sql = element.getTextTrim();
            DynamicSql selectSql = null;
            if (StringUtil.isNotEmpty(sql)) {
                selectSql = new DynamicSql(SQL.SELECT, select, resultType, sql);
            }
            if (selectSql != null) {
                proxys.put(selectSql.getId(), selectSql);
            }
        });
        inserts.forEach(element -> {
            String insert = element.attributeValue(SQL.ID);
            String sql = element.getTextTrim();
            DynamicSql insertSql = null;
            if (StringUtil.isNotEmpty(sql)) {
                insertSql = new DynamicSql(SQL.INSERT, insert, null, sql);
                String useAutoKey = element.attributeValue(XML.USEGENERATEDKKEYS);
                if (StringUtil.isNotEmpty(useAutoKey)) {
                    insertSql.setUseGeneratedKey(Boolean.valueOf(useAutoKey));
                    insertSql.setPkName(element.attributeValue(XML.KEPPROPERTY));
                }
            }
            if (insertSql != null) {
                proxys.put(insertSql.getId(), insertSql);
            }
        });
        deletes.forEach(element -> {
            String delete = element.attributeValue(SQL.ID);
            String sql = element.getTextTrim();
            DynamicSql deleteSql = null;
            if (StringUtil.isNotEmpty(sql)) {
                deleteSql = new DynamicSql(SQL.DELETE, delete, null, sql);
            }
            if (deleteSql != null) {
                proxys.put(deleteSql.getId(), deleteSql);
            }
        });
        updates.forEach(element -> {
            String update = element.attributeValue(SQL.ID);
            String sql = element.getTextTrim();
            DynamicSql updateSql = null;
            if (StringUtil.isNotEmpty(sql)) {
                updateSql = new DynamicSql(SQL.UPDATE, update, null, sql);
            }
            if (updateSql != null) {
                proxys.put(updateSql.getId(), updateSql);
            }
        });
        try {
            final Map<String, DynamicSql> makeMethod = new HashMap<>(8);
            final Class tClass = Class.forName(proxyClass);
            if (tClass.isInterface()) {
                DynamicSql dynamicSql;
                for (Method method : tClass.getDeclaredMethods()) {
                    dynamicSql = proxys.get(method.getName());
                    if (dynamicSql != null) {
                        makeMethod.put(dynamicSql.getId(), dynamicSql);
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
