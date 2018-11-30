package pers.pandora.bean;

import pers.pandora.annotation.Column;
import pers.pandora.annotation.Id;
import pers.pandora.annotation.Table;

/**
 *自定义实体类demo样本
 * 若用mbg已经生成相关beans
 * 如果有重名实体类（关联表名相同，则删除一方实体类，不然会起命名冲突，表现在实体类转换时报错）
 */
@Table("download")//必须加该注解标识数据库表
public class Entity {
    @Id//必须加该注解标识唯一主键
    private int id;
    @Column("fileName")
    private String filename;
    @Column//不加该注解，无法被赋值
    private  String path;

    /**
     * 必须有bean对应属性的相关getter/setter方法
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
