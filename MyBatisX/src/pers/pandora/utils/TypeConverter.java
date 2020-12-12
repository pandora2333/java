package pers.pandora.utils;

import pers.pandora.constant.ENTITY;
import pers.pandora.constant.SQL;

/**
 * Type conversion between database field and entity class field
 */
public class TypeConverter {

    public static String databaseType2JavaType(String columnType) {
        //varchar-->string
        if (SQL.VARCHAR.equalsIgnoreCase(columnType) || SQL.CHAR.equalsIgnoreCase(columnType)
                || SQL.TEXT.equalsIgnoreCase(columnType) || SQL.TINYEXT.equalsIgnoreCase(columnType)
                || SQL.JSON.equalsIgnoreCase(columnType)) {
            return ENTITY.STRING;
        } else if (SQL.INT.equalsIgnoreCase(columnType) || SQL.TINYINT.equalsIgnoreCase(columnType) ||
                SQL.SMALLINT.equalsIgnoreCase(columnType) || SQL.INTEGER.equalsIgnoreCase(columnType)) {
            return ENTITY.INTEGER;
        } else if (SQL.BIGINT.equalsIgnoreCase(columnType)) {
            return ENTITY.LONG;
        } else if (SQL.DOUBLE.equalsIgnoreCase(columnType)) {
            return ENTITY.DOUBLE;
        } else if (SQL.FLOAT.equalsIgnoreCase(columnType)) {
            return ENTITY.FLOAT;
        } else if (SQL.CLOB.equalsIgnoreCase(columnType)) {
            return ENTITY.CLOB;
        } else if (SQL.BLOB.equalsIgnoreCase(columnType)) {
            return ENTITY.BLOB;
        } else if (SQL.DATE.equalsIgnoreCase(columnType)) {
            return ENTITY.DATE;
        } else if (SQL.TIME.equalsIgnoreCase(columnType)) {
            return ENTITY.TIME;
        } else if (SQL.TIMESTAMP.equalsIgnoreCase(columnType) || SQL.DATETIME.equalsIgnoreCase(columnType)) {
            return ENTITY.TIMESTAMP;
        } else if (SQL.DECIMAL.equalsIgnoreCase(columnType)) {
            return ENTITY.DECIMAL;
        }
        return null;
    }
}
