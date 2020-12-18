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

import javassist.util.proxy.MethodHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.annotation.*;
import pers.pandora.constant.LOG;
import pers.pandora.utils.ClassUtils;
import pers.pandora.utils.CollectionUtil;
import pers.pandora.utils.StringUtils;
import pers.pandora.vo.Tuple;

public final class BeanPool {

    private static final Logger logger = LogManager.getLogger(BeanPool.class);

    private static final ThreadLocal<Properties> prop = ThreadLocal.withInitial(Properties::new);

    private static final ThreadLocal singleton = ThreadLocal.withInitial(() -> null);

    private final Map<String, Object> beans = new ConcurrentHashMap<>(16);

    private final Map<Class<?>, List<Object>> typeBeans = new ConcurrentHashMap<>(16);

    private Map<Object, List<Field>> unBeanInjectMap = new ConcurrentHashMap<>(16);

    private Map<String, Integer> in = new ConcurrentHashMap<>(16);

    private Map<String, Method> beanMethods = new ConcurrentHashMap<>(16);

    private Map<String, List<String>> out = new ConcurrentHashMap<>(16);

    private Map<Method, Object> configs = new ConcurrentHashMap<>(16);

    private Map<Method, String[]> methodParams = new ConcurrentHashMap<>(16);

    private List<Class<?>> controllerAndService = new CopyOnWriteArrayList<>();

    private ThreadPoolExecutor executor;

    private Set<String> interceptors;

    private AOPProxyFactory aopProxyFactory;

    private List<Future<Boolean>> result;
    //Thread pool minimum number of cores
    private int minCore = 5;
    //Thread pool maximum number of cores
    private int maxCore = minCore + 5;
    //Thread idle time
    private long keepAlive = 10;
    //Thread idle time unit
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    //Timeout waiting for class loading time
    private long timeOut = 60 * 10;
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
        this.timeOut = timeout;
    }

    public int getMinCore() {
        return minCore;
    }

    public void setMinCore(int minCore) {
        this.minCore = minCore;
    }

    public int getMaxCore() {
        return maxCore;
    }

    public void setMaxCore(int maxCore) {
        this.maxCore = maxCore;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public long getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(long keepAlive) {
        this.keepAlive = keepAlive;
    }

    //all paths should exists in SRC,it's not supported for regex pattern
    public void init(String... paths) {
        if (paths == null || paths.length == 0) {
            logger.warn("No path loaded");
            return;
        }
        executor = new ThreadPoolExecutor(minCore, maxCore, keepAlive, timeUnit, new LinkedBlockingQueue<>());
        result = new ArrayList<>(10);
        //init AOP Config
        if (aopPaths != null && aopPaths.length != 0) {
            interceptors = new CopyOnWriteArraySet<>();
            for (String path : aopPaths) {
                scanFile(checkPath(path), true);
            }
        }
        waitFutures(result, timeOut, timeOutUnit);
        result.clear();
        //init bean
        for (String path : paths) {
            scanFile(checkPath(path), false);
        }
        waitFutures(result, timeOut, timeOutUnit);
        result.clear();
        //Topology injection
        topologyBean();
        //Controller auto-injected
        injectControllerAndService();
        waitFutures(result, timeOut, timeOutUnit);
        executor.shutdownNow();
        //Attribute cyclic dependency injection
        injectValueForAutowired();
        executor = null;
        result = null;
    }

    private void injectControllerAndService() {
        controllerAndService.stream().filter(tClass -> getBeanByType(tClass) == null)
                .forEach(tClass -> {
                    boolean annotation = tClass.isAnnotationPresent(Controller.class);
                    result.add(executor.submit(new IOTask(tClass.getName(), annotation ? Controller.class : Service.class, false)));
                });
        controllerAndService = null;
    }

    private void topologyBean() {
        ArrayDeque<String> q = new ArrayDeque<>(beans.size());
        beans.forEach((k, v) -> q.addLast(k));
        String cur;
        List<String> next;
        while (q.size() > 0) {
            cur = q.getFirst();
            q.pollFirst();
            next = out.get(cur);
            if (CollectionUtil.isNotEmptry(next)) {
                next.forEach(name -> {
                    in.put(name, in.get(name) - 1);
                    if (in.get(name) == 0) {
                        Method method = beanMethods.get(name);
                        assert method != null;
                        Class<?>[] types = method.getParameterTypes();
                        Object[] params = new Object[types.length];
                        String[] names = methodParams.get(method);
                        for (int i = 0; i < types.length; i++) {
                            params[i] = beans.get(names[i]);
                        }
                        createBean(method, name, configs.get(method), params);
                        q.addLast(name);
                    }
                });
            }
        }
        in = null;
        out = null;
        methodParams = null;
        configs = null;
        beanMethods = null;
    }

    public static void waitFutures(List<Future<Boolean>> result, long timeOut, TimeUnit timeOutUnit) {
        for (Future future : result) {
            try {
                future.get(timeOut, timeOutUnit);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("waitFutures" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            }
        }
    }

    public static String checkPath(String path) {
        if (!path.startsWith(ROOTPATH)) {
            path = ROOTPATH + path;
        }
        return path.replaceAll(FILE_REGEX_SPLITER, String.valueOf(PATH_SEPARATOR));
    }

    private void injectValueForAutowired() {
        unBeanInjectMap.forEach((k, v) -> {
            for (Field field : v) {
                Autowired fieldSrc = field.getAnnotation(Autowired.class);
                String name = fieldSrc.value();
                //Try byName injection first. If there is no corresponding bean, try byType injection again
                if (!StringUtils.isNotEmpty(name)) {
                    name = field.getName();
                }
                if (beans.containsKey(name)) {
                    try {
                        field.set(k, beans.get(name));
                    } catch (IllegalAccessException e) {
                        //ignore
                    }
                } else {
                    try {
                        field.set(k, getBeanByType(field.getType()));
                    } catch (IllegalAccessException e) {
                        //ignore
                    }
                }
            }
        });
        unBeanInjectMap = null;
    }

    public <T> T getBean(String beanName) {
        if (StringUtils.isNotEmpty(beanName)) {
            return (T) beans.get(beanName);
        }
        return null;
    }

    public <T> T getBeanByType(Class<T> tClass) {
        if (tClass != null) {
            List<Object> objects = typeBeans.get(tClass);
            return objects != null && objects.size() == 1 ? (T) objects.get(0) : null;
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
                        result.add(executor.submit(new IOTask(className, null, aop)));
                    }
                }
            }
        }
    }

    private void createBean(Method method, String nameTemp, Object config, Object[] params) {
        //Save meta object,Not a post proxy object
        Object obj;
        try {
            obj = method.invoke(config, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("createBean" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            return;
        }
        initBean(obj, nameTemp);
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
        if (template == null) {
            if (t.isAnnotationPresent(Configruation.class)) {
                Object config, obj;
                try {
                    config = t.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("scanBean" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
                    return;
                }
                //subclass or parent class have all public methods
                int i;
                String[] args;
                String nameTemp;
                Class<?>[] params;
                Object[] objs;
                boolean delay;
                Annotation annotation;
                for (Method method : t.getMethods()) {
                    annotation = method.getAnnotation(Bean.class);
                    if (annotation != null) {
                        nameTemp = ((Bean) annotation).value();
                        if (!StringUtils.isNotEmpty(nameTemp)) {
                            nameTemp = method.getName();
                        }
                        //the same name,only save the first one
                        if (beans.containsKey(nameTemp)) {
                            logger.warn("Duplicate bean name");
                            continue;
                        }
                        i = 0;
                        delay = false;
                        params = method.getParameterTypes();
                        args = new String[params.length];
                        for (Annotation[] annotations : method.getParameterAnnotations()) {
                            for (Annotation an : annotations) {
                                //the same @Autowired, only save the first one
                                if (an instanceof Autowired) {
                                    args[i] = ((Autowired) an).value();
                                    break;
                                }
                            }
                            i++;
                        }
                        i = 0;
                        objs = new Object[params.length];
                        for (Class<?> param : params) {
                            if (!ClassUtils.checkBasicClass(param)) {
                                if (!StringUtils.isNotEmpty(args[i])) {
                                    args[i] = Character.toLowerCase(param.getSimpleName().charAt(0)) + param.getSimpleName().substring(1);
                                }
                                obj = beans.get(args[i]);
                                if (obj != null || (obj = getBeanByType(param)) != null) {
                                    objs[i] = obj;
                                } else {
                                    delay = true;
                                    in.put(nameTemp, in.getOrDefault(nameTemp, 0) + 1);
                                    //DCL
                                    if (!out.containsKey(args[i])) {
                                        synchronized (out) {
                                            if (!out.containsKey(args[i])) {
                                                out.put(args[i], new CopyOnWriteArrayList<>());
                                            }
                                        }
                                    }
                                    out.get(args[i]).add(nameTemp);
                                }
                            }
                            i++;
                        }
                        beanMethods.put(nameTemp, method);
                        if (!delay) {
                            createBean(method, nameTemp, config, objs);
                        } else {
                            configs.put(method, config);
                            methodParams.put(method, args);
                        }
                    }
                }
            } else if (t.isAnnotationPresent(Controller.class) || t.isAnnotationPresent(Service.class)) {
                controllerAndService.add(t);
            }
        } else if (template == Controller.class || template == Service.class) {
            if (getBeanByType(t) != null) {
                return;
            }
            try {
                initBean(ClassUtils.getClass(t, null), Character.toLowerCase(t.getSimpleName().charAt(0))
                        + t.getSimpleName().substring(1));
            } catch (IllegalAccessException | InstantiationException e) {
                logger.error("scanBean" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e);
            }
        } else if (template == PropertySource.class) {
            PropertySource propertySource = t.getAnnotation(PropertySource.class);
            String filePath = propertySource != null ? propertySource.value() : null;
            if (!StringUtils.isNotEmpty(filePath)) {
                filePath = t.getName() + FILE_SPLITER + PROPERTIES;
            }
            loadProperties(t, filePath);
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

    private void initBean(Object obj, String objName) {
        assert obj != null;
        Class<?> objTarget = obj.getClass();
        String target = obj.getClass().getName();
        int cnt = 1;
        if (aopProxyFactory != null && interceptors.stream().anyMatch(target::matches)) {
            try {
                objTarget.getMethod(JavassistAOPProxyFactory.PROXY_MARK, MethodHandler.class);
            } catch (NoSuchMethodException e) {
                //The bean has not yet been represented
                obj = ClassUtils.copy(obj.getClass(), obj, aopProxyFactory.createProxyClass(obj.getClass()));
                cnt++;
            }
        }
        singleton.set(obj);
        injectValue(objTarget);
        beans.put(objName, obj);
        List<Class<?>> parents = new ArrayList<>(objTarget.getInterfaces().length + cnt);
        Collections.addAll(parents, objTarget.getInterfaces());
        while (objTarget != Object.class) {
            parents.add(objTarget);
            objTarget = objTarget.getSuperclass();
        }
        for (Class<?> parent : parents) {
            //DCL
            if (!typeBeans.containsKey(parent)) {
                synchronized (typeBeans) {
                    if (!typeBeans.containsKey(parent)) {
                        typeBeans.put(parent, new CopyOnWriteArrayList<>());
                    }
                }
            }
            typeBeans.get(parent).add(obj);
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
            if (source.exists() && !source.isDirectory()) {
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

        private Class<?> template;

        IOTask(String className, Class<?> template, boolean aop) {
            this.aop = aop;
            this.template = template;
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
                    scanBean(tClass, null, template, null);
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
            //current class all methods
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