package pers.pandora.test;

import pers.pandora.annotation.*;
/**
*author by pandora
*date 2018/11/24
*version 1.3
*适用范围：针对主键自增，无联合主键的数据表使用
*/
@Table
public class Download{

	@Id
	private  Integer  id;
	public Integer  getId(){
		return id;
	}
	public void  setId(Integer field){
		this.id= field;
	}
	@Column
	private  String  fileName;
	public String  getFileName(){
		return fileName;
	}
	public void  setFileName(String field){
		this.fileName= field;
	}
	@Column
	private  String  path;
	public String  getPath(){
		return path;
	}
	public void  setPath(String field){
		this.path= field;
	}
	@Column
	private  java.sql.Timestamp  time;
	public java.sql.Timestamp  getTime(){
		return time;
	}
	public void  setTime(java.sql.Timestamp field){
		this.time= field;
	}
	@Column
	private  String  upper;
	public String  getUpper(){
		return upper;
	}
	public void  setUpper(String field){
		this.upper= field;
	}
}