package pers.pandora.test;
import pers.pandora.annotation.*;

@Table
public class User{
	@Id
	private Integer id;
	public Integer getId(){
		return id;
	}
	public void setId(Integer field){
		this.id= field;
	}
	@Column
	private String userName;
	public String getUserName(){
		return userName;
	}
	public void setUserName(String field){
		this.userName= field;
	}
	@Column
	private Integer stars;
	public Integer getStars(){
		return stars;
	}
	public void setStars(Integer field){
		this.stars= field;
	}
	@Column
	private Integer forbid;
	public Integer getForbid(){
		return forbid;
	}
	public void setForbid(Integer field){
		this.forbid= field;
	}
	@Column
	private String msg;
	public String getMsg(){
		return msg;
	}
	public void setMsg(String field){
		this.msg= field;
	}
	@Column
	private String address;
	public String getAddress(){
		return address;
	}
	public void setAddress(String field){
		this.address= field;
	}
	@Column
	private String pd;
	public String getPd(){
		return pd;
	}
	public void setPd(String field){
		this.pd= field;
	}
	@Column
	private String img;
	public String getImg(){
		return img;
	}
	public void setImg(String field){
		this.img= field;
	}
	@Column
	private java.sql.Timestamp cdate;
	public java.sql.Timestamp getCdate(){
		return cdate;
	}
	public void setCdate(java.sql.Timestamp field){
		this.cdate= field;
	}
	@Column
	private String follow;
	public String getFollow(){
		return follow;
	}
	public void setFollow(String field){
		this.follow= field;
	}
	@Column
	private String collect;
	public String getCollect(){
		return collect;
	}
	public void setCollect(String field){
		this.collect= field;
	}
	@Column
	private Integer active;
	public Integer getActive(){
		return active;
	}
	public void setActive(Integer field){
		this.active= field;
	}
	@Column
	private Integer sex;
	public Integer getSex(){
		return sex;
	}
	public void setSex(Integer field){
		this.sex= field;
	}

}