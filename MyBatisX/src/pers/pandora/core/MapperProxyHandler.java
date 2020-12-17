package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.ENTITY;
import pers.pandora.constant.LOG;
import pers.pandora.constant.SQL;
import pers.pandora.constant.XML;
import pers.pandora.utils.ClassUtil;
import pers.pandora.utils.StringUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.The proxy mapper generator implements all mapper interface methods
 * 2.Using the dynamic proxy provided by JDK has strong portability
 */
public final class MapperProxyHandler {

    private static final Logger logger = LogManager.getLogger(MapperProxyHandler.class);

    private volatile Configuration configuration;

    private CacheFactory cacheFactory;

    public void setCacheFactory(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * The implementation of XML file SQL parsing, processing, entity class assignment a series of processing procedures
     *
     * @param dynamicSqls
     * @param proxy
     * @param <T>
     * @return Mapper proxy implementation object
     * @throws Exception
     */
    public <T> T parseMethod(Map<String, DynamicSql> dynamicSqls, Class<T> proxy) {
        SQLProxyHandler sqlProxyHandler = new SQLProxyHandler();
        sqlProxyHandler.setSqls(dynamicSqls);
        return (T) Proxy.newProxyInstance(proxy.getClassLoader(), new Class[]{proxy}, sqlProxyHandler);
    }

    /**
     * JDK dynamic proxy method processor to enhance the implementation of mapper interface methods
     */
    private class SQLProxyHandler implements InvocationHandler {

        //SQL statement storage
        private Map<String, DynamicSql> sqls;

        public void setSqls(Map<String, DynamicSql> sqls) {
            this.sqls = sqls;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            List<Object> list = new ArrayList<>(1);
            boolean notUnique = handleSQL(method, args, list);
            if (list.size() == 0) {
                return null;
            }
            return notUnique ? list : list.get(0);
        }

        /**
         * This paper deals with the assignment of entity class by select SQL statement. The specific assignment is in the invoke set method
         * Note: Property assignment of setter method based on JavaBean
         *
         * @param rs
         * @param resultType
         * @param list
         */
        private void handleField(ResultSet rs, String resultType, List<Object> list) {
            if (!StringUtil.isNotEmpty(resultType)) {
                return;
            }
            Class<?> tClass = ClassUtil.getClass(resultType);
            assert tClass != null;
            if (ClassUtil.checkBasicClass(tClass)) {
                try {
                    rs.next();
                } catch (SQLException e) {
                    //ignore
                }
                try {
                    list.add(rs.getObject(1));
                } catch (SQLException e) {
                    //ignore
                }
                return;
            }
            ResultSetMetaData metaData;
            assert configuration != null;
            Map<String, String> alias = configuration.getAlias();
            try {
                metaData = rs.getMetaData();
                String columnName, alia;
                Object columnValue, rowObj;
                while (rs.next()) {
                    rowObj = ClassUtil.getInstance(tClass);
                    for (int i = 0; i < metaData.getColumnCount(); i++) {
                        columnName = metaData.getColumnLabel(i + 1);
                        columnValue = rs.getObject(i + 1);
                        alia = alias.get(columnName);
                        if (StringUtil.isNotEmpty(alia)) {
                            columnName = alia;
                        }
                        invokeSet(rowObj, columnName, columnValue);
                    }
                    list.add(rowObj);
                }
                close(null, rs);
            } catch (Exception e) {
                logger.error("handleField" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            }
        }

        /**
         * According to the execution result of SQL statement, assign value to the corresponding entity class, and select the target statement
         *
         * @param obj
         * @param columnName
         * @param columnValue
         */
        private void invokeSet(Object obj, String columnName, Object columnValue) {
            Method m;
            try {
                if (columnValue != null) {
                    m = obj.getClass().getDeclaredMethod(ENTITY.SET + columnName.substring(0, 1).toUpperCase()
                            + columnName.substring(1), columnValue.getClass());
                    m.invoke(obj, columnValue);
                }
            } catch (Exception e) {
                //ignore
            }
        }

        /**
         * Handle methods for select, insert, etc
         *
         * @param method
         * @param args
         * @param list
         * @return
         * @throws Exception
         */
        private boolean handleSQL(Method method, Object[] args, List<Object> list) throws Exception {
            assert configuration != null;
            DynamicSql dynamicSql = sqls.get(method.getName());
            assert dynamicSql != null;
            String sql = dynamicSql.getSql();
            assert StringUtil.isNotEmpty(sql);
            List<Object> params = new ArrayList<>(args.length);
            String cacheSql = tokenSpec(sql, args, params);
            String tableName = getTableName(cacheSql, SQL.FROM, XML.WHERE);
            String key = cacheFactory != null ? cacheFactory.createKey(tableName, cacheSql) : null;
            Object cacheObject;
            //query cache
            if (cacheFactory != null && (cacheObject = cacheFactory.get(key)) != null) {
                list.add(cacheObject);
                return method.getReturnType() == List.class;
            }
            logger.debug("DEBUG SQL:" + LOG.LOG_PRE, sql);
            PoolConnection connection = TransactionProxyFactory.TRANSACTIONS.get();
            boolean transation = false;
            if (connection == null || connection.getTransNew() > 0) {
                connection = configuration.getDbPool().getConnection();
            } else {
                if (connection.getTransNew() == 0) {
                    connection.setTransNew(1);
                }
                transation = true;
            }
            //SQL uses precompiled mode,SQL execution process is divided into preparation, optimization and execution
            PreparedStatement st = connection.getConnection().prepareStatement((String) params.get(params.size() - 1));
            //Assignment parameter
            assignParams(st, params);
            ResultSet rs = null;
            if (dynamicSql.getMethod().equals(SQL.SELECT)) {
                rs = st.executeQuery();
                handleField(rs, dynamicSql.getResultType(), list);
            } else if (dynamicSql.getMethod().equals(SQL.INSERT) || dynamicSql.getMethod().equals(SQL.UPDATE)
                    || dynamicSql.getMethod().equals(SQL.DELETE)) {
                st.execute();
                //get pk value
                if (dynamicSql.getMethod().equals(SQL.INSERT) && dynamicSql.isUseGeneratedKey() && StringUtil.isNotEmpty(dynamicSql.getPkName())) {
                    tableName = getTableName(sql, XML.INTO, XML.VALUE);
                    rs = st.executeQuery(SQL.SELECT + XML.BLANK + SQL.MAX + ENTITY.LEFT_BRACKET + dynamicSql.getPkName()
                            + ENTITY.RIGHT_BRACKET + XML.BLANK + SQL.FROM + XML.BLANK + tableName);
                    rs.next();
                    Object value = rs.getObject(1);
                    args[0].getClass().getDeclaredMethod(ENTITY.SET + Character.toUpperCase(dynamicSql.getPkName().charAt(0))
                            + dynamicSql.getPkName().substring(1), value.getClass()).invoke(args[0], value);
                }
            }
            close(st, rs);
            if (!transation) {
                configuration.getDbPool().commit(connection);
            }
            if (cacheFactory != null) {
                if (sql.startsWith(SQL.SELECT)) {
                    cacheFactory.put(key, method.getReturnType() == List.class ? list : list.size() > 0 ? list.get(0) : null);
                } else {
                    cacheFactory.removeKey(key);
                }
            }
            return method.getReturnType() == List.class;
        }

        private void assignParams(PreparedStatement st, List<Object> params) {
            Class<?> type;
            for (int i = 0; i < params.size() - 1; i++) {
                try {
                    if (params.get(i) == null) {
                        st.setObject(i + 1, null);
                        continue;
                    }
                    type = params.get(i).getClass();
                    if (type == Integer.class || type == int.class) {
                        st.setInt(i + 1, (Integer) params.get(i));
                    } else if (type == Long.class || type == long.class) {
                        st.setLong(i + 1, (Long) params.get(i));
                    } else if (type == Double.class || type == double.class) {
                        st.setDouble(i + 1, (Double) params.get(i));
                    } else if (type == Float.class || type == float.class) {
                        st.setFloat(i + 1, (float) params.get(i));
                    } else if (type == String.class) {
                        st.setString(i + 1, (String) params.get(i));
                    } else if (type == Clob.class) {
                        st.setClob(i + 1, (Clob) params.get(i));
                    } else if (type == Blob.class) {
                        st.setBlob(i + 1, (Blob) params.get(i));
                    } else if (type == Date.class) {
                        st.setDate(i + 1, (Date) params.get(i));
                    } else if (type == Time.class) {
                        st.setTime(i + 1, (Time) params.get(i));
                    } else if (type == Timestamp.class) {
                        st.setTimestamp(i + 1, (Timestamp) params.get(i));
                    } else if (type == BigDecimal.class) {
                        st.setBigDecimal(i + 1, (BigDecimal) params.get(i));
                    }

                } catch (SQLException e) {
                    logger.error("assignParams" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                }
            }
        }

        private String getTableName(String sql, String condition1, String condition2) {
            String table = sql.substring(sql.indexOf(condition1) + 4).trim();
            int index = table.indexOf(condition2);
            if (index > 0) {
                table = table.replace(table.substring(index), LOG.NO_CHAR).trim();
            }
            return table;
        }
    }

    /**
     * Parsing special characters of XML file expression, such as #{}, le, etc
     *
     * @param sql
     * @param args
     * @param params
     * @return
     * @throws SQLException
     */
    private String tokenSpec(String sql, Object[] args, List<Object> params) {
        String preSql = sql.replaceAll(XML.VAR_REGEX_PATTERN, String.valueOf(SQL.QUESTION_MARK));
        if (args == null || args.length == 0) {
            params.add(preSql);
            return sql;
        }
        final Pattern pattern = Pattern.compile(XML.VAR_REGEX_PATTERN);
        String var_mark = String.valueOf(XML.VAR_MARK) + ENTITY.LEFT_CURLY_BRACKET;
        if (sql.contains(var_mark)) {
            Matcher matcher = pattern.matcher(sql);
            StringBuffer sb = new StringBuffer();
            int cursor = 0;
            Object param = null;
            String rightBracket = String.valueOf(ENTITY.RIGHT_CURLY_BRACKET);
            String quotation = String.valueOf(SQL.QUOTATION);
            boolean vo = !ClassUtil.checkBasicClass(args[0].getClass());
            while (matcher.find()) {
                if (vo) {
                    String paramTemp = matcher.group().replace(var_mark, LOG.NO_CHAR).replace(rightBracket, LOG.NO_CHAR);
                    try {
                        param = args[0].getClass().getDeclaredMethod(ENTITY.GET + Character.toUpperCase(paramTemp.charAt(0)) + paramTemp.substring(1)).invoke(args[0]);
                    } catch (Exception e) {
                        logger.error("tokenSpec" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                    }
                } else {
                    param = args[cursor];
                }
                matcher.appendReplacement(sb, quotation + param + quotation);
                params.add(param);
                cursor++;
            }
            sql = matcher.appendTail(sb).toString();
        }
        if (sql.contains(XML.BLANK + XML.LT + XML.BLANK)) {
            sql = sql.replace(XML.BLANK + XML.LT + XML.BLANK, XML.BLANK + ENTITY.LT + XML.BLANK);
            preSql = preSql.replace(XML.BLANK + XML.LT + XML.BLANK, XML.BLANK + ENTITY.LT + XML.BLANK);
        }
        if (sql.contains(XML.BLANK + XML.GT + XML.BLANK)) {
            sql = sql.replace(XML.BLANK + XML.GT + XML.BLANK, XML.BLANK + ENTITY.GT + XML.BLANK);
            preSql = preSql.replace(XML.BLANK + XML.GT + XML.BLANK, XML.BLANK + ENTITY.GT + XML.BLANK);
        }
        if (sql.contains(XML.BLANK + XML.LE + XML.BLANK)) {
            sql = sql.replace(XML.BLANK + XML.LE + XML.BLANK, XML.BLANK + ENTITY.LE + XML.BLANK);
            preSql = preSql.replace(XML.BLANK + XML.LE + XML.BLANK, XML.BLANK + ENTITY.LE + XML.BLANK);
        }
        if (sql.contains(XML.BLANK + XML.GE + XML.BLANK)) {
            sql = sql.replace(XML.BLANK + XML.GE + XML.BLANK, XML.BLANK + ENTITY.GE + XML.BLANK);
            preSql = preSql.replace(XML.BLANK + XML.GE + XML.BLANK, XML.BLANK + ENTITY.GE + XML.BLANK);
        }
        //add tail as pre-sql
        params.add(preSql);
        return sql;
    }

    private static void close(Statement st, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                //ignore
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }
}