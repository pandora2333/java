package pers.pandora.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.Enum.Propagation;
import pers.pandora.annotation.Transactional;
import pers.pandora.constant.LOG;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;

/**
 * 1. Interface implementation
 * 2. The interface method must be public
 * 3. No transaction method can be invoked in the non interface method
 * 4.The Dao layer cannot be called through a multithreading within a transaction method
 */
public class JDKTransactionProxyFactory implements TransactionProxyFactory {

    private static final Logger logger = LogManager.getLogger(JDKTransactionProxyFactory.class);

    private DBPool dbPool;

    @Override
    public <T> T createProxyClass(final Class<T> tClass, final DBPool dbPool) {
        final TransactionHandler transactionHandler = new TransactionHandler();
        this.dbPool = dbPool;
        try {
            transactionHandler.setTarget(tClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("createProxyClass" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
        }
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), tClass.getInterfaces(), transactionHandler);
    }

    private class TransactionHandler implements InvocationHandler {

        private Object target;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            assert target != null;
            try {
                method = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                //ignore
            }
            Transactional transactional = method.getAnnotation(Transactional.class);
            Object result = null;
            boolean transaction = false;
            PoolConnection connection = null;
            int level = MapperProxyHandler.ZERO;
            if (transactional != null) {
                transaction = true;
                //close db auto-commit
                try {
                    connection = dbPool.getConnection();
                } catch (SQLException e) {
                    logger.error("proxy invoke" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                }
                if (connection != null) {
                    try {
                        connection.getConnection().setAutoCommit(false);
                        level = connection.getConnection().getTransactionIsolation();
                        connection.getConnection().setTransactionIsolation(transactional.isolation());
                        //Binding transactions through ThreadLocal
                        if(transactional.propagation() == Propagation.REQUIRES_NEW){
                            connection.setTransNew(MapperProxyHandler.ZERO);
                        }
                        TRANSACTIONS.set(connection);
                    } catch (SQLException e) {
                        logger.error("connection set auto_commit" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                    }
                }

            }
            try {
                result = method.invoke(target, args);
                //commit
            } catch (IllegalAccessException | InvocationTargetException e) {
                //execute rollback
                if (transaction && checkNoRollBackException(e.getCause().toString(), transactional.no_rollback_exception())) {
                    try {
                        assert connection != null;
                        connection.getConnection().rollback();
                        logger.debug("method:" + LOG.LOG_PRE + "trigger connection rollback" + LOG.LOG_POS, method.getName(), LOG.ERROR_DESC, e.getCause());
                    } catch (SQLException e1) {
                        logger.error("connection rollback" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getCause());
                    }
                }
            }
            if (connection != null) {
                try {
                    connection.getConnection().setAutoCommit(true);
                    connection.getConnection().setTransactionIsolation(level);
                    TRANSACTIONS.remove();
                    dbPool.commit(connection);
                } catch (SQLException e) {
                    logger.error("connection set auto_commit" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                }
            }
            return result;
        }

        private boolean checkNoRollBackException(String message, String[] no_roll_backs) {
            for (String no_roll_back : no_roll_backs) {
                if (message.startsWith(no_roll_back)) {
                    return false;
                }
            }
            return true;
        }

        void setTarget(Object target) {
            this.target = target;
        }
    }
}
