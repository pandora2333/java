package pers.pandora.core.utils;

import org.dom4j.Document;
import org.dom4j.Element;
import pers.pandora.core.DBPool;
import pers.pandora.core.PoolConnection;
import pers.pandora.mbg.ColumnInfo;
import pers.pandora.mbg.JavaGetSetter;
import pers.pandora.mbg.TableInfo;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MBG逆向工程
 */
public class MBGUtils {

    private static DBPool dbPool = DBPool.getDBPool();
    /**
     * 表名为key，表信息对象为value
     * 定义为map的目的：
     * 1.线程安全
     * 2.将表名与表的封装类关联起来，方便查询，以后扩展和代码优化更方便
     * ps：可以改为List集合等其它方式存储，可以获得更小的内存消耗,节省内存
     */
    private static Map<String, TableInfo> tables = new ConcurrentHashMap<>();

    public static Map<String, TableInfo> getTables() {//对外提供使用
        return tables;
    }

    /**
     * 加锁适应多线程环境，针对具体使用环境，可以考虑优化时去掉
     *解析数据库表，并持久化到相应Java实体类中
     * @param poPackage
     * @param mapperPackage
     * @param way
     * @param xmlPackage
     */
    private static synchronized void parseTables(String poPackage,String mapperPackage,String way,String xmlPackage){
        PoolConnection connection = null;
        ResultSet rs = null;
        try {
            connection =dbPool.getConnection();
            DatabaseMetaData dbmd=connection.getConnection().getMetaData();
            ResultSet tableRet=dbmd.getTables(null, "%", "%", new String[]{"TABLE"});
            while (tableRet.next()) {
                String tableName=(String) tableRet.getObject("TABLE_NAME");
                TableInfo ti=new TableInfo(tableName,new ArrayList<ColumnInfo>());
                tables.put(tableName,ti);
                ResultSet set=dbmd.getColumns(null, "%", tableName,"%");//查询表中所有字段
                while(set.next()){
                    ColumnInfo ci=new ColumnInfo(set.getString("COLUMN_NAME"),set.getString("TYPE_NAME"),false);
                    ti.getColumnInfos().add(ci);
                }
                ResultSet set2=dbmd.getPrimaryKeys(null, "%",tableName);//查询t_user表中注解
                while(set2.next()){
                    for(ColumnInfo columnInfo:ti.getColumnInfos()) {
                        if(set2.getObject("COLUMN_NAME").equals(columnInfo.getColumnName())) {
                            columnInfo.setPrimaryKeyId(true);
                            break;//不支持联合主键查询
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (connection != null) {
                    dbPool.commit(connection);
                }
            }catch (SQLException e) {
                e.printStackTrace();
            }
        }
        createEntity(poPackage,way);//生成java实体类源文件
        createMapper(poPackage,mapperPackage,way);//生成mapper接口源文件
        createMapperXML(mapperPackage,poPackage,xmlPackage,way);//生成mapper对应xml文件
    }

    /**
     * 一表一实体，对应规则，持久化到Java代码中
     * @param poPackage
     * @param way
     */
   private static  void createEntity(String poPackage,String way){
        if(tables.size()>0){
            StringBuilder javaType = new StringBuilder();//提高重用性,解决多线程问题
            StringBuilder javaFile = new StringBuilder();
            for(TableInfo tableInfo:tables.values()){
                javaType.append("package ").append(poPackage+";\n\n");
                javaType.append("import pers.pandora.annotation.*;\n");//导包
                /**
                 * 加上作者信息及使用注释
                 */
                javaType.append("/**\n");
                javaType.append("*author by pandora\n");
                javaType.append("*date 2018/11/24\n");
                javaType.append("*version 1.3\n");
                javaType.append("*适用范围：针对主键自增，无联合主键的数据表使用\n");
                javaType.append("*/\n");
                /**
                 *
                 */
                javaFile.append(tableInfo.getTableName().substring(0,1).toUpperCase()+tableInfo.getTableName().substring(1));
                javaType.append("@Table\n");//加入table注解
                javaType.append("public class ").append(javaFile)
                        .append("{\n");
                for(ColumnInfo columnInfo:tableInfo.getColumnInfos()) {
                    javaType.append(JavaGetSetter.fieldDefined(columnInfo))
                            .append(JavaGetSetter.getter(columnInfo))
                            .append(JavaGetSetter.setter(columnInfo));
                }
                javaType.append("\n}");
                if(way!=null&&!way.equals("all")&&way.equals(tableInfo.getTableName())) {
                    try {
                        createFile(poPackage, javaFile.toString(), javaType.toString(),true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }else if(way!=null&&way.equals("all")){
                    try {
                        createFile(poPackage, javaFile.toString(), javaType.toString(),true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                javaFile.delete(0,javaFile.length());
                javaType.delete(0,javaType.length());
            }
        }
    }

    /**
     * 担任具体写操作任务，将表持久化到硬盘中
     * @param poPackage
     * @param javaFile
     * @param content
     * @param isJava
     * @throws IOException
     */
    private static void createFile(String poPackage,String javaFile,String content,boolean isJava) throws IOException {//字符流的读写
        File temp = new File("src/"+poPackage.replace(".","/"));
        if(!temp.exists()){
            temp.mkdirs();//目录不存在，则帮助用户创建
        }
        FileWriter fw = null;
        if(isJava){
            fw = new FileWriter(temp.toPath() + "/" + javaFile + ".java");
        }else{
            fw = new FileWriter(temp.toPath() + "/" + javaFile + ".xml");
        }
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);
        bw.flush();
        if(bw!=null){
            bw.close();
        }
        if(fw!=null){
            fw.close();
        }
    }

    /**
     * 创建对应实体操作的Mapper接口
     * @param poPackage
     * @param mapperPackage
     * @param way
     */
    private static  void createMapper(String poPackage,String mapperPackage,String way){
        if(tables.size()>0){
            StringBuilder javaType = new StringBuilder();//提高重用性,解决多线程问题
            StringBuilder javaFile = new StringBuilder();
            for(TableInfo tableInfo:tables.values()){
                javaType.append("package ").append(mapperPackage+";\n\n");
                javaFile.append(tableInfo.getTableName().substring(0,1).toUpperCase()+tableInfo.getTableName().substring(1));
                javaType.append("import ").append(poPackage+"."+javaFile+";\n");//导包
                /**
                 * 加上作者信息及使用注释
                 */
                javaType.append("/**\n");
                javaType.append("*author by pandora\n");
                javaType.append("*date 2018/11/24\n");
                javaType.append("*version 1.3\n");
                javaType.append("*适用范围：针对主键自增，无联合主键的数据表使用\n");
                javaType.append("*/\n");
                /**
                 *
                 */
                javaType.append("public interface ").append(javaFile+"Mapper")
                        .append("{\n");
                javaType.append("\n\tpublic "+javaFile+" queryForOne(int id); ");
                javaType.append("\n\tpublic void update("+javaFile+" entity); ");
                javaType.append("\n\tpublic void insert("+javaFile+" entity);");
                javaType.append("\n\tpublic void deleteById(int id1);");
                javaType.append("\n}");
                if(way!=null&&!way.equals("all")&&way.equals(tableInfo.getTableName())) {
                    try {
                        createFile(mapperPackage, javaFile+"Mapper", javaType.toString(),true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }else if(way!=null&&way.equals("all")){
                    try {
                        createFile(mapperPackage,javaFile+"Mapper", javaType.toString(),true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                javaFile.delete(0,javaFile.length());
                javaType.delete(0,javaType.length());
            }
        }
    }

    /**
     * 创建对应Mapper接口的XML文件
     * @param mapperPackage
     * @param poPackage
     * @param xmlPackage
     * @param way
     */
    private static  void createMapperXML(String mapperPackage,String poPackage,String xmlPackage,String way){
        if(tables.size()>0){
            StringBuilder javaType = new StringBuilder();//记录文件内容
            StringBuilder javaFile = new StringBuilder();//记录文件创建目录
            StringBuilder insertParams = new StringBuilder();//存储xml中insert #{params}实体属性参数
            StringBuilder updateParams = new StringBuilder();//存储xml中update #{params}实体属性参数
            StringBuilder primaryKey = new StringBuilder();//标识主键
            for(TableInfo tableInfo:tables.values()){
                javaFile.append(tableInfo.getTableName().substring(0,1).toUpperCase()+tableInfo.getTableName().substring(1));
//                javaType.append("<!DOCTYPE MyMapper SYSTEM \"mapper.dtd\">\n");
                javaType.append("<MyMapper namespace=\""+mapperPackage+"."+javaFile+"Mapper"+"\">\n");
                javaType.append("\t<!--可以使用lt,gt,le,ge表示大小关系，配合返回值为List使用-->\n");
                javaType.append("\t<!--如gt #{id} and deptno lt #{no}-->\n");
                javaType.append("\t<!--注意:mapper中定义参数若为实体类，则#{no}对应参数实体类中属性名,否则随意取名-->\n");
                javaType.append("\t<select id=\"queryForOne\" resultType=\""+poPackage+"."+javaFile+"\">\n" +
                        "\t\tselect * from "+tableInfo.getTableName()+" where id = #{id}\n" +
                        "  \t</select>\n");
                for(ColumnInfo columnInfo:tableInfo.getColumnInfos()){//遍历insert所需params
                    if(!columnInfo.isPrimaryKeyId()) {
                        insertParams.append("#{"+columnInfo.getColumnName()+"},");
                        updateParams.append(columnInfo.getColumnName()+"="+"#{"+columnInfo.getColumnName()+"},");
                    }else {
                        primaryKey.append(columnInfo.getColumnName());
                    }
                }
                javaType.append("\t<insert id=\"insert\">\n" +
                        "\t\tinsert into "+tableInfo.getTableName()+" value(null,"+insertParams.substring(0,insertParams.length()-1)+")\n" +
                        "\t</insert>\n");
                javaType.append("\t<delete id=\"deleteById\">\n" +
                        "\t\tdelete from "+tableInfo.getTableName()+" where id in (#{id1})\n" +
                        "\t</delete>\n");
                javaType.append("\t<update id=\"update\">\n" +
                        "\t\tupdate "+tableInfo.getTableName()+" set "+updateParams.substring(0,updateParams.length()-1)+" where "+primaryKey+" = #{"+primaryKey+"}\n" +
                        "\t</update>\n");
                javaType.append("</MyMapper>");
                if(way!=null&&!way.equals("all")&&way.equals(tableInfo.getTableName())) {
                    try {
                        createFile(xmlPackage, javaFile+"Mapper", javaType.toString(),false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }else if(way!=null&&way.equals("all")){
                    try {
                        createFile(xmlPackage,javaFile+"Mapper", javaType.toString(),false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                javaFile.delete(0,javaFile.length());
                javaType.delete(0,javaType.length());
                insertParams.delete(0,insertParams.length());
                updateParams.delete(0,updateParams.length());
                primaryKey.delete(0,primaryKey.length());
            }
        }
    }

    /**
     * MBG总入口
     * 担任解析mbg初始化配置文件：mbg.xml解析，加载，entity-mapper-xml生成的总任务
     * @param xmlPath
     */
    public static void parseXML(String xmlPath){//解析mbg.xml文件
        Document doc = Dom4JUtil.getDocument("src/"+xmlPath);
        Element rootElement = doc.getRootElement();
        String poPackage = rootElement.element("poPackage").getTextTrim();
        String mapperPackage = rootElement.element("mapperPackage").getTextTrim();
        String xmlPackage = rootElement.element("xmlPackage").getTextTrim();
        Element table = rootElement.element("table");
        String way =  table.attributeValue("way");
        if(way!=null&&way.equals("singleton")){
            way = table.getTextTrim();
        }else {
            way = "all" ;
        }
        parseTables(poPackage,mapperPackage,way,xmlPackage);
    }
}
