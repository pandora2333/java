package pers.pandora.test.mapper;
import pers.pandora.test.User;

public interface UserMapper{
	 User queryForOne(int id);
	 void update(User entity);
	 void insert(User entity);
	 void deleteById(int id);
}