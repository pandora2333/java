package pers.pandora.core;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.LOG;
import pers.pandora.vo.Tuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class JavassistAOPProxyFactory implements AOPProxyFactory {

    private static Logger logger = LogManager.getLogger(JavassistAOPProxyFactory.class);

    private static final char METHOD_SEPARATOR = '#';

    @Override
    public <T> T createProxyClass(Class<T> t) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(t);
        Class<?> proxyClass = proxyFactory.createClass();
        T javassistProxy = null;
        try {
            javassistProxy = (T) proxyClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            logger.error("createProxyClass" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getMessage());
        }
        JavassistInterceptor interceptor = new JavassistInterceptor();
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
    }

    private static class JavassistInterceptor implements MethodHandler {

        private String className;

        public void setClassName(String className) {
            this.className = className;
        }

        public Object invoke(Object self, Method parentMethod, Method proceed, Object[] args) throws InvocationTargetException, IllegalAccessException {
            JoinPoint joinPoint = new JoinPoint();
            joinPoint.setProxyClassName(className);
            joinPoint.setMethodName(parentMethod.getName());
            joinPoint.setParams(args);
            //before
            try {
                exec(BEFOREHANDlES, joinPoint);
            } catch (InvocationTargetException | IllegalAccessException e) {
                logger.error("exec proxy method:" + LOG.LOG_PRE + LOG.LOG_POS, parentMethod.getName(), LOG.EXCEPTION_DESC, e);
            }
            Object ret = null;
            try {
                ret = proceed.invoke(self, joinPoint.getParams());
                //after
                joinPoint.setReturnValue(ret);
                exec(AFTERHANDlES, joinPoint);
            } catch (IllegalAccessException | InvocationTargetException e) {
                //throw
                joinPoint.setException(e.getCause());
                exec(THROWHANDlES, joinPoint);
            }
            return ret;
        }

        private void exec(Set<Tuple<Integer, String, Method>> set, JoinPoint joinPoint) throws InvocationTargetException, IllegalAccessException {
            String proxyMethod = joinPoint.getProxyClassName() + METHOD_SEPARATOR + joinPoint.getMethodName();
            for (Tuple<Integer, String, Method> tuple : set) {
                if (proxyMethod.matches(tuple.getK2())) {
                    tuple.getV().invoke(OBJECTS.get(tuple.getV()), joinPoint);
                }
            }
        }
    }
}