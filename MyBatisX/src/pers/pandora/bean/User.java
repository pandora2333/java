package pers.pandora.bean;

import pers.pandora.annotation.Column;
import pers.pandora.annotation.Id;
import pers.pandora.annotation.Table;

/**
 * 自定义实体类demo样本
 */
@Table("user")
public class User {
    @Id
    private int id;
    @Column("userName")
    private String username;
    @Column
    private  String msg;

    public void setId(int id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", msg='" + msg + '\'' +
                '}';
    }
}
