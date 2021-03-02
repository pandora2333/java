package pers.pandora.pog;

import pers.pandora.constant.ENTITY;
import pers.pandora.constant.XML;
import pers.pandora.utils.TypeConverter;

/**
 * Java property generator
 */
public class JavaGetSetter {
    //Record the current build property name
    private static String field;

    //setter methods
    public static String setter(ColumnInfo columnInfo) {
        JavaGetSetter.field = columnInfo.getColumnName();
        StringBuilder sb = new StringBuilder();
        sb.append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.PUBLIC).append(XML.BLANK).append(ENTITY.VOID).append(XML.BLANK).append(ENTITY.SET)
                .append(Character.toUpperCase(columnInfo.getColumnName().charAt(0))).append(columnInfo.getColumnName().substring(1))
                .append(ENTITY.LEFT_BRACKET).append(TypeConverter.databaseType2JavaType(columnInfo.getDataType())).append(XML.BLANK).append(ENTITY.FIELD)
                .append(ENTITY.RIGHT_BRACKET).append(ENTITY.LEFT_CURLY_BRACKET).append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.TAB).append(ENTITY.THIS)
                .append(ENTITY.POINT).append(field).append(XML.EQUAL_SIGN).append(XML.BLANK).append(ENTITY.FIELD).append(ENTITY.SEMICOLON).append(ENTITY.LINE)
                .append(ENTITY.TAB).append(ENTITY.RIGHT_CURLY_BRACKET);
        return sb.toString();
    }

    public static String getField(ColumnInfo columnInfo) {
        return field;
    }

    //getter methods
    public static String getter(ColumnInfo columnInfo) {
        JavaGetSetter.field = columnInfo.getColumnName();
        StringBuilder sb = new StringBuilder();
        sb.append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.PUBLIC).append(XML.BLANK).append(TypeConverter.databaseType2JavaType(columnInfo.getDataType())).append(XML.BLANK)
                .append(ENTITY.GET).append(columnInfo.getColumnName().substring(0, 1).toUpperCase()).append(columnInfo.getColumnName().substring(1)).append(ENTITY.LEFT_BRACKET)
                .append(ENTITY.RIGHT_BRACKET).append(ENTITY.LEFT_CURLY_BRACKET).append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.TAB).append(ENTITY.RETURN).append(XML.BLANK)
                .append(field).append(ENTITY.SEMICOLON).append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.RIGHT_CURLY_BRACKET);
        return sb.toString();
    }

    public static String fieldDefined(ColumnInfo columnInfo) {
        JavaGetSetter.field = columnInfo.getColumnName();
        StringBuilder sb = new StringBuilder();
        if (columnInfo.isPrimaryKeyId()) {
            sb.append(ENTITY.TAB).append(ENTITY.ID);
        } else {
            sb.append(ENTITY.TAB).append(ENTITY.COLUMN);
        }
        sb.append(ENTITY.LINE).append(ENTITY.TAB).append(ENTITY.PRIVATE).append(XML.BLANK).append(TypeConverter.databaseType2JavaType(columnInfo.getDataType())).append(XML.BLANK)
                .append(columnInfo.getColumnName()).append(ENTITY.SEMICOLON);
        return sb.toString();
    }
}
