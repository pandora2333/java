package pers.pandora.test;

import org.dom4j.DocumentException;

import pers.pandora.core.*;
import pers.pandora.test.dd.TestTransaction;
import pers.pandora.test.dd.mapper.TestMapper;

import java.sql.Timestamp;

/**
 * Federated primary keys are not supported
 */
@Deprecated
public class testMyBatisX {

    public static void test02(){
        MBG mbg = new MBG("src/dd_test.properties");
        mbg.setDataBaseCoder(new PBEDataBaseCoder());
        try {
            mbg.parseXML("src/mbg.xml");
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        mbg = new MBG("src/dbpool.properties");
        mbg.setDataBaseCoder(new PBEDataBaseCoder());
        try {
            mbg.parseXML("src/mbg.xml");
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

//    public static void test03(){
//        Configuration configuration = new Configuration();
//        configuration.setDataBaseCoder(new PBEDataBaseCoder());
//        configuration.setDbPoolProperties("src/dbpool.properties");
//        configuration.setDataBaseCoder(new PBEDataBaseCoder());
//        configuration.setTransactionProxyFactory(new JDKTransactionProxyFactory());
//        configuration.init("pers.pandora.test");
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactory(configuration);
//        //use cache
//        sqlSessionFactory.setCacheFactory(new SimpleMapCacheFactory());
////        SqlSession sqlSession2 = sqlSessionFactory.createSqlSession("test/mapper/DownloadMapper.xml");
////        System.out.println(sqlSession2.createMapper(DownloadMapper.class).queryForOne(3));
//        SqlSession sqlSession = sqlSessionFactory.createSqlSession("test/dd//mapper/UserMapper.xml");
////        User user = new User();
////        user.setId(17);
////        UserMapper mapper = sqlSession.createMapper(UserMapper.class);
////        System.out.println(mapper.query(user));
////        System.out.println(mapper.query(user));
////        sqlSession.createMapper(pers.pandora.test.mapper.UserMapper.class).deleteById(10);
//        User user = new User();
//        user.setId(null);
//        user.setUserName("尝试一次啊");
//        user.setCdate(new Timestamp(1000000));
//        user.setMsg("慈善超市菜市场");
//        user.setAddress("是窜西南石油大学");
//        user.setActive(1);
//        user.setFollow("1,2,3,4");
//        user.setCdate(new Timestamp(10000000));
//        sqlSession.createMapper(UserMapper.class).insert(user);
//        System.out.println(user.getId());
////        SqlSession sqlSession3 = sqlSessionFactory.createSqlSession("test/mapper/ArticleMapper.xml");
////        System.out.println(sqlSession3.createMapper(ArticleMapper.class).queryForOne(1));
//    }

    public static void main(String[] args) {
//        test02();
//        test03();
        test04();
    }

    private static void test04() {
        Configuration configuration = new Configuration();
        //firstly, set dbProperties file
        configuration.setDbPoolProperties("src/dd_test.properties");
        //secondly, set DataBaseCoder
        configuration.setDataBaseCoder(new PBEDataBaseCoder());
        configuration.setTransactionProxyFactory(new JDKTransactionProxyFactory());
        configuration.init("pers.pandora.test.dd");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactory(configuration);
        SqlSession sqlSession = sqlSessionFactory.createSqlSession("test/dd/mapper/TestMapper.xml");
        TestMapper mapper = sqlSession.createMapper(TestMapper.class);
        //System.out.println(mapper.queryForOne(3));
        TestTransaction proxy = configuration.getTransactionProxyByType(TestTransaction.class);
        proxy.test(3,4,13,3,mapper);
    }
}
