package pers.pandora.test.dd;

import pers.pandora.annotation.Transaction;
import pers.pandora.annotation.Transactional;
import pers.pandora.test.dd.mapper.TestMapper;

@Transaction
public class TestTransactionImpl implements TestTransaction {

    @Transactional
    @Override
    public void test(int id, TestMapper mapper) {
        System.out.println(mapper.queryForOne(id));
    }
}
