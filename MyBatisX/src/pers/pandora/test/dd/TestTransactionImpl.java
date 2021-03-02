//package pers.pandora.test.dd;
//
//import pers.pandora.Enum.Propagation;
//import pers.pandora.annotation.Transaction;
//import pers.pandora.annotation.Transactional;
//import pers.pandora.test.dd.mapper.TestMapper;
//
//@Deprecated
//@Transaction
//public class TestTransactionImpl implements TestTransaction {
//
//    @Transactional(propagation = Propagation.REQUIRES_NEW,no_rollback_exception = {"java.lang.RuntimeException"})
//    @Override
//    public void test(int a, int b, int d, int id, TestMapper mapper) {
//        System.out.println(mapper.queryForOne(a, b, d, id));
////        throw new RuntimeException("test rollback");
//    }
//}
