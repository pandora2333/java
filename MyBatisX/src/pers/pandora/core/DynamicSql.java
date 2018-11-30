package pers.pandora.core;
//动态绑定sql
public class DynamicSql {
    private String method;
    private String id;//方法标识id
    private String resultType;
    private String sql;

    public DynamicSql(String method, String id, String resultType, String sql) {
        this.method = method;
        this.id = id;
        this.resultType = resultType;
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }
}
