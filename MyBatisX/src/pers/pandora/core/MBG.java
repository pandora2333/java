package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import pers.pandora.constant.ENTITY;
import pers.pandora.constant.LOG;
import pers.pandora.constant.SQL;
import pers.pandora.constant.XML;
import pers.pandora.mbg.ColumnInfo;
import pers.pandora.mbg.JavaGetSetter;
import pers.pandora.mbg.TableInfo;
import pers.pandora.utils.Dom4JUtil;
import pers.pandora.utils.StringUtils;
import pers.pandora.utils.TypeConverter;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * MBG reverse engineering
 * Note: It should run in a single thread environment
 */
public class MBG {

    private static Logger logger = LogManager.getLogger(MBG.class);

    private DBPool dbPool;
    //The table name is key and the table information object is value
    private final Map<String, TableInfo> tables = new HashMap<>(16);

    public Map<String, TableInfo> getTables() {//对外提供使用
        return tables;
    }

    public DBPool getDbPool() {
        return dbPool;
    }

    public MBG(String dbProperties) {
        dbPool = new DBPool(dbProperties);
    }

    /**
     * Parse the database table and persist it into the corresponding Java entity class
     *
     * @param poPackage
     * @param mapperPackage
     * @param way
     * @param xmlPackage
     */
    private void parseTables(String poPackage, String mapperPackage, String way, String xmlPackage) {
        PoolConnection connection = null;
        try {
            connection = dbPool.getConnection();
            DatabaseMetaData dbmd = connection.getConnection().getMetaData();
            String percent = String.valueOf(SQL.PERCENT);
            ResultSet tableRet = dbmd.getTables(null, percent, percent, new String[]{SQL.TABLE});
            while (tableRet.next()) {
                String tableName = (String) tableRet.getObject(SQL.TABLE_NAME);
                TableInfo tableInfo = new TableInfo(tableName, new ArrayList<>());
                tables.put(tableName, tableInfo);
                //Query all fields in the table
                ResultSet set = dbmd.getColumns(null, percent, tableName, percent);
                while (set.next()) {
                    ColumnInfo ci = new ColumnInfo(set.getString(SQL.COLUMN_NAME), set.getString(SQL.TYPE_NAME), false);
                    tableInfo.getColumnInfos().add(ci);
                }
                //Query annotations in a table
                ResultSet set2 = dbmd.getPrimaryKeys(null, percent, tableName);
                while (set2.next()) {
                    for (ColumnInfo columnInfo : tableInfo.getColumnInfos()) {
                        if (set2.getObject(SQL.COLUMN_NAME).equals(columnInfo.getColumnName())) {
                            columnInfo.setPrimaryKeyId(true);
                            tableInfo.setPk(columnInfo);
                            break;//Federated primary key queries are not supported
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("parseTables" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
        } finally {
            try {
                if (connection != null) {
                    dbPool.commit(connection);
                }
            } catch (SQLException e) {
                //ignore
            }
        }
        assert way != null;
        //Record file content
        final StringBuilder xmlContent = new StringBuilder();
        //Log file creation directory
        final StringBuilder mapperXML = new StringBuilder();
        //Store insert #{params} entity attribute parameters in XML
        final StringBuilder insertParams = new StringBuilder();
        //Store the update #{params} entity attribute parameters in XML
        final StringBuilder updateParams = new StringBuilder();
        //Identify primary key
        final StringBuilder primaryKey = new StringBuilder();
        final StringBuilder mapperContent = new StringBuilder();
        final StringBuilder mapper = new StringBuilder();
        final StringBuilder entityContent = new StringBuilder();
        final StringBuilder entity = new StringBuilder();
        for (TableInfo tableInfo : tables.values()) {
            //Generate Java entity class source file
            boolean singleton = createEntity(way, poPackage, entity, entityContent, tableInfo);
            //Generate mapper interface source file
            createMapper(way, poPackage, mapperPackage, mapper, mapperContent, tableInfo);
            //Generate mapper corresponding XML file
            createMapperXML(way, poPackage, xmlPackage, mapperPackage, mapperXML, xmlContent, insertParams, updateParams, primaryKey, tableInfo);
            if (singleton) {
                break;
            }
        }

    }

    /**
     * One table, one entity, corresponding rules, persistent in Java code
     *
     * @param way
     * @param poPackage
     * @param entity
     * @param entityContent
     * @param tableInfo
     * @return singleton pattern
     */
    private boolean createEntity(String way, String poPackage, StringBuilder entity, StringBuilder entityContent, TableInfo tableInfo) {
        entity.append(Character.toUpperCase(tableInfo.getTableName().charAt(0))).append(tableInfo.getTableName().substring(1));
        entityContent.append(ENTITY.PACKAGE).append(XML.BLANK).append(poPackage).append(ENTITY.SEMICOLON).append(ENTITY.LINE);
        entityContent.append(ENTITY.IMPORT).append(XML.BLANK).append(ENTITY.IMPORT_HEAD).append(ENTITY.SEMICOLON).append(ENTITY.LINE).append(ENTITY.LINE);
        entityContent.append(ENTITY.TABLE_ANNOTATION).append(ENTITY.LINE);
        entityContent.append(ENTITY.PUBLIC).append(XML.BLANK).append(ENTITY.CLASS).append(XML.BLANK).append(entity).append(ENTITY.LEFT_CURLY_BRACKET).append(ENTITY.LINE);
        for (ColumnInfo columnInfo : tableInfo.getColumnInfos()) {
            entityContent.append(JavaGetSetter.fieldDefined(columnInfo)).append(JavaGetSetter.getter(columnInfo)).append(JavaGetSetter.setter(columnInfo)).append(ENTITY.LINE);
        }
        entityContent.append(ENTITY.LINE).append(ENTITY.RIGHT_CURLY_BRACKET);
        boolean singleton = writeFile(way, poPackage, entity.toString(), entityContent.toString(), tableInfo, true, "createEntity");
        entity.delete(0, entity.length());
        entityContent.delete(0, entityContent.length());
        return singleton;
    }

    /**
     * Creating mapper interface corresponding to entity operation
     *
     * @param way
     * @param poPackage
     * @param mapperPackage
     * @param mapper
     * @param mapperContent
     * @param tableInfo
     * @return singleton pattern
     */
    private boolean createMapper(String way, String poPackage, String mapperPackage, StringBuilder mapper, StringBuilder mapperContent, TableInfo tableInfo) {
        mapper.append(Character.toUpperCase(tableInfo.getTableName().charAt(0))).append(tableInfo.getTableName().substring(1));
        mapperContent.append(ENTITY.PACKAGE).append(XML.BLANK).append(mapperPackage).append(ENTITY.SEMICOLON).append(ENTITY.LINE);
        mapperContent.append(ENTITY.IMPORT).append(XML.BLANK).append(poPackage).append(ENTITY.POINT).append(mapper).append(ENTITY.SEMICOLON).append(ENTITY.LINE).append(ENTITY.LINE);
        mapperContent.append(ENTITY.PUBLIC).append(XML.BLANK).append(ENTITY.INTERFACE).append(XML.BLANK).append(mapper).append(ENTITY.MAPPER).append(ENTITY.LEFT_CURLY_BRACKET);
        mapperContent.append(ENTITY.LINE).append(ENTITY.TAB).append(XML.BLANK).append(mapper).append(XML.BLANK).append(ENTITY.QUERYBYID).append(ENTITY.LEFT_BRACKET).append(SQL.INT).append(XML.BLANK).append(SQL.ID).append(ENTITY.RIGHT_BRACKET).append(ENTITY.SEMICOLON);
        mapperContent.append(ENTITY.LINE).append(ENTITY.TAB).append(XML.BLANK).append(ENTITY.VOID).append(XML.BLANK).append(SQL.UPDATE).append(ENTITY.LEFT_BRACKET).append(mapper).append(XML.BLANK).append(ENTITY.ENTITY).append(ENTITY.RIGHT_BRACKET).append(ENTITY.SEMICOLON);
        mapperContent.append(ENTITY.LINE).append(ENTITY.TAB).append(XML.BLANK).append(ENTITY.VOID).append(XML.BLANK).append(SQL.INSERT).append(ENTITY.LEFT_BRACKET).append(mapper).append(XML.BLANK).append(ENTITY.ENTITY).append(ENTITY.RIGHT_BRACKET).append(ENTITY.SEMICOLON);
        mapperContent.append(ENTITY.LINE).append(ENTITY.TAB).append(XML.BLANK).append(ENTITY.VOID).append(XML.BLANK).append(ENTITY.DELETEBYID).append(ENTITY.LEFT_BRACKET).append(SQL.INT).append(XML.BLANK).append(SQL.ID).append(ENTITY.RIGHT_BRACKET).append(ENTITY.SEMICOLON);
        mapperContent.append(ENTITY.LINE).append(ENTITY.RIGHT_CURLY_BRACKET);
        boolean singleton = writeFile(way, mapperPackage, mapper.toString() + ENTITY.MAPPER, mapperContent.toString(), tableInfo, true, "createMapper");
        mapper.delete(0, mapper.length());
        mapperContent.delete(0, mapperContent.length());
        return singleton;
    }
    //Only judge the type of self increasing number
    private String checkPKType(TableInfo tableInfo) {
        String autoPk = TypeConverter.databaseType2JavaType(tableInfo.getPk().getDataType());
        if(StringUtils.isNotEmpty(autoPk) && (autoPk.equals(ENTITY.INTEGER) || autoPk.equals(ENTITY.LONG))){
            return autoPk;
        }
        return null;
    }

    /**
     * Create the XML file corresponding to mapper interface
     *
     * @param way
     * @param xmlPackage
     * @param mapperXML
     * @param xmlContent
     * @param insertParams
     * @param updateParams
     * @param primaryKey
     * @param mapperPackage
     * @param poPackage
     * @param tableInfo
     * @return singleton pattern
     */
    private boolean createMapperXML(String way, String poPackage, String xmlPackage, String mapperPackage, StringBuilder mapperXML, StringBuilder xmlContent, StringBuilder insertParams, StringBuilder updateParams, StringBuilder primaryKey, TableInfo tableInfo) {
        mapperXML.append(Character.toUpperCase(tableInfo.getTableName().charAt(0))).append(tableInfo.getTableName().substring(1));
//                javaType.append("<!DOCTYPE MyMapper SYSTEM \"mapper.dtd\">\n");
        xmlContent.append(XML.MAPPER_HEAD).append(XML.BLANK).append(XML.NAMESPACE).append(XML.EQUAL_SIGN).append(XML.QUOTATION).append(mapperPackage).append(ENTITY.POINT).append(mapperXML).append(ENTITY.MAPPER).append(XML.QUOTATION).append(XML.END).append(ENTITY.LINE);
        xmlContent.append(ENTITY.TAB).append(XML.DESC_1).append(ENTITY.LINE);
        xmlContent.append(ENTITY.TAB).append(XML.DESC_2).append(ENTITY.LINE);
        xmlContent.append(ENTITY.TAB).append(XML.DESC_3).append(ENTITY.LINE);
        xmlContent.append(ENTITY.TAB).append(XML.SELECT).append(XML.BLANK).append(SQL.ID).append(XML.EQUAL_SIGN).append(XML.QUOTATION).append(ENTITY.QUERYBYID).append(XML.QUOTATION).append(XML.BLANK).append(XML.RESULTTYPE).append(XML.EQUAL_SIGN).append(XML.QUOTATION).append(poPackage).append(ENTITY.POINT)
                .append(mapperXML).append(XML.QUOTATION).append(XML.END).append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.TAB).append(SQL.SELECT).append(XML.BLANK).append(XML.STAR).append(XML.BLANK).append(SQL.FROM).append(XML.BLANK).append(tableInfo.getTableName()).append(XML.BLANK)
                .append(XML.WHERE).append(XML.BLANK).append(SQL.ID).append(XML.EQUAL_SIGN).append(XML.VAR_MARK).append(ENTITY.LEFT_CURLY_BRACKET).append(SQL.ID).append(ENTITY.RIGHT_CURLY_BRACKET).append(ENTITY.LINE).append(XML.BLANK).append(ENTITY.TAB).
                append(XML.SELECT_END).append(ENTITY.LINE);
        //Params required for traversing insert
        for (ColumnInfo columnInfo : tableInfo.getColumnInfos()) {
            if (!columnInfo.isPrimaryKeyId()) {
                insertParams.append(XML.VAR_MARK).append(ENTITY.LEFT_CURLY_BRACKET).append(columnInfo.getColumnName()).append(ENTITY.RIGHT_CURLY_BRACKET).append(XML.COMMA);
                updateParams.append(columnInfo.getColumnName()).append(XML.EQUAL_SIGN).append(XML.VAR_MARK).append(ENTITY.LEFT_CURLY_BRACKET)
                        .append(columnInfo.getColumnName()).append(ENTITY.RIGHT_CURLY_BRACKET).append(XML.COMMA);
            } else {
                primaryKey.append(columnInfo.getColumnName());
            }
        }
        xmlContent.append(ENTITY.TAB).append(XML.INSERT).append(XML.BLANK).append(SQL.ID).append(XML.EQUAL_SIGN).append(XML.QUOTATION).append(SQL.INSERT).append(XML.QUOTATION);
        if(StringUtils.isNotEmpty(checkPKType(tableInfo))) {
            judgePK(xmlContent,tableInfo.getPk().getColumnName());
        }
        xmlContent.append(XML.END).append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.TAB).append(SQL.INSERT).append(XML.BLANK).append(XML.INTO).append(XML.BLANK)
                .append(tableInfo.getTableName()).append(XML.BLANK).append(XML.VALUE).append(ENTITY.LEFT_BRACKET).append(XML.NULL).append(XML.COMMA).append(insertParams.substring(0, insertParams.length() - 1)).append(ENTITY.RIGHT_BRACKET)
                .append(ENTITY.LINE).append(ENTITY.TAB).append(XML.INSERT_END).append(ENTITY.LINE);
        xmlContent.append(ENTITY.TAB).append(XML.DELETE).append(XML.BLANK).append(SQL.ID).append(XML.EQUAL_SIGN).append(XML.QUOTATION).append(ENTITY.DELETEBYID).append(XML.QUOTATION).append(XML.END).append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.TAB).append(SQL.DELETE).append(XML.BLANK).append(SQL.FROM).append(XML.BLANK)
                .append(tableInfo.getTableName()).append(XML.BLANK).append(XML.WHERE).append(XML.BLANK).append(SQL.ID).append(XML.EQUAL_SIGN).append(XML.VAR_MARK).append(ENTITY.LEFT_CURLY_BRACKET).append(SQL.ID).append(ENTITY.RIGHT_CURLY_BRACKET).append(ENTITY.LINE).append(ENTITY.TAB).append(XML.DELETE_END).append(ENTITY.LINE);
        xmlContent.append(ENTITY.TAB).append(XML.UPDATE).append(XML.BLANK).append(SQL.ID).append(XML.EQUAL_SIGN).append(XML.QUOTATION).append(SQL.UPDATE).append(XML.QUOTATION).append(XML.END).append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.TAB).append(SQL.UPDATE).append(XML.BLANK).append(tableInfo.getTableName())
                .append(XML.BLANK).append(XML.SET).append(XML.BLANK).append(updateParams.substring(0, updateParams.length() - 1)).append(XML.BLANK).append(XML.WHERE).append(XML.BLANK).append(primaryKey).append(XML.EQUAL_SIGN)
                .append(XML.VAR_MARK).append(ENTITY.LEFT_CURLY_BRACKET).append(primaryKey).append(ENTITY.RIGHT_CURLY_BRACKET).append(ENTITY.LINE).append(ENTITY.TAB).append(XML.UPDATE_END).append(ENTITY.LINE);
        xmlContent.append(XML.MAPPER_END);
        boolean singleton = writeFile(way, xmlPackage, mapperXML.toString() + ENTITY.MAPPER, xmlContent.toString(), tableInfo, false, "createMapperXML");
        mapperXML.delete(0, mapperXML.length());
        xmlContent.delete(0, xmlContent.length());
        insertParams.delete(0, insertParams.length());
        updateParams.delete(0, updateParams.length());
        primaryKey.delete(0, primaryKey.length());
        return singleton;
    }

    private void judgePK(StringBuilder xmlContent,String pkName) {
        xmlContent.append(XML.BLANK).append(XML.USEGENERATEDKKEYS).append(XML.EQUAL_SIGN).append(XML.QUOTATION).append(XML.TRUE).append(XML.QUOTATION)
                .append(XML.BLANK).append(XML.KEPPROPERTY).append(XML.EQUAL_SIGN).append(XML.QUOTATION).append(pkName).append(XML.QUOTATION);
    }

    //Database table name cannot be the same as all
    private boolean writeFile(String way, String filePackage, String fileName, String fileContent, TableInfo tableInfo, boolean isJava, String methodName) {
        if (way.equals(tableInfo.getTableName())) {
            try {
                createFile(filePackage, fileName, fileContent, isJava);
            } catch (IOException e) {
                logger.error(methodName + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            }
            return true;
        } else if (way.equals(XML.WAY_ALL)) {
            try {
                createFile(filePackage, fileName, fileContent, isJava);
            } catch (IOException e) {
                logger.error(methodName + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            }
        }
        return false;
    }

    /**
     * As a specific write operation task, the table is persisted to the hard disk
     *
     * @param poPackage
     * @param javaFile
     * @param content
     * @param isJava
     * @throws IOException
     */
    private void createFile(String poPackage, String javaFile, String content, boolean isJava) throws IOException {
        String slash = String.valueOf(ENTITY.SLASH);
        File temp = new File(ENTITY.ROOTPATH + poPackage.replaceAll(ENTITY.POINT_REGEX, slash));
        if (!temp.exists()) {
            temp.mkdirs();
        }
        FileWriter fw;
        if (isJava) {
            fw = new FileWriter(temp.toPath() + slash + javaFile + ENTITY.POINT + ENTITY.JAVA_MARK);
        } else {
            fw = new FileWriter(temp.toPath() + slash + javaFile + ENTITY.POINT + XML.XML_MARK);
        }
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);
        bw.flush();
        bw.close();
        fw.close();
    }

    /**
     * Responsible for parsing MBG initialization configuration file: mbg.xml The total task of parsing, loading, and entity mapper XML generation
     * Note:the file should be 'mbg.xml'
     *
     * @param xmlPath
     */
    public void parseXML(String xmlPath) throws DocumentException {
        Document doc = Dom4JUtil.getDocument(xmlPath);
        Element rootElement = doc.getRootElement();
        String poPackage = rootElement.element(XML.POPACKAGE).getTextTrim();
        String mapperPackage = rootElement.element(XML.MAPPERPACKAGE).getTextTrim();
        String xmlPackage = rootElement.element(XML.XMLPACKAGE).getTextTrim();
        Element table = rootElement.element(XML.TABLE);
        String way = table.attributeValue(XML.WAY);
        if (StringUtils.isNotEmpty(way) && way.equals(XML.SINGLETON)) {
            way = table.getTextTrim();
        } else {
            way = XML.WAY_ALL;
        }
        parseTables(poPackage, mapperPackage, way, xmlPackage);
        tables.clear();
    }
}
