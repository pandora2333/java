package pers.pandora.test.dd;


import pers.pandora.test.dd.mapper.TestMapper;

public interface TestTransaction {

    void test(int a, int b, int d, int id, TestMapper mapper);
}
