package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.ENTITY;
import pers.pandora.constant.LOG;
import pers.pandora.constant.SQL;
import pers.pandora.constant.XML;
import pers.pandora.utils.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.The proxy mapper generator implements all mapper interface methods
 * 2.Using the dynamic proxy provided by JDK has strong portability
 */
public final class MapperProxyClass {

    private static final Logger logger = LogManager.getLogger(MapperProxyClass.class);

    private volatile Configuration configuration;

    public Configuration getConfiguration() {
        return configuration;
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
    public <T> T parseMethod(List<DynamicSql> dynamicSqls, Class<T> proxy) {
        SQLProxyHandler sqlProxyHandler = new SQLProxyHandler();
        sqlProxyHandler.setSqls(dynamicSqls);
        return (T) Proxy.newProxyInstance(proxy.getClassLoader(), new Class[]{proxy}, sqlProxyHandler);
    }

    /**
     * JDK dynamic proxy method processor to enhance the implementation of mapper interface methods
     */
    private class SQLProxyHandler implements InvocationHandler {

        //SQL statement storage
        private List<DynamicSql> sqls;

        public void setSqls(List<DynamicSql> sqls) {
            this.sqls = sqls;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            List<Object> list = new ArrayList<>();
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
         * @param target
         * @param list
         */
        private void handleField(ResultSet rs, Object target, List<Object> list) {
            ResultSetMetaData metaData;
            assert configuration != null;
            Map<String, String> alias = configuration.getAlias();
            try {
                metaData = rs.getMetaData();
                while (rs.next()) {
                    Object rowObj = target.getClass().newInstance();
                    for (int i = 0; i < metaData.getColumnCount(); i++) {
                        String columnName = metaData.getColumnLabel(i + 1);
                        Object columnValue = rs.getObject(i + 1);
                        if (alias.get(columnName) != null) {
                            columnName = alias.get(columnName);
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
                    if (columnValue.getClass() == Long.class || columnValue.getClass() == Integer.class) {
                        m = obj.getClass().getDeclaredMethod(ENTITY.SET + columnName.substring(0, 1).toUpperCase()
                                + columnName.substring(1), Integer.class);
                        columnValue = Integer.valueOf(String.valueOf(columnValue));
                    } else {
                        m = obj.getClass().getDeclaredMethod(ENTITY.SET + columnName.substring(0, 1).toUpperCase()
                                + columnName.substring(1), columnValue.getClass());
                    }
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
            DynamicSql dynamicSql = sqls.stream().filter(sql -> method.getName().equals(sql.getId())).findFirst().get();
            String sql = dynamicSql.getSql();
            logger.debug("DEBUG SQL:" + LOG.LOG_PRE, sql);
            PoolConnection connection = TransactionProxyFactory.TRANSACTIONS.get();
            boolean transation = false;
            if (connection == null) {
                connection = configuration.getDbPool().getConnection();
            }else{
                transation = true;
            }
            sql = tokenSpec(connection, sql, args);
            Statement st = connection.getConnection().createStatement();
            ResultSet rs = null;
            if (dynamicSql.getMethod().equals(SQL.SELECT)) {
                rs = st.executeQuery(sql);
                Object t = configuration.getTableObject(getTableName(sql, SQL.FROM, XML.WHERE));
                handleField(rs, t, list);
            } else if (dynamicSql.getMethod().equals(SQL.INSERT) || dynamicSql.getMethod().equals(SQL.UPDATE)
                    || dynamicSql.getMethod().equals(SQL.DELETE)) {
                st.execute(sql);
                //get pk value
                if (dynamicSql.getMethod().equals(SQL.INSERT) && dynamicSql.isUseGeneratedKey() && StringUtils.isNotEmpty(dynamicSql.getPkName())) {
                    String tableName = getTableName(sql, XML.INTO, XML.VALUE);
                    rs = st.executeQuery(SQL.SELECT + XML.BLANK + SQL.MAX + ENTITY.LEFT_BRACKET + dynamicSql.getPkName()
                            + ENTITY.RIGHT_BRACKET + XML.BLANK + SQL.FROM + XML.BLANK + tableName);
                    rs.next();
                    Object value = rs.getObject(1);
                    args[0].getClass().getDeclaredMethod(ENTITY.SET + Character.toUpperCase(dynamicSql.getPkName().charAt(0))
                            + dynamicSql.getPkName().substring(1), value.getClass()).invoke(args[0], value);
                }
            }
            close(st, rs);
            if(!transation){
                configuration.getDbPool().commit(connection);
            }
            return method.getReturnType() == List.class;
        }

        private String getTableName(String sql, String condition1, String condition2) {
            String table = sql.substring(sql.indexOf(condition1) + 4).trim();
            int index = table.indexOf(condition2);
            if (index > 0) {
                table = table.replace(table.substring(index), LOG.NO_CHAR).trim();
            }
            return table;
        }

        private boolean checkPKType(Class<?> returnType) {
            return returnType == Integer.class || returnType == int.class ||
                    returnType == Long.class || returnType == long.class;
        }
    }

    /**
     * Parsing special characters of XML file expression, such as #{}, le, etc
     *
     * @param con
     * @param sql
     * @param args
     * @param <T>
     * @return
     * @throws SQLException
     */
    private <T> String tokenSpec(PoolConnection con, String sql, Object[] args) throws SQLException {
        List<Class<T>> tClass = new ArrayList<>();
        Map poClassMap = configuration.getPoClassTableMap();
        String percent = String.valueOf(SQL.PERCENT);
        ResultSet tableRet = con.getConnection()
                .getMetaData().getTables(null, percent, percent, new String[]{SQL.TABLE});
        while (tableRet.next()) {
            String tableName = (String) tableRet.getObject(SQL.TABLE_NAME);
            if (poClassMap.containsKey(tableName)) {
                tClass.add((Class<T>) poClassMap.get(tableName));
            }
        }
        close(null, tableRet);
        final Pattern pattern = Pattern.compile(XML.VAR_REGEX_PATTERN);
        if (sql != null) {
            String var_mark = String.valueOf(XML.VAR_MARK) + ENTITY.LEFT_CURLY_BRACKET;
            if (sql.contains(var_mark)) {
                Matcher matcher = pattern.matcher(sql);
                StringBuffer sb = new StringBuffer();
                int cursor = 0;
                Object param = null;
                String rightBracket = String.valueOf(ENTITY.RIGHT_CURLY_BRACKET);
                String quotation = String.valueOf(SQL.QUOTATION);
                while (matcher.find()) {
                    if (tClass.contains(args[0].getClass())) {
                        T temp = (T) args[0];
                        String paramTemp = matcher.group().replace(var_mark, LOG.NO_CHAR).replace(rightBracket, LOG.NO_CHAR);
                        try {
                            param = temp.getClass().getDeclaredMethod(ENTITY.GET + Character.toUpperCase(paramTemp.charAt(0)) + paramTemp.substring(1)).invoke(temp);
                            //Not all attribute parameters of an entity are assigned valid values
                            if (param == null) {
                                //Any type of database can be used
                                param = SQL.ZERO;
                            }
                        } catch (Exception e) {
                            logger.error("tokenSpec" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                        }
                    }
                    if (param != null) {
                        matcher.appendReplacement(sb, quotation + param + quotation);
                    } else {
                        matcher.appendReplacement(sb, quotation + args[cursor] + quotation);
                    }
                    cursor++;
                }
                sql = matcher.appendTail(sb).toString();
            }
            if (sql.contains(XML.BLANK + XML.LT + XML.BLANK)) {
                sql = sql.replace(XML.BLANK + XML.LT + XML.BLANK, XML.BLANK + ENTITY.LT + XML.BLANK);
            }
            if (sql.contains(XML.BLANK + XML.GT + XML.BLANK)) {
                sql = sql.replace(XML.BLANK + XML.GT + XML.BLANK, XML.BLANK + ENTITY.GT + XML.BLANK);
            }
            if (sql.contains(XML.BLANK + XML.LE + XML.BLANK)) {
                sql = sql.replace(XML.BLANK + XML.LE + XML.BLANK, XML.BLANK + ENTITY.LE + XML.BLANK);
            }
            if (sql.contains(XML.BLANK + XML.GE + XML.BLANK)) {
                sql = sql.replace(XML.BLANK + XML.GE + XML.BLANK, XML.BLANK + ENTITY.GE + XML.BLANK);
            }
        }
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