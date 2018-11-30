package pers.pandora.mbg;

import java.util.List;

/**
 * 记录表信息
 */
public class TableInfo {
    private String tableName;
    private List<ColumnInfo> columnInfos;

    public TableInfo(String tableName, List<ColumnInfo> columnInfos) {
        this.tableName = tableName;
        this.columnInfos = columnInfos;
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
