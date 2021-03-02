package pers.pandora.pog;

import java.util.ArrayList;
import java.util.List;

/**
 * Record sheet information
 */
public class TableInfo {

    private String tableName;

    private List<ColumnInfo> columnInfos;

    private List<ColumnInfo> pks = new ArrayList<>(1);

    public TableInfo(String tableName, List<ColumnInfo> columnInfos) {
        this.tableName = tableName;
        this.columnInfos = columnInfos;
    }

    public List<ColumnInfo> getPks() {
        return pks;
    }

    public void addPk(ColumnInfo pk) {
        this.pks.add(pk);
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
