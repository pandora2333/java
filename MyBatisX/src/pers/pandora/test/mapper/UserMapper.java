package pers.pandora.test.mapper;

import pers.pandora.test.User;
/**
*author by pandora
*date 2018/11/24
*version 1.3
*适用范围：针对主键自增，无联合主键的数据表使用
*/
public interface UserMapper{

	public User queryForOne(int id); 
	public void update(User entity); 
	public void insert(User entity);
	public void deleteById(int id1);
}