package pers.pandora.orm.pog;

/**
 * Field in record table
 */
public class ColumnInfo {

    private String columnName;

    private String dataType;

    public boolean isPrimaryKeyId() {
        return primaryKeyId;
    }

    public void setPrimaryKeyId(boolean primaryKeyId) {
        this.primaryKeyId = primaryKeyId;
    }

    private boolean primaryKeyId;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public ColumnInfo(String columnName, String dataType, boolean primaryKeyId) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.primaryKeyId = primaryKeyId;
    }
}
