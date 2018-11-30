package pers.pandora.core.utils;

/**
 * 数据库字段与实体类字段类型转换
 */
public class TypeConverter {
    public  static  String databaseType2JavaType(String columnType) {
        //varchar-->string
        if("varchar".equalsIgnoreCase(columnType)||"char".equalsIgnoreCase(columnType)
                ||"text".equalsIgnoreCase(columnType)||"tinytext".equalsIgnoreCase(columnType)){
            return "String";
        }else if("int".equalsIgnoreCase(columnType)||"tinyint".equalsIgnoreCase(columnType)||
                "smallint".equalsIgnoreCase(columnType)||"integer".equalsIgnoreCase(columnType)){
            return "Integer";
        }else if("bigint".equalsIgnoreCase(columnType)){
            return "Long";
        }else if("double".equalsIgnoreCase(columnType)){
            return "Double";
        }else if("float".equalsIgnoreCase(columnType)){
            return "Float";
        }else if("clob".equalsIgnoreCase(columnType)){
            return "java.sql.Clob";
        }else if("blob".equalsIgnoreCase(columnType)){
            return "java.sql.Blob";
        }else if("date".equalsIgnoreCase(columnType)){
            return "java.sql.Date";
        }else if("time".equalsIgnoreCase(columnType)){
            return "java.sql.Time";
        }else if("timestamp".equalsIgnoreCase(columnType)||"datetime".equalsIgnoreCase(columnType)){
            return "java.sql.Timestamp";
        }
        return null;
    }
}
