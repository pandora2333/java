package pers.pandora.core;

public interface TransactionProxyFactory {

    ThreadLocal<PoolConnection> TRANSACTIONS = new ThreadLocal<>();

    <T> T createProxyClass(final Class<T> tClass,final DBPool dbPool);
}
