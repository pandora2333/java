package pers.pandora.mbg;

import pers.pandora.core.utils.TypeConverter;

/**
 * Java的属性生成器
 */
public class JavaGetSetter {
    private static String field;//记录当前生成属性名
    public static String setter(ColumnInfo columnInfo){//setter
        JavaGetSetter.field = columnInfo.getColumnName();
        StringBuilder sb = new StringBuilder();
        sb.append("\n\tpublic void  set").append(columnInfo.getColumnName().substring(0,1).toUpperCase()+columnInfo.getColumnName().substring(1))
                .append("("+ TypeConverter.databaseType2JavaType( columnInfo.getDataType()))
                .append(" field)")
                .append("{\n\t\t").append("this."+field).append("= field;\n")
                .append("\t}");
        return sb.toString();
    }

    public static String getField(ColumnInfo columnInfo) {
        return field;
    }

    public static String getter(ColumnInfo columnInfo){//getter
        JavaGetSetter.field = columnInfo.getColumnName();
        StringBuilder sb = new StringBuilder();
        sb.append("\n\tpublic "+TypeConverter.databaseType2JavaType(columnInfo.getDataType())+"  get").append(columnInfo.getColumnName().substring(0,1).toUpperCase()+columnInfo.getColumnName().substring(1))
                .append("()")
                .append("{\n\t\t").append("return "+field).append(";\n")
                .append("\t}");
        return sb.toString();
    }
    public static String fieldDefined(ColumnInfo columnInfo){
        JavaGetSetter.field = columnInfo.getColumnName();
        StringBuilder sb = new StringBuilder();
        if(columnInfo.isPrimaryKeyId()) {
            sb.append("\n\t@Id");
        }else {
            sb.append("\n\t@Column");
        }
        sb.append("\n\tprivate  ").append(TypeConverter.databaseType2JavaType(columnInfo.getDataType())+"  ").append(columnInfo.getColumnName()+";");
        return  sb.toString();
    }
}
