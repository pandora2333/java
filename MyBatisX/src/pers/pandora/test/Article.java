package pers.pandora.test;

import pers.pandora.annotation.*;
/**
*author by pandora
*date 2018/11/24
*version 1.3
*适用范围：针对主键自增，无联合主键的数据表使用
*/
@Table
public class Article{

	@Id
	private  Integer  id;
	public Integer  getId(){
		return id;
	}
	public void  setId(Integer field){
		this.id= field;
	}
	@Column
	private  Integer  pid;
	public Integer  getPid(){
		return pid;
	}
	public void  setPid(Integer field){
		this.pid= field;
	}
	@Column
	private  Integer  rootid;
	public Integer  getRootid(){
		return rootid;
	}
	public void  setRootid(Integer field){
		this.rootid= field;
	}
	@Column
	private  String  title;
	public String  getTitle(){
		return title;
	}
	public void  setTitle(String field){
		this.title= field;
	}
	@Column
	private  String  cont;
	public String  getCont(){
		return cont;
	}
	public void  setCont(String field){
		this.cont= field;
	}
	@Column
	private  java.sql.Timestamp  pdate;
	public java.sql.Timestamp  getPdate(){
		return pdate;
	}
	public void  setPdate(java.sql.Timestamp field){
		this.pdate= field;
	}
	@Column
	private  Integer  isleaf;
	public Integer  getIsleaf(){
		return isleaf;
	}
	public void  setIsleaf(Integer field){
		this.isleaf= field;
	}
	@Column
	private  Integer  counter;
	public Integer  getCounter(){
		return counter;
	}
	public void  setCounter(Integer field){
		this.counter= field;
	}
	@Column
	private  Integer  reply;
	public Integer  getReply(){
		return reply;
	}
	public void  setReply(Integer field){
		this.reply= field;
	}
	@Column
	private  String  img;
	public String  getImg(){
		return img;
	}
	public void  setImg(String field){
		this.img= field;
	}
	@Column
	private  Integer  register;
	public Integer  getRegister(){
		return register;
	}
	public void  setRegister(Integer field){
		this.register= field;
	}
	@Column
	private  Integer  userName;
	public Integer  getUserName(){
		return userName;
	}
	public void  setUserName(Integer field){
		this.userName= field;
	}
	@Column
	private  Integer  unread;
	public Integer  getUnread(){
		return unread;
	}
	public void  setUnread(Integer field){
		this.unread= field;
	}
	@Column
	private  Integer  spec;
	public Integer  getSpec(){
		return spec;
	}
	public void  setSpec(Integer field){
		this.spec= field;
	}
	@Column
	private  Integer  locker;
	public Integer  getLocker(){
		return locker;
	}
	public void  setLocker(Integer field){
		this.locker= field;
	}
	@Column
	private  String  question;
	public String  getQuestion(){
		return question;
	}
	public void  setQuestion(String field){
		this.question= field;
	}
	@Column
	private  String  answer;
	public String  getAnswer(){
		return answer;
	}
	public void  setAnswer(String field){
		this.answer= field;
	}
}