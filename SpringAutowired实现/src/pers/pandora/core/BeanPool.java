package pers.pandora.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.annotation.*;
import pers.pandora.constant.LOG;
import pers.pandora.utils.ClassUtils;
import pers.pandora.utils.CollectionUtil;
import pers.pandora.utils.StringUtils;
import pers.pandora.vo.Tuple;

public final class BeanPool {

    private static Logger logger = LogManager.getLogger(BeanPool.class);

    private ThreadLocal<Properties> prop;

    private ThreadLocal singleton;

    private Map<String, Object> beans = new ConcurrentHashMap<>(16);

    private Map<Class<?>, List<Object>> typeBeans = new ConcurrentHashMap<>(16);

    private Map<Object, List<Field>> unBeanInjectMap = new ConcurrentHashMap<>(16);

    private ThreadPoolExecutor executor;

    private Set<String> interceptors;

    private AOPProxyFactory aopProxyFactory;

    private List<Future<Boolean>> result;
    //Thread pool minimum number of cores
    private int minCore = Runtime.getRuntime().availableProcessors();
    //Thread pool maximum number of cores
    private int maxCore = minCore + 5;
    //Thread idle time
    private long keepAlive = 50;
    //Thread idle time unit
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    //Timeout waiting for class loading time
    private long timeout = 5;
    //Timeout wait class load time unit
    private TimeUnit timeOutUnit = TimeUnit.SECONDS;
    //AOP Config for @Aspect
    private String[] aopPaths;

    public static final char PATH_SEPARATOR = '/';
    //Considering that JSP files may produce a large number of class files, it is optimized to obtain them from the SRC source directory
    public static final String ROOTPATH = "src" + PATH_SEPARATOR;

    public static final char FILE_SPLITER = '.';

    public static final String FILE_POS_MARK = "java";

    public static final char PATH_SPLITER_PATTERN = '\\';

    public static final String FILE_REGEX_SPLITER = "\\.";

    public static final String NO_CHAR = "";

    public static final String PROPERTIES = "properties";

    private static final String BEAN_POOL_CLASS = "pers.pandora.core.BeanPool";

    {
        singleton = new ThreadLocal() {
            @Override
            protected Object initialValue() {
                return null;
            }
        };
        prop = new ThreadLocal<Properties>() {
            @Override
            protected Properties initialValue() {
                return new Properties();
            }
        };
    }

    public AOPProxyFactory getAopProxyFactory() {
        return aopProxyFactory;
    }

    public void setAopProxyFactory(AOPProxyFactory aopProxyFactory) {
        this.aopProxyFactory = aopProxyFactory;
    }

    public Set<String> getInterceptors() {
        return interceptors;
    }

    public String[] getAopPaths() {
        return aopPaths;
    }

    public void setAopPaths(String... aopPaths) {
        this.aopPaths = aopPaths;
    }

    public void initThreadPool(int minCore, int maxCore, long keepAlive, TimeUnit timeUnit, long timeout, TimeUnit timeOutUnit) {
        this.minCore = minCore;
        this.maxCore = maxCore;
        this.keepAlive = keepAlive;
        this.timeUnit = timeUnit;
        this.timeOutUnit = timeOutUnit;
        this.timeout = timeout;
    }

    //all paths should exists in SRC,it's not supported for regex pattern
    public void init(String... paths) {
        if (paths == null || paths.length == 0) {
            logger.warn("No path loaded");
            return;
        }
        executor = new ThreadPoolExecutor(minCore, maxCore, keepAlive, timeUnit, new LinkedBlockingQueue<>());
        result = new ArrayList<>();
        //init AOP Config
        if (aopPaths != null && aopPaths.length != 0) {
            interceptors = new CopyOnWriteArraySet<>();
            for (String path : aopPaths) {
                scanFile(checkPath(path), true);
            }
        }
        for (Future future : result) {
            try {
                future.get(timeout, timeOutUnit);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("init" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            }
        }
        result.clear();
        //init bean
        for (String path : paths) {
            scanFile(checkPath(path), false);
        }
        for (Future future : result) {
            try {
                future.get(timeout, timeOutUnit);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("init" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            }
        }
        executor.shutdownNow();
        injectValueForAutowired();
        executor = null;
        result.clear();
        result = null;
    }

    private String checkPath(String path) {
        if (!path.startsWith(ROOTPATH)) {
            path = ROOTPATH + path;
        }
        return path.replaceAll(FILE_REGEX_SPLITER, String.valueOf(PATH_SEPARATOR));
    }

    private void injectValueForAutowired() {
        unBeanInjectMap.forEach((k, v) -> {
            for (Field field : v) {
                Autowired fieldSrc = field.getAnnotation(Autowired.class);
                if (beans.containsKey(fieldSrc.value())) {
                    try {
                        field.set(k, beans.get(fieldSrc.value()));
                    } catch (IllegalAccessException e) {
                        //ignore
                    }
                } else if (fieldSrc.value().equals(LOG.NO_CHAR) && typeBeans.containsKey(field.getType())) {
                    if (typeBeans.get(field.getType()).size() == 1) {
                        try {
                            field.set(k, typeBeans.get(field.getType()).get(0));
                        } catch (IllegalAccessException e) {
                            //ignore
                        }
                    } else {
                        logger.warn("Multiple bean injections of the same type were detected, and the bean name needs to be specified:"
                                + LOG.LOG_POS, LOG.ERROR_DESC, field.getType());
                        return;
                    }
                }
            }
        });
    }

    public <T> T getBean(String beanName) {
        if (StringUtils.isNotEmpty(beanName)) {
            if (beans.get(beanName) != null) {
                return (T) beans.get(beanName);
            }
        }
        return null;
    }

    public <T> T getBeanByType(Class<T> tClass) {
        if (tClass != null) {
            List<Object> objects = typeBeans.get(tClass);
            return CollectionUtil.isNotEmptry(objects) ? (T) objects.get(0) : null;
        }
        return null;
    }

    private void scanFile(String path, boolean aop) {
        File files = new File(path);
        if (!files.exists()) {
            files = new File(path + FILE_SPLITER + FILE_POS_MARK);
        }
        if (files.exists()) {
            if (files.isDirectory()) {
                for (File file : Objects.requireNonNull(files.listFiles())) {
                    scanFile(file.getPath(), aop);
                }
            } else {
                if (files.getPath().endsWith(FILE_SPLITER + FILE_POS_MARK)) {
                    String className = files.getPath().substring(4).replace(FILE_SPLITER + FILE_POS_MARK, NO_CHAR).
                            replace(PATH_SPLITER_PATTERN, FILE_SPLITER);
                    if (!className.equals(BEAN_POOL_CLASS)) {
                        result.add(executor.submit(new IOTask(className, aop)));
                    }
                }
            }
        }
    }

    /**
     * If you want to assign values to the bean property, you must first declare @Configuration and @Bean
     *
     * @param t
     * @param field
     * @param template
     * @param prop
     * @param <T>
     */
    private <T> void scanBean(Class<T> t, Field field, Class template, Properties prop) {
        if (t.isAnnotationPresent(Configruation.class)) {
            Object config, obj;
            try {
                config = t.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("scanBean" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                return;
            }
            String target;
            List<Object> tmp;
            for (Method method : t.getDeclaredMethods()) {
                Annotation annotation = method.getAnnotation(Bean.class);
                if (annotation != null) {
                    String nameTemp = ((Bean) annotation).value();
                    if (!StringUtils.isNotEmpty(nameTemp)) {
                        nameTemp = method.getName();
                    }
                    if (beans.containsKey(nameTemp)) {
                        continue;
                    }
                    //Save meta object,Not a post proxy object
                    Class<?> objTarget;
                    try {
                        obj = method.invoke(config);
                        objTarget = obj.getClass();
                        target = obj.getClass().getName();
                        if (aopProxyFactory != null && interceptors.stream().anyMatch(target::matches)) {
                            obj = ClassUtils.copy(obj.getClass(), obj, aopProxyFactory.createProxyClass(obj.getClass()));
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.error("scanBean" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                        return;
                    }
                    singleton.set(obj);
                    injectValue(objTarget);
                    beans.put(nameTemp, obj);
                    if (typeBeans.containsKey(objTarget)) {
                        typeBeans.get(objTarget).add(obj);
                    } else {
                        tmp = new CopyOnWriteArrayList<>();
                        tmp.add(obj);
                        typeBeans.put(objTarget, tmp);
                    }
                }
            }
        } else if (template == PropertySource.class) {
            for (Annotation annotation : t.getDeclaredAnnotations()) {
                if (annotation instanceof PropertySource) {
                    String filePath = ((PropertySource) annotation).value();
                    if (!StringUtils.isNotEmpty(filePath)) {
                        filePath = t.getName() + FILE_SPLITER + PROPERTIES;
                    }
                    loadProperties(t, filePath);
                }
            }
        } else if (template == Value.class) {
            if (field != null) {
                try {
                    Value fieldSrc = field.getAnnotation(Value.class);
                    String fieldValue = fieldSrc.value();
                    if (!StringUtils.isNotEmpty(fieldValue)) {
                        fieldValue = field.getName();
                    }
                    if (prop.get(fieldValue) == null) {
                        return;
                    }
                    String key = String.valueOf(prop.get(fieldValue));
                    if (field.getType() == int.class || field.getType() == Integer.class) {
                        field.set(singleton.get(), Integer.valueOf(key));
                    } else if (field.getType() == long.class || field.getType() == Long.class) {
                        field.set(singleton.get(), Long.valueOf(key));
                    } else if (field.getType() == float.class || field.getType() == Float.class) {
                        field.set(singleton.get(), Float.valueOf(key));
                    } else if (field.getType() == double.class || field.getType() == Double.class) {
                        field.set(singleton.get(), Double.valueOf(key));
                    } else if (field.getType() == byte.class || field.getType() == Byte.class) {
                        field.set(singleton.get(), Byte.valueOf(key));
                    } else if (field.getType() == short.class || field.getType() == Short.class) {
                        field.set(singleton.get(), Short.valueOf(key));
                    } else if (field.getType() == char.class || field.getType() == Character.class) {
                        field.set(singleton.get(), key.charAt(0));
                    } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        field.set(singleton.get(), Boolean.valueOf(key));
                    } else if (field.getType() == String.class) {
                        field.set(singleton.get(), key);
                    } else {
                        logger.warn("Non-basic data types and strings cannot be assigned with @ value:"
                                + LOG.LOG_POS, LOG.ERROR_DESC, field.getType());
                    }
                } catch (Exception e) {
                    logger.error("Illegal type assignment" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                }
            }
        }
    }

    //Automatic injection of attribute values
    private <T> void injectValue(Class<T> tClass) {
        scanBean(tClass, null, PropertySource.class, null);
    }

    /**
     * Load configuration file
     * Note:you can simply use the @ Autowired function without specifying the configuration file
     *
     * @param tClass
     * @param file
     */
    private void loadProperties(Class tClass, String file) {
        try {
            File source = new File(ROOTPATH + file);
            if (source.exists()) {
                FileInputStream inputStream = new FileInputStream(source);
                prop.get().load(inputStream);
                inputStream.close();
            }
            for (Field field : tClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Value.class)) {
                    scanBean(tClass, field, Value.class, prop.get());
                } else if (!Modifier.isPrivate(field.getModifiers()) && field.isAnnotationPresent(Autowired.class)) {
                    Autowired fieldSrc = field.getAnnotation(Autowired.class);
                    if (beans.containsKey(fieldSrc.value())) {
                        try {
                            field.set(singleton.get(), beans.get(fieldSrc.value()));
                        } catch (IllegalAccessException e) {
                            //ignore
                        }
                    } else if (!StringUtils.isNotEmpty(fieldSrc.value()) && typeBeans.containsKey(field.getType())) {
                        if (typeBeans.get(field.getType()).size() == 1) {
                            try {
                                field.set(singleton.get(), typeBeans.get(field.getType()).get(0));
                            } catch (IllegalAccessException e) {
                                //ignore
                            }
                        } else {
                            logger.warn("Multiple bean injections of the same type were detected, and the bean name needs to be specified:"
                                    + LOG.LOG_POS, LOG.ERROR_DESC, field.getType());
                        }
                    } else {
                        if (unBeanInjectMap.containsKey(singleton.get())) {
                            unBeanInjectMap.get(singleton.get()).add(field);
                        } else {
                            List<Field> tmp = new CopyOnWriteArrayList<>();
                            tmp.add(field);
                            unBeanInjectMap.put(singleton.get(), tmp);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("loadProperties" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
        }
    }

    private class IOTask implements Callable<Boolean> {

        private String className;

        private boolean aop;

        IOTask(String className, boolean aop) {
            this.aop = aop;
            this.className = className;
        }

        @Override
        public Boolean call() {
            singleton.remove();
            try {
                Class<?> tClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
                if (aop) {
                    scanAOP(tClass);
                } else {
                    scanBean(tClass, null, null, null);
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                logger.error(LOG.LOG_PRE + "exec for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                        this, className, LOG.EXCEPTION_DESC, e);
                return false;
            }
            return true;
        }
    }

    private void scanAOP(Class<?> tClass) throws IllegalAccessException, InstantiationException {
        if (aopProxyFactory == null) {
            return;
        }
        Annotation annotation = tClass.getAnnotation(Aspect.class);
        if (annotation != null) {
            int order = ((Aspect) annotation).value();
            String cutPonit;
            for (Method method : tClass.getDeclaredMethods()) {
                method.setAccessible(true);
                annotation = method.getAnnotation(Before.class);
                boolean aop = false;
                if (annotation != null) {
                    cutPonit = ((Before) annotation).value();
                    interceptors.add(cutPonit);
                    aop = true;
                    aopProxyFactory.BEFOREHANDlES.add(new Tuple<>(order, cutPonit, method));
                }
                annotation = method.getAnnotation(After.class);
                if (annotation != null) {
                    cutPonit = ((After) annotation).value();
                    interceptors.add(cutPonit);
                    aop = true;
                    aopProxyFactory.AFTERHANDlES.add(new Tuple<>(order, cutPonit, method));
                }
                annotation = method.getAnnotation(Throw.class);
                if (annotation != null) {
                    cutPonit = ((Throw) annotation).value();
                    interceptors.add(cutPonit);
                    aop = true;
                    aopProxyFactory.THROWHANDlES.add(new Tuple<>(order, cutPonit, method));
                }
                //No AOP Method,No Create Executable Object
                if (aop) {
                    aopProxyFactory.OBJECTS.put(method, aopProxyFactory.OBJECTS.getOrDefault(method, tClass.newInstance()));
                }
            }
        }
    }
}


 	
