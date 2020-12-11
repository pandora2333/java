package pers.pandora.mbg;

import java.util.List;

/**
 * Record sheet information
 */
public class TableInfo {

    private String tableName;

    private List<ColumnInfo> columnInfos;

    private ColumnInfo pk;

    public TableInfo(String tableName, List<ColumnInfo> columnInfos) {
        this.tableName = tableName;
        this.columnInfos = columnInfos;
    }

    public ColumnInfo getPk() {
        return pk;
    }

    public void setPk(ColumnInfo pk) {
        this.pk = pk;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<ColumnInfo> getColumnInfos() {
        return columnInfos;
    }

    public void setColumnInfos(List<ColumnInfo> columnInfos) {
        this.columnInfos = columnInfos;
    }
}
