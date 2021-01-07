package pers.pandora.core;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.Enum.Propagation;
import pers.pandora.annotation.Transactional;
import pers.pandora.constant.LOG;
import pers.pandora.utils.StringUtils;
import pers.pandora.vo.Tuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public class JavassistAOPProxyFactory implements AOPProxyFactory {

    private static final Logger logger = LogManager.getLogger(JavassistAOPProxyFactory.class.getName());

    public static final String PROXY_MARK = "setHandler";

    public static final String DBNAME = "dBPool";

    @Override
    public <T> T createProxyClass(final Class<T> t) {
        final ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(t);
        final Class<?> proxyClass = proxyFactory.createClass();
        T javassistProxy = null;
        try {
            javassistProxy = (T) proxyClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            logger.error("createProxyClass" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getMessage());
        }
        final JavassistInterceptor interceptor = new JavassistInterceptor();
        interceptor.setClassName(t.getName());
        assert javassistProxy != null;
        ((ProxyObject) javassistProxy).setHandler(interceptor);
        return javassistProxy;
    }

    @Override
    public void clear() {
        AFTERHANDlES.clear();
        BEFOREHANDlES.clear();
        THROWHANDlES.clear();
        OBJECTS.clear();
        TRANSACTIONS.remove();
        DBPOOLS.clear();
    }

    private final class JavassistInterceptor implements MethodHandler {

        private String className;

        public void setClassName(String className) {
            this.className = className;
        }

        private boolean checkNoRollBackException(final String message, final String[] no_roll_backs) {
            for (String no_roll_back : no_roll_backs) {
                if (message.startsWith(no_roll_back)) {
                    return false;
                }
            }
            return true;
        }

        private Object proxyTransaction(final Object target, final Object[] args, final Method method, final Transactional transactional) throws SQLException {
            Object result = null;
            PoolConnection connection;
            String dbName = transactional.dbPool();
            if (!StringUtils.isNotEmpty(dbName)) {
                dbName = DBNAME;
            }
            final DBPool dbPool = DBPOOLS.get(dbName);
            assert dbPool != null;
            int level;
            //close db auto-commit
            try {
                connection = dbPool.getConnection();
            } catch (SQLException e) {
                logger.error("proxy invoke" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                throw e;
            }
            assert connection != null;
            final Connection cur = connection.getConnection();
            try {
                cur.setAutoCommit(false);
                level = cur.getTransactionIsolation();
                cur.setTransactionIsolation(transactional.isolation());
                //Binding transactions through ThreadLocal
                if (transactional.propagation() == Propagation.REQUIRES_NEW) {
                    connection.setTransNew((byte) 0);
                }
                TRANSACTIONS.set(connection);
            } catch (SQLException e) {
                logger.error("connection set auto_commit" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                throw e;
            }
            try {
                result = method.invoke(target, args);
                //commit
            } catch (IllegalAccessException | InvocationTargetException e) {
                //execute rollback
                if (checkNoRollBackException(e.getCause().toString(), transactional.no_rollback_exception())) {
                    try {
                        cur.rollback();
                        logger.debug("method:" + LOG.LOG_PRE + "trigger connection rollback" + LOG.LOG_POS, method.getName(), LOG.ERROR_DESC, e.getCause());
                    } catch (SQLException e1) {
                        logger.error("connection rollback" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getCause());
                        throw e1;
                    }
                }
            }
            try {
                cur.setAutoCommit(true);
                cur.setTransactionIsolation(level);
                TRANSACTIONS.remove();
                dbPool.commit(connection);
            } catch (SQLException e) {
                logger.error("connection set auto_commit" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                throw e;
            }
            return result;
        }

        public Object invoke(Object self, Method parentMethod, Method proceed, Object[] args) throws InvocationTargetException, IllegalAccessException {
            final JoinPoint joinPoint = new JoinPoint();
            joinPoint.setProxyClassName(className);
            final StringBuilder methodName = new StringBuilder(parentMethod.getName());
            methodName.append(LEFT_BRACKET);
            final Class<?>[] types = parentMethod.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                methodName.append(types[i].getName());
                if (i != types.length - 1) {
                    methodName.append(COMMA);
                }
            }
            methodName.append(RIGHT_BRACKET);
            joinPoint.setMethodName(methodName.toString());
            joinPoint.setParams(args);
            //before
            try {
                exec(BEFOREHANDlES, joinPoint);
            } catch (InvocationTargetException | IllegalAccessException e) {
                logger.error("exec proxy method:" + LOG.LOG_PRE + LOG.LOG_POS, parentMethod.getName(), LOG.EXCEPTION_DESC, e.getCause());
            }
            Object ret = null;
            try {
                final Transactional transactional = parentMethod.getAnnotation(Transactional.class);
                if (transactional != null) {
                    ret = proxyTransaction(self, joinPoint.getParams(), proceed, transactional);
                } else {
                    ret = proceed.invoke(self, joinPoint.getParams());
                }
                //after
                joinPoint.setReturnValue(ret);
                exec(AFTERHANDlES, joinPoint);
            } catch (IllegalAccessException | InvocationTargetException | SQLException e) {
                //throw
                joinPoint.setException(e.getCause());
                exec(THROWHANDlES, joinPoint);
            }
            return ret;
        }

        private void exec(final Set<Tuple<Integer, String, Method>> set, final JoinPoint joinPoint) throws InvocationTargetException, IllegalAccessException {
            final String proxyMethod = joinPoint.getProxyClassName() + METHOD_SEPARATOR + joinPoint.getMethodName();
            for (Tuple<Integer, String, Method> tuple : set) {
                if (proxyMethod.matches(tuple.getK2())) {
                    tuple.getV().invoke(OBJECTS.get(tuple.getV()), joinPoint);
                }
            }
        }
    }
}