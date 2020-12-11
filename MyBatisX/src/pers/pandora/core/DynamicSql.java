package pers.pandora.core;
//Dynamic binding SQL
public class DynamicSql {

    private String method;
    //Method id
    private String id;

    private String resultType;

    private String sql;

    private boolean useGeneratedKey;

    private String pkName;

    public DynamicSql(String method, String id, String resultType, String sql) {
        this.method = method;
        this.id = id;
        this.resultType = resultType;
        this.sql = sql;
    }

    public void setUseGeneratedKey(boolean useGeneratedKey) {
        this.useGeneratedKey = useGeneratedKey;
    }

    public void setPkName(String pkName) {
        this.pkName = pkName;
    }

    public String getPkName() {
        return pkName;
    }

    public boolean isUseGeneratedKey() {
        return useGeneratedKey;
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
