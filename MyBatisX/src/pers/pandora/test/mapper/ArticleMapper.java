package pers.pandora.test.mapper;

import pers.pandora.test.Article;
/**
*author by pandora
*date 2018/11/24
*version 1.3
*适用范围：针对主键自增，无联合主键的数据表使用
*/
public interface ArticleMapper{

	public Article queryForOne(int id); 
	public void update(Article entity); 
	public void insert(Article entity);
	public void deleteById(int id1);
}