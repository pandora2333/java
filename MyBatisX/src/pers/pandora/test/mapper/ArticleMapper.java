package pers.pandora.test.mapper;
import pers.pandora.test.Article;

public interface ArticleMapper{
	 Article queryForOne(int id);
	 void update(Article entity);
	 void insert(Article entity);
	 void deleteById(int id);
}