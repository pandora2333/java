package pers.pandora;

import org.dom4j.DocumentException;

import pers.pandora.core.Configuration;
import pers.pandora.core.MBG;
import pers.pandora.core.SqlSession;
import pers.pandora.core.SqlSessionFactory;
import pers.pandora.test.mapper.ArticleMapper;
import pers.pandora.test.mapper.UserMapper;


import java.sql.Timestamp;

/**
 * Federated primary keys are not supported
 */
public class testMyBatisX {
//    @Test
//    public void test(){//测试自定义mapper,xml,entity类
//        SqlSession sqlSession = SqlSessionFactory.createSqlSession("EntityMapper.xml");
//        System.out.println(sqlSession.createMapper(EntityMapper.class).queryForOne(3));
////        System.out.println(sqlSession.createMapper(EntityMapper.class).queryForList(1,3));
////        sqlSession.createMapper(EntityMapper.class).insert("aaa","bbb");
////        sqlSession.createMapper(EntityMapper.class).delete(8,9);
////        sqlSession.createMapper(EntityMapper.class).update("delete",10);
//        Entity entity = new Entity();
//        entity.setId(1);
//        entity.setFilename("acgp56217297.jpg");
//        System.out.println(sqlSession.createMapper(EntityMapper.class).queryForEntity(entity));
//        SqlSession sqlSession2 = SqlSessionFactory.createSqlSession("UserMapper.xml");
//        sqlSession.createMapper(EntityMapper.class).updateD(entity);
//        User user = new User();
//        user.setId(2);
//        user.setUsername("EntityX");
//        System.out.println(sqlSession2.createMapper(UserMapper.class).queryForUser(user));
//        sqlSession2.createMapper(UserMapper.class).updateD(user);
//    }

    public static void test02(){
        MBG mbg = new MBG("src/dbpool.properties");
        try {
            mbg.parseXML("src/mbg.xml");
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public static void test03(){
        Configuration configuration = new Configuration();
        configuration.setDbPoolProperties("src/dbpool.properties");
        configuration.init("pers.pandora.test");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactory(configuration);
//        SqlSession sqlSession2 = sqlSessionFactory.createSqlSession("test/mapper/DownloadMapper.xml");
//        System.out.println(sqlSession2.createMapper(DownloadMapper.class).queryForOne(3));
        SqlSession sqlSession = sqlSessionFactory.createSqlSession("test/mapper/UserMapper.xml");
//        System.out.println(sqlSession.createMapper(pers.pandora.test.mapper.UserMapper.class).queryForOne(17));
//        sqlSession.createMapper(pers.pandora.test.mapper.UserMapper.class).deleteById(10);
        pers.pandora.test.User user = new pers.pandora.test.User();
        user.setId(null);
        user.setUserName("尝试一次啊");
        user.setCdate(new Timestamp(1000000));
        user.setMsg("慈善超市菜市场");
        user.setAddress("是窜西南石油大学");
        user.setActive(1);
        user.setFollow("1,2,3,4");
        user.setCdate(new Timestamp(10000000));
        sqlSession.createMapper(UserMapper.class).insert(user);
        System.out.println(user.getId());
//        SqlSession sqlSession3 = sqlSessionFactory.createSqlSession("test/mapper/ArticleMapper.xml");
//        System.out.println(sqlSession3.createMapper(ArticleMapper.class).queryForOne(1));
    }

    public static void main(String[] args) {
//        test02();
        test03();
    }
}
