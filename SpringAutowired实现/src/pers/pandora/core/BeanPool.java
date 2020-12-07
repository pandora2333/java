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
import pers.pandora.utils.CollectionUtil;
import pers.pandora.utils.StringUtils;

public final class BeanPool {

    private static Logger logger = LogManager.getLogger(BeanPool.class);

    private static ThreadLocal<Properties> prop;

    private static ThreadLocal singleton;

    private static Map<String, Object> beans = new ConcurrentHashMap<>(16);

    private static Map<Class<?>, List<Object>> typeBeans = new ConcurrentHashMap<>(16);

    private static Map<Object, List<Field>> unBeanInjectMap = new ConcurrentHashMap<>(16);

    private static ThreadPoolExecutor executor;

    private static List<Future<Boolean>> result;
    //Thread pool minimum number of cores
    public static int minCore = Runtime.getRuntime().availableProcessors();
    //Thread pool maximum number of cores
    public static int maxCore = minCore + 5;
    //Thread idle time
    public static long keepAlive = 50;
    //Thread idle time unit
    public static TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    //Timeout waiting for class loading time
    public static long timeout = 5;
    //Timeout wait class load time unit
    public static TimeUnit timeOutUnit = TimeUnit.SECONDS;
    //Considering that JSP files may produce a large number of class files, it is optimized to obtain them from the SRC source directory
    public static final String ROOTPATH = "src/";

    public static final String METHOD_SPLITER = "|";

    public static final char FILE_SPLITER = '.';

    public static final String FILE_POS_MARK = "java";

    public static final String CLASS_FILE_POS = "class";

    public static final char PATH_SPLITER_PATTERN = '\\';

    public static final char JAVA_PACKAGE_SPLITER = '.';

    public static final String NO_CHAR = "";

    public static final String PROPERTIES = "properties";

    private static final String BEAN_POOL_CLASS = "pers.pandora.core.BeanPool";

    static {
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

    public static void init() {
        executor = new ThreadPoolExecutor(minCore, maxCore, keepAlive, timeUnit, new LinkedBlockingQueue<>());
        result = new ArrayList<>();
        scanFile(ROOTPATH);
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

    private static void injectValueForAutowired() {
        unBeanInjectMap.forEach((k, v) -> {
            for (Field field : v) {
                Autowired fieldSrc = field.getAnnotation(Autowired.class);
                if (beans.containsKey(fieldSrc.value())) {
                    try {
                        field.set(k, beans.get(fieldSrc.value()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else if (fieldSrc.value().equals(LOG.NO_CHAR) && typeBeans.containsKey(field.getType())) {
                    if (typeBeans.get(field.getType()).size() == 1) {
                        try {
                            field.set(k, typeBeans.get(field.getType()).get(0));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
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

    public static <T> T getBean(String beanName, Class<T> clazz) {
        if (StringUtils.isNotEmpty(beanName)) {
            if (beans.get(beanName) != null && beans.get(beanName).getClass() == clazz) {
                return (T) beans.get(beanName);
            }
        }
        return null;
    }

    public static <T> T getBeanByType(Class<T> t) {
        if (t != null) {
            List<Object> objects = typeBeans.get(t);
            return CollectionUtil.isNotEmptry(objects) ? (T) objects.get(0) : null;
        }
        return null;
    }

    private static void scanFile(String path) {
        File files = new File(path);
        if (files != null) {
            if (files.isDirectory()) {
                for (File file : files.listFiles()) {
                    scanFile(file.getPath());
                }
            } else {
                if (files.getPath().endsWith(FILE_SPLITER + FILE_POS_MARK)) {
                    String className = files.getPath().substring(4).replace(FILE_SPLITER + FILE_POS_MARK, NO_CHAR).
                            replace(PATH_SPLITER_PATTERN, JAVA_PACKAGE_SPLITER);
                    if (!className.equals(BEAN_POOL_CLASS)) {
                        result.add(executor.submit(new IOTask(className)));
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
    private static <T> void scanBean(Class<T> t, Field field, Class template, Properties prop) {
        if (t.isAnnotationPresent(Configruation.class)) {
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
                    Object obj = null;
                    try {
                        obj = method.invoke(t.newInstance());
                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                        logger.error("scanBean" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                    }
                    singleton.set(obj);
                    injectValue(obj.getClass(), nameTemp);
                    beans.put(nameTemp, obj);
                    if (typeBeans.containsKey(obj.getClass())) {
                        typeBeans.get(obj.getClass()).add(obj);
                    } else {
                        List<Object> tmp = new CopyOnWriteArrayList<>();
                        tmp.add(obj);
                        typeBeans.put(obj.getClass(), tmp);
                    }
                }
            }
        } else if (template == PropertySource.class) {
            for (Annotation annotation : t.getDeclaredAnnotations()) {
                if (annotation instanceof PropertySource) {
                    String filePath = ((PropertySource) annotation).value();
                    if (!StringUtils.isNotEmpty(filePath)) {
                        filePath = t.getName() + JAVA_PACKAGE_SPLITER + PROPERTIES;
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
    private static <T> void injectValue(Class<T> clazz, String beanName) {
        scanBean(clazz, null, PropertySource.class, null);
    }

    /**
     * Load configuration file
     * Note:you can simply use the @ Autowired function without specifying the configuration file
     *
     * @param clazz
     * @param file
     */
    private static void loadProperties(Class clazz, String file) {
        try {
            File source = new File(ROOTPATH + file);
            if (source != null && source.exists()) {
                prop.get().load(new FileInputStream(source));
            }
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Value.class)) {
                    scanBean(clazz, field, Value.class, prop.get());
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

    private static class IOTask implements Callable<Boolean> {

        private String className;

        public IOTask(String className) {
            this.className = className;
        }

        @Override
        public Boolean call() {
            singleton.remove();
            try {
                scanBean(Class.forName(className, true, Thread.currentThread().getContextClassLoader())
                        , null, null, null);
            } catch (ClassNotFoundException e) {
                logger.error(LOG.LOG_PRE + "exec for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                        this, className, LOG.EXCEPTION_DESC, e);
                return false;
            }
            return true;
        }
    }
}


 	
