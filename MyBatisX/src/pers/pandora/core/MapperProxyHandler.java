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

    public static final byte ZERO = 0;

    public static final byte ONE = 1;

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
            List<Object> list = new ArrayList<>(ONE);
            boolean notUnique = handleSQL(method, args, list);
            if (list.size() == ZERO) {
                return null;
            }
            return notUnique ? list : list.get(ZERO);
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
                    list.add(rs.getObject(ONE));
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
                    for (int i = ZERO; i < metaData.getColumnCount(); i++) {
                        columnName = metaData.getColumnLabel(i + ONE);
                        columnValue = rs.getObject(i + ONE);
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
                logger.error("handleField" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getCause());
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
                    m = obj.getClass().getDeclaredMethod(ENTITY.SET + Character.toUpperCase(columnName.charAt(ZERO))
                            + columnName.substring(ONE), columnValue.getClass());
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
            String tableName;
            String key= null;
            if(dynamicSql.getMethod().equals(SQL.SELECT)){
                tableName = getTableName(cacheSql, SQL.FROM, XML.WHERE);
                key = cacheFactory != null ? cacheFactory.createKey(tableName, cacheSql) : null;
                Object cacheObject;
                //query cache
                if (cacheFactory != null && (cacheObject = cacheFactory.get(key)) != null) {
                    list.add(cacheObject);
                    return method.getReturnType() == List.class;
                }
            }
            logger.debug("DEBUG SQL:" + LOG.LOG_PRE, sql);
            PoolConnection connection = AOPProxyFactory.TRANSACTIONS.get();
            boolean transation = false;
            if (connection == null || connection.getTransNew() > ZERO) {
                connection = configuration.getDbPool().getConnection();
            } else {
                if (connection.getTransNew() == ZERO) {
                    connection.setTransNew(ONE);
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
                    Object value = rs.getObject(ONE);
                    args[0].getClass().getDeclaredMethod(ENTITY.SET + Character.toUpperCase(dynamicSql.getPkName().charAt(0))
                            + dynamicSql.getPkName().substring(ONE), value.getClass()).invoke(args[ZERO], value);
                }
            }
            close(st, rs);
            if (!transation) {
                configuration.getDbPool().commit(connection);
            }
            if (cacheFactory != null) {
                if (sql.startsWith(SQL.SELECT)) {
                    cacheFactory.put(key, method.getReturnType() == List.class ? list : list.size() > ZERO ? list.get(ZERO) : null);
                } else {
                    cacheFactory.removeKey(key);
                }
            }
            return method.getReturnType() == List.class;
        }

        private void assignParams(PreparedStatement st, List<Object> params) {
            Class<?> type;
            for (int i = 0; i < params.size() - ONE; i++) {
                try {
                    if (params.get(i) == null) {
                        st.setObject(i + ONE, null);
                        continue;
                    }
                    type = params.get(i).getClass();
                    if (type == Integer.class || type == int.class) {
                        st.setInt(i + ONE, (Integer) params.get(i));
                    } else if (type == Long.class || type == long.class) {
                        st.setLong(i + ONE, (Long) params.get(i));
                    } else if (type == Double.class || type == double.class) {
                        st.setDouble(i + ONE, (Double) params.get(i));
                    } else if (type == Float.class || type == float.class) {
                        st.setFloat(i + ONE, (float) params.get(i));
                    } else if (type == String.class) {
                        st.setString(i + ONE, (String) params.get(i));
                    } else if (type == Clob.class) {
                        st.setClob(i + ONE, (Clob) params.get(i));
                    } else if (type == Blob.class) {
                        st.setBlob(i + ONE, (Blob) params.get(i));
                    } else if (type == Date.class) {
                        st.setDate(i + ONE, (Date) params.get(i));
                    } else if (type == Time.class) {
                        st.setTime(i + ONE, (Time) params.get(i));
                    } else if (type == Timestamp.class) {
                        st.setTimestamp(i + ONE, (Timestamp) params.get(i));
                    } else if (type == BigDecimal.class) {
                        st.setBigDecimal(i + ONE, (BigDecimal) params.get(i));
                    }

                } catch (SQLException e) {
                    logger.error("assignParams" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                }
            }
        }

        private String getTableName(String sql, String condition1, String condition2) {
            String table = sql.substring(sql.indexOf(condition1) + 4).trim();
            int index = table.indexOf(condition2);
            if (index > ZERO) {
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
        if (args == null || args.length == ZERO) {
            params.add(preSql);
            return sql;
        }
        final Pattern pattern = Pattern.compile(XML.VAR_REGEX_PATTERN);
        String var_mark = String.valueOf(XML.VAR_MARK) + ENTITY.LEFT_CURLY_BRACKET;
        if (sql.contains(var_mark)) {
            Matcher matcher = pattern.matcher(sql);
            StringBuffer sb = new StringBuffer();
            int cursor = ZERO;
            Object param = null;
            String rightBracket = String.valueOf(ENTITY.RIGHT_CURLY_BRACKET);
            String quotation = String.valueOf(SQL.QUOTATION);
            boolean vo = !ClassUtil.checkBasicClass(args[ZERO].getClass());
            while (matcher.find()) {
                if (vo) {
                    String paramTemp = matcher.group().replace(var_mark, LOG.NO_CHAR).replace(rightBracket, LOG.NO_CHAR);
                    try {
                        param = args[ZERO].getClass().getDeclaredMethod(ENTITY.GET + Character.toUpperCase(paramTemp.charAt(ZERO)) + paramTemp.substring(ONE)).invoke(args[ZERO]);
                    } catch (Exception e) {
                        logger.error("tokenSpec" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getCause());
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