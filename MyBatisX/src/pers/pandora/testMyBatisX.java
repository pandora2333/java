package pers.pandora;

import org.junit.jupiter.api.Test;
import pers.pandora.bean.Entity;
import pers.pandora.bean.User;
import pers.pandora.core.SqlSession;
import pers.pandora.core.SqlSessionFactory;
import pers.pandora.core.utils.MBGUtils;
import pers.pandora.mapper.EntityMapper;
import pers.pandora.mapper.UserMapper;
import pers.pandora.test.mapper.ArticleMapper;
import pers.pandora.test.mapper.DownloadMapper;
//import pers.pandora.test.mapper.ArticleMapper;
//import pers.pandora.test.mapper.DownloadMapper;

import java.sql.Timestamp;

/**
 * author by pandora
 * date 2018/11/24
 * version 1.3
 * 注意:针对主键自增，无联合主键的数据表使用,不支持clob，blob等特殊数据类型，仅争对一些常用数据库字段封装
 * 依赖jar包:jdbc驱动包,dom4j包，junit5(当前类的单元测试使用)
 * jdk版本：jdk8及以上
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
    @Test
    public void test02(){//测试MBG技术
        MBGUtils.parseXML("mbg.xml");
    }
    @Test
    public void test03(){//测试利用MBG技术自动生成的mapper，xml及entity类
        SqlSession sqlSession2 = SqlSessionFactory.createSqlSession("test/mapper/DownloadMapper.xml");
        System.out.println(sqlSession2.createMapper(DownloadMapper.class).queryForOne(3));
        SqlSession sqlSession = SqlSessionFactory.createSqlSession("test/mapper/UserMapper.xml");
        System.out.println(sqlSession.createMapper(pers.pandora.test.mapper.UserMapper.class).queryForOne(17));
        sqlSession.createMapper(pers.pandora.test.mapper.UserMapper.class).deleteById(10);
        pers.pandora.test.User user = new pers.pandora.test.User();
        user.setId(3);
        user.setUserName("尝试一次啊");
        user.setCdate(new Timestamp(1000000));
        user.setMsg("慈善超市菜市场");
        user.setAddress("是窜西南石油大学");
        user.setActive(1);
        user.setFollow("1,2,3,4");
        user.setCdate(new Timestamp(10000000));
        sqlSession.createMapper(pers.pandora.test.mapper.UserMapper.class).update(user);
        SqlSession sqlSession3 = SqlSessionFactory.createSqlSession("test/mapper/ArticleMapper.xml");
        System.out.println(sqlSession3.createMapper(ArticleMapper.class).queryForOne(1));
    }
}
