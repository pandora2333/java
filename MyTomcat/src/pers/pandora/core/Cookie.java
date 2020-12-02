package pers.pandora.core;

public final class Cookie {

    private String key;

    private String value;
    //GMT time string
    private String expires;

    private String doamin;
    //Cookie sub-domain path
    private String path;
    //httpOnly,default no
    private int secure;
    //expire time after max_age,default time is session level
    private int max_age;
    //cookie version
    private int version = 1;//默认version1
    //when to update Cookie
    private boolean needUpdate;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        needUpdate = true;
        this.expires = expires;
    }

    public String getDoamin() {
        return doamin;
    }

    public void setDoamin(String doamin) {
        needUpdate = true;
        this.doamin = doamin;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        needUpdate = true;
        this.path = path;
    }

    public int getSecure() {
        return secure;
    }

    public void setSecure(int secure) {
        needUpdate = true;
        this.secure = secure;
    }

    public int getMax_age() {
        return max_age;
    }

    public void setMax_age(int max_age) {
        needUpdate = true;
        this.max_age = max_age;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        needUpdate = true;
        this.version = version;
    }

    @Override
    public String toString() {
        return "Cookie{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", expires='" + expires + '\'' +
                ", doamin='" + doamin + '\'' +
                ", path='" + path + '\'' +
                ", secure=" + secure +
                ", max_age=" + max_age +
                ", version=" + version +
                '}';
    }

    public boolean isNeedUpdate() {
        return  needUpdate;
    }

    public void setNeedUpdate(boolean needUpdate) {
        this.needUpdate = needUpdate;
    }
}
