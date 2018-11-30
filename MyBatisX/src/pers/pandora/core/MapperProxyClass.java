package pers.pandora.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代理Mapper生成器，代理实现所有mapper接口方法
 * 使用jdk自带的动态代理，移植性强
 */
public class MapperProxyClass{
    private static  List<DynamicSql> sqls;//sql语句存储
    private static DBPool dbPool = DBPool.getDBPool();//获取dbpool连接池
    private static  List<Object> list = new LinkedList();//数据库关联对象存储
    private static  boolean flag;//记录返回的是list还是单一entity

    /**
     * MapperProxyClass类总入口方法，实现对xml文件sql解析，处理，实体类赋值一系列处理过程
     * @param dynamicSqls
     * @param proxy
     * @param <T>
     * @return mapper代理实现对象
     * @throws Exception
     */
    public  static  <T> T parseMethod(List<DynamicSql> dynamicSqls,Class<T> proxy) throws Exception {
        sqls = dynamicSqls;
        return (T) Proxy.newProxyInstance(proxy.getClassLoader(),new Class[]{proxy},new MyHandler());
    }

    /**
     * jdk动态代理的方法处理器，对mapper接口方法增强实现
     */
    static class MyHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            handleSQL(method,args);
            if(list.size()==0){
                return null;
            }
            return flag?list:list.get(0);
        }

        /**
         * 处理select sql语句对实体类赋值问题，具体赋值在invokeSet方法
         * @param rs
         * @param t
         */
        private static void handleField(ResultSet rs,Object t){
            ResultSetMetaData metaData = null;
            try {
                metaData = rs.getMetaData();
                Map<String,String> alias = Configuration.getAlias();
                while(rs.next()){
                    Object rowObj = t.getClass().newInstance();
                    //调用javabean的无参构造器
                    //多列 selet usdername ,pwd,age from user where id>? and salary>?
                    for(int i=0;i<metaData.getColumnCount();i++){
                        String columnName=metaData.getColumnLabel(i+1);//该方法可以得到别名，如username'name'
                        Object columnValue=rs.getObject(i+1);
                        if(alias.get(columnName)!=null){
                            columnName = alias.get(columnName);
                        }
                        //调用rowobj对象的setusername方法
                        invokeSet(rowObj,columnName,columnValue);
                    }
                    list.add(rowObj);
                }
                close(null,rs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 根据sql语句执行结果为对应实体类赋值，目标select语句
         * @param obj
         * @param columnName
         * @param columnValue
         */
        private static  void invokeSet(Object obj,String columnName,Object columnValue){
            Method m;
            try {
                if(columnValue!=null){
//                    System.out.println(columnValue.getClass());
                    if(columnValue.getClass() == Long.class||columnValue.getClass() == Integer.class){
                        m = obj.getClass().getDeclaredMethod("set"+columnName.substring(0,1).toUpperCase()+columnName.substring(1),Integer.class);//对于自增主键映射过来是Long型
                        columnValue = Integer.valueOf(String.valueOf(columnValue));
                    }else {
                        m = obj.getClass().getDeclaredMethod("set" + columnName.substring(0, 1).toUpperCase() + columnName.substring(1), columnValue.getClass());
                    }
                    m.invoke(obj, columnValue);
                }
            } catch (Exception e) {
//                System.out.println(e);
//                System.out.println("参数封装出错！参数类型或方法名出错!");
            }
        }

        /**
         * 处理sql语句，select，insert等
         * @param method
         * @param args
         * @throws Exception
         */
        private void handleSQL(Method method, Object[] args) throws Exception {
            list.clear();
            for(DynamicSql dynamicSql:sqls) {
//                System.out.println("sql:"+dynamicSql.getMethod());
                if (method.getName().equals(dynamicSql.getId())) {
                    String sql = dynamicSql.getSql();
                    System.out.println("DEBUG SQL:"+sql);
                    PoolConnection connection = dbPool.getConnection();
                    sql = tokenSpec(connection,sql,args);
                    Statement st = connection.getConnection().createStatement();
                    if (dynamicSql.getMethod().equals("select")) {
//                        ResultSet rs = connection.queryForDB(sql);//不能有效释放资源，废弃
                        ResultSet rs = st.executeQuery(sql);
                        String table = sql.substring(sql.indexOf("from") + 4).trim();
                        if (table.contains("where")) {
                            table = table.replace(table.substring(table.indexOf("where")), "").trim();
                        }
                        Object t = Configuration.getBean(table,Class.forName(dynamicSql.getResultType()));
                        handleField(rs,t);
                        close(st,rs);
                        dbPool.commit(connection);
                        break;
                    }else if(dynamicSql.getMethod().equals("insert")||dynamicSql.getMethod().equals("update")||dynamicSql.getMethod().equals("delete")) {
                        st.execute(sql);
                        close(st,null);
                        dbPool.commit(connection);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 解析xml文件表达式的特殊字符，如#{}，le等
     * @param conn
     * @param sql
     * @param args
     * @param <T>
     * @return
     * @throws SQLException
     */
    private static <T> String tokenSpec(PoolConnection conn,String sql,Object[] args) throws SQLException {
        List<Class<T>> tClass = new LinkedList<>();
        Map poClassMap = Configuration.getPoClassTableMap();
        ResultSet tableRet=conn.getConnection()
                .getMetaData().getTables(null, "%", "%", new String[]{"TABLE"});
        while (tableRet.next()) {
            String tableName = (String) tableRet.getObject("TABLE_NAME");
            if(poClassMap.containsKey(tableName)){
                tClass.add((Class<T>) poClassMap.get(tableName));
            }
        }
        close(null,tableRet);
        final Pattern pattern = Pattern.compile("\\#\\{.*?\\}");//匹配#{}解析字段
//      final  Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");//匹配数字
        if (sql != null) {
            if (sql.contains("#{")) {
                // sql = sql.replaceFirst("\\#\\{.*?\\}", "" + args[0]);
                Matcher matcher = pattern.matcher(sql);
                StringBuffer sb = new StringBuffer();
                int cursor = 0;
                Object param = null;
                while (matcher.find()) {
                    if(tClass.contains(args[0].getClass())){
                        T temp = (T)args[0];
                        String paramTemp = matcher.group().replace("#{","").replace("}","");
                        try {
                            param = temp.getClass().getDeclaredMethod("get"+paramTemp.substring(0,1).toUpperCase()+paramTemp.substring(1)).invoke(temp);
                            if(param==null){//对于实体的属性参数不是全部赋有有效值的处理
                                param="0";//数据库任何类型均可使用
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(param!=null){
                        matcher.appendReplacement(sb, "'" + param+"'");
                    }else{
                        matcher.appendReplacement(sb, "'" + args[cursor] + "'");
                    }
                    cursor++;
                }
                sql = matcher.appendTail(sb).toString();
                flag = false;
            }
            if (sql.contains(" lt ")) {
                sql = sql.replace(" lt ", " < ");
                flag = true;
            }
            if (sql.contains(" gt ")) {
                sql = sql.replace(" gt ", " > ");
                flag = true;
            }
            if (sql.contains(" le ")) {
                sql = sql.replace(" le ", " <= ");
                flag = true;
            }
            if (sql.contains(" ge ")) {
                sql = sql.replace(" lt ", " >= ");
                flag = true;
            }
            if(sql.contains(" in")){
                flag = true;
            }
        }
        return  sql;
    }

    /**
     * 关闭流资源
     */
    private static void close(Statement st,ResultSet rs){
        if(rs!=null){
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if(st!=null) {
            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
 }