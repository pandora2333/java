package pers.pandora.mvc;

import javassist.Modifier;
import jdk.internal.org.objectweb.asm.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.annotation.*;
import pers.pandora.constant.LOG;
import pers.pandora.core.BeanPool;
import pers.pandora.core.WebSocketSession;
import pers.pandora.utils.CollectionUtil;
import pers.pandora.utils.StringUtils;
import pers.pandora.vo.Pair;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.interceptor.Interceptor;
import pers.pandora.core.Request;
import pers.pandora.core.Response;
import pers.pandora.utils.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.*;

/**
 * 1.It handles the mapping relationship between URL path and controller
 * 2.It initializes all interceptors that implement the interceptor interface
 * 3.It assigns MVC method parameters
 * 4.It handles the mapping relationship between URL path and WebSocket connection
 */
public final class RequestMappingHandler {

    private static final Logger logger = LogManager.getLogger(RequestMappingHandler.class);
    //for @Controller
    //url - method
    private final Map<String, Method> mappings = new ConcurrentHashMap<>(16);
    //method - controller(singleton instance)
    private final Map<Method, Object> controllers = new ConcurrentHashMap<>(16);
    //restful-param-path
    private final Map<String, Method> regexMappings = new ConcurrentHashMap<>(16);
    //single controller
    private final Map<String, Object> objectMap = new ConcurrentHashMap<>(16);
    //for @WebSocket
    //url - method
    private final Map<String, Method> wsMappings = new ConcurrentHashMap<>(16);
    //method - controller(singleton instance)
    private final Map<Method, Object> wsControllers = new ConcurrentHashMap<>(16);
    //Bean Pool
    private BeanPool beanPool;

    private Set<Pair<Integer, Interceptor>> interceptors;

    private static final Comparator<Pair<Integer, Interceptor>> CMP = (p1, p2) -> {
        int t = p1.getK().compareTo(p2.getK());
        return t != 0 ? t : System.identityHashCode(p1.getV()) - System.identityHashCode(p2.getV());
    };

    public static final String METHOD_SPLITER = "|";

    public static final String CLASS_FILE_POS = "class";

    //Classes loading thread pool related parameters
    private ThreadPoolExecutor executor;

    private List<Future<Boolean>> result;

    public static final String MVC_CLASS = "pers.pandora.mvc.RequestMappingHandler";
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
//    static {
// 类加载阶段启用多线程造成死锁：死循环关键，IOTask任务 scanResolers(Class.forName(xx))加载本类文件时，
// 加载任务被分配到另一个线程，因此初始化类信息加载，两个线程互相等待对方释放类加载锁，造成死锁等待
//    解决方式: 添加IOTask人物时候，排除掉当前类文件
//         init();
//    }

    public Set<Pair<Integer, Interceptor>> getInterceptors() {
        return interceptors;
    }

    public Map<String, Method> getWsMappings() {
        return wsMappings;
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

    public long getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(long keepAlive) {
        this.keepAlive = keepAlive;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public BeanPool getBeanPool() {
        return beanPool;
    }

    public void setBeanPool(BeanPool beanPool) {
        this.beanPool = beanPool;
    }

    /**
     * Execute the controller method to return the request forwarding address
     *
     * @param modelAndView
     * @return
     * @throws Exception
     */
    public void parseUrl(ModelAndView modelAndView) {
        Map<String, Object> valueObject = new HashMap<>(4);
        Method method = mappings.get(modelAndView.getRequest().getReqUrl());
        boolean restful = false;
        String[] tmp = null;
        if (method == null) {
            String reqUrl = modelAndView.getRequest().getReqUrl();
            if (StringUtils.isNotEmpty(reqUrl)) {
                int cnt = modelAndView.getRequest().getPathParams().size();
                for (Map.Entry<String, Method> methodEntry : regexMappings.entrySet()) {
                    tmp = methodEntry.getKey().split(String.valueOf(HTTPStatus.SLASH), -1);
                    if (tmp.length == cnt && reqUrl.matches(methodEntry.getKey().replaceAll(HTTPStatus.PATH_PARAM_SEPARATOE,
                            HTTPStatus.PATH_PARM_REPLACE))) {
                        method = methodEntry.getValue();
                        restful = true;
                        break;
                    }
                }
            }
            if (method == null) {
                modelAndView.setPage(null);
                return;//The corresponding path was not found
            }
        }
        Object controller = controllers.get(method);
        if (controller == null) {
            modelAndView.setPage(null);
            return;//Failed to initialize controller class to generate instance
        }
        if (method != null && method.isAnnotationPresent(ResponseBody.class)) {
            modelAndView.setJson(true);
        }
        RequestMapping requestMethod = method.getAnnotation(RequestMapping.class);
        if (requestMethod == null || !requestMethod.method().equals(modelAndView.getRequest().getMethod())) {
            logger.warn(LOG.LOG_PRE + "target method isn't supported for this method:" + LOG.LOG_PRE,
                    modelAndView.getRequest().getServerName(), modelAndView.getRequest().getMethod());
            modelAndView.getResponse().setCode(HTTPStatus.CODE_405);
            modelAndView.setPage(null);
            return;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object objects[] = new Object[parameterTypes.length];
        String[] paramNames = new String[parameterTypes.length];
        String[] restfulParamNames = new String[parameterTypes.length];
        Class<?>[] genericTypes = new Class<?>[parameterTypes.length];
        Map<String, String> restfulParamValues = new HashMap<>(4);
        Map<String, String> defaultValues = new HashMap<>(4);
        Map<String, List<Object>> params = modelAndView.getRequest().getParams();
        Annotation[][] annotations = method.getParameterAnnotations();
        java.lang.reflect.Type[] types = method.getGenericParameterTypes();

        for (int i = 0; i < types.length; i++) {
            if (types[i] == null) {
                continue;
            }
            if (types[i] instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) types[i];
                genericTypes[i] = (Class<?>) pt.getActualTypeArguments()[0];
            }
        }
        for (int i = 0; i < annotations.length; i++) {
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof RequestParam) {
                    RequestParam param = (RequestParam) annotation;
                    paramNames[i] = param.value();
                    if (StringUtils.isNotEmpty((param.defaultValue()))) {
                        defaultValues.put(param.value(), param.defaultValue());
                    }
                } else if (restful && annotation instanceof PathVariable) {
                    PathVariable param = (PathVariable) annotation;
                    restfulParamNames[i] = param.value();
                } else if (annotation instanceof RequestBody) {
                    RequestBody param = (RequestBody) annotation;
                    paramNames[i] = param.value();
                }
            }
        }
        if (restful && tmp != null) {
            List<String> pathParams = modelAndView.getRequest().getPathParams();
            if (CollectionUtil.isNotEmptry(pathParams)) {
                int len = pathParams.size() == tmp.length ? tmp.length : -1;
                for (int i = 0; i < len; i++) {
                    if (tmp[i].matches(HTTPStatus.PATH_PARAM_SEPARATOE)) {
                        restfulParamValues.put(tmp[i].substring(1, tmp[i].length() - 1), pathParams.get(i));
                    }
                }
            }
        }
        Object target = null, tmpListObj;
        List list;
        List<Object> tmpList = null;
        Map<Integer, Map<String, String>> typeParams;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == Request.class) {
                objects[i] = modelAndView.getRequest();
            } else if (parameterTypes[i] == Response.class) {
                objects[i] = modelAndView.getResponse();
            } else if (parameterTypes[i] == ModelAndView.class) {
                objects[i] = modelAndView;
            } else if (!ClassUtils.checkBasicClass(parameterTypes[i])) {
                if (StringUtils.isNotEmpty(paramNames[i])) {
                    target = modelAndView.getRequest().getParams().get(paramNames[i]);
                    if (target == null) {
                        target = modelAndView.getRequest().getSession().getAttrbuites().get(paramNames[i]);
                    }
                    if (parameterTypes[i] == List.class && genericTypes[i] != null) {
                        if (target == null) {
                            typeParams = modelAndView.getRequest().getListTypeParams().get(paramNames[i]);
                            if (typeParams != null) {
                                tmpList = new ArrayList<>(typeParams.size());
                                for (int j = 0; j < typeParams.size(); j++) {
                                    try {
                                        tmpListObj = ClassUtils.getClass(genericTypes[i], beanPool, false);
                                        ClassUtils.initWithParams(tmpListObj, typeParams.get(j));
                                        tmpList.add(tmpListObj);
                                    } catch (IllegalAccessException | InstantiationException e) {
                                        //ignore
                                    }
                                }
                            }
                        } else {
                            list = (List) target;
                            if (list.get(0).getClass() == genericTypes[i]) {
                                tmpList = list;
                                continue;
                            }
                            if (genericTypes[i] == Integer.class) {
                                tmpList = ClassUtils.convertBasicObject(list, Integer.class);
                            } else if (genericTypes[i] == Long.class) {
                                tmpList = ClassUtils.convertBasicObject(list, Long.class);
                            } else if (genericTypes[i] == Boolean.class) {
                                tmpList = ClassUtils.convertBasicObject(list, Boolean.class);
                            } else if (genericTypes[i] == Character.class) {
                                tmpList = ClassUtils.convertBasicObject(list, Character.class);
                            } else if (genericTypes[i] == Float.class) {
                                tmpList = ClassUtils.convertBasicObject(list, Float.class);
                            } else if (genericTypes[i] == Double.class) {
                                tmpList = ClassUtils.convertBasicObject(list, Double.class);
                            } else if (genericTypes[i] == Byte.class) {
                                tmpList = ClassUtils.convertBasicObject(list, Byte.class);
                            } else if (genericTypes[i] == Short.class) {
                                tmpList = ClassUtils.convertBasicObject(list, Short.class);
                            } else {
                                logger.warn("unknown basic class type");
                            }
                        }
                        target = tmpList;
                    }
                } else {
                    try {
                        target = ClassUtils.getClass(parameterTypes[i], beanPool, true);
                        //requestScope
                        ClassUtils.initWithParams(target, params);
                        //sessionScope
                        ClassUtils.initWithParams(target, modelAndView.getRequest().getSession().getAttrbuites());
                        valueObject.put(parameterTypes[i].getSimpleName().toLowerCase(), target);
                    } catch (IllegalAccessException | InstantiationException e) {
                        //ignore
                    }
                }
                objects[i] = target;
            } else if (StringUtils.isNotEmpty(paramNames[i])) {
                list = params.get(paramNames[i]);
                objects[i] = list != null && list.size() == 1 ? list.get(0) : null;
                if (objects[i] == null) {
                    objects[i] = getValueByType(parameterTypes[i], defaultValues.get(paramNames[i]));
                }
            } else if (restful && StringUtils.isNotEmpty(restfulParamNames[i])) {
                objects[i] = getValueByType(parameterTypes[i], restfulParamValues.get(restfulParamNames[i]));
            } else {
                logger.warn(LOG.LOG_PRE + "parseUrl ModelAndView:" + LOG.LOG_PRE + "by class:" + LOG.LOG_PRE + "=> method:" +
                                LOG.LOG_PRE + LOG.LOG_PRE, modelAndView.getRequest().getServerName(), modelAndView, controller.getClass().getName(),
                        method.getName(), LOG.ERROR_DESC);
                modelAndView.getResponse().setCode(HTTPStatus.CODE_400);
                modelAndView.setPage(null);
                return;
            }
        }
        try {
            Object result = method.invoke(controller, objects);
            if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
                modelAndView.setJson(true);
                result = LOG.NO_CHAR;
            }
            if (modelAndView.isJson()) {
                List<Object> temp = new ArrayList<>(1);
                temp.add(result);
                modelAndView.getRequest().getParams().put(Response.PLAIN, temp);
                modelAndView.setPage(HTTPStatus.PLAIN);
                modelAndView.getResponse().setServlet(MVC_CLASS);
            } else {
                String page = (String) result;
                if (StringUtils.isNotEmpty(page) && page.startsWith(HTTPStatus.REDIRECT)) {
                    modelAndView.getRequest().setRedirect(true);
                    page = page.substring(page.indexOf(HTTPStatus.COLON) + 1);
                }
                modelAndView.setPage(page);
                modelAndView.getRequest().setReqUrl(page);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error(LOG.LOG_PRE + "parseUrl ModelAndView:" + LOG.LOG_PRE + "by class:" + LOG.LOG_PRE + "=> method:" +
                            LOG.LOG_PRE + LOG.LOG_POS, modelAndView.getRequest().getServerName(), modelAndView, controller.getClass().getName(),
                    method.getName(), LOG.ERROR_DESC, e.getCause());
            modelAndView.getResponse().setCode(HTTPStatus.CODE_400);
            modelAndView.setPage(null);
        }
        modelAndView.getRequest().setObjectList(valueObject);
    }

    /**
     * Execute WebSocket callback method
     *
     * @param webSocketSession
     * @param clients
     */
    public void execWSCallBack(WebSocketSession webSocketSession, Map<String, WebSocketSession> clients) {
        if (webSocketSession == null || clients == null) {
            return;
        }
        //uri-all-match pattern
        String reqUrl = webSocketSession.getReqUrl();
        if (!StringUtils.isNotEmpty(reqUrl)) {
            return;
        }
        Method method = wsMappings.get(reqUrl);
        Object wsController = wsControllers.get(method);
        if (method != null && wsController != null) {
            Class<?>[] params = method.getParameterTypes();
            Object[] values = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (params[i] == WebSocketSession.class) {
                    values[i] = webSocketSession;
                } else if (params[i] == Map.class) {
                    values[i] = clients;
                }
            }
            try {
                method.invoke(wsController, values);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("execWSCallBack" + LOG.LOG_POS, LOG.EXCEPTION_DESC, e.getCause());
            }
        } else {
            logger.warn("No Match WS uri resource:" + LOG.LOG_PRE, reqUrl);
        }
    }

    private Object getValueByType(Class<?> parameterType, String defaultValue) {
        if (!StringUtils.isNotEmpty(defaultValue)) {
            return parameterType == String.class ? LOG.NO_CHAR : null;
        }
        if (parameterType == String.class) {
            return defaultValue;
        }
        if (parameterType == Integer.class || parameterType == int.class) {
            return Integer.valueOf(defaultValue);
        }
        if (parameterType == Long.class || parameterType == long.class) {
            return Long.valueOf(defaultValue);
        }
        if (parameterType == Byte.class || parameterType == byte.class) {
            return Byte.valueOf(defaultValue);
        }
        if (parameterType == Short.class || parameterType == short.class) {
            return Short.valueOf(defaultValue);
        }
        if (parameterType == Boolean.class || parameterType == boolean.class) {
            return Boolean.valueOf(defaultValue);
        }
        if (parameterType == Character.class || parameterType == char.class) {
            return defaultValue.charAt(0);
        }
        if (parameterType == Float.class || parameterType == float.class) {
            return Float.valueOf(defaultValue);
        }
        if (parameterType == Double.class || parameterType == double.class) {
            return Double.valueOf(defaultValue);
        }
        return null;
    }

    //Using ASM to operate class bytecode file to get parameter name
    @Deprecated
    public void handleMethodParamNames(Class<?> t) {
        Map<String, Integer> modifers = new HashMap<>(4);
        Map<String, String[]> paramNames = new HashMap<>(4);
        for (Method method : t.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                StringBuilder key = new StringBuilder(t.getName());
                key.append(METHOD_SPLITER);
                key.append(method.getName());
                key.append(METHOD_SPLITER);
                for (Class<?> param : method.getParameterTypes()) {
                    key.append(param.getName());
                    key.append(BeanPool.FILE_SPLITER);
                }
                modifers.put(key.toString(), method.getModifiers());
                paramNames.put(key.toString(), new String[method.getParameterTypes().length]);
            }
        }
        String className = t.getName();
        int lastDotIndex = className.lastIndexOf(BeanPool.FILE_SPLITER);
        className = className.substring(lastDotIndex + 1) + BeanPool.FILE_SPLITER + CLASS_FILE_POS;
        InputStream is = t.getResourceAsStream(className);
        try {
            ClassReader classReader = new ClassReader(is);
            classReader.accept(new ClassVisitor(Opcodes.ASM4) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM4) {
                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                            // The first parameter of a static method is the parameter of the method. If it is an instance method, the first parameter is this
                            StringBuilder key = new StringBuilder(t.getName() + METHOD_SPLITER + name + METHOD_SPLITER);
                            for (Type type : Type.getArgumentTypes(desc)) {
                                key.append(type.getClassName());
                                key.append(BeanPool.FILE_SPLITER);
                            }
                            if (Modifier.isStatic(modifers.get(key))) {
                                paramNames.get(key)[index] = name;
                            } else if (index > 0) {
                                paramNames.get(key)[index - 1] = name;
                            }
                        }
                    };

                }
            }, 0);
        } catch (IOException e) {
            logger.error(LOG.LOG_PRE + "handleMethodParamNames for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                    MVC_CLASS, t.getName(), LOG.EXCEPTION_DESC, e);
        }
    }

    private void scanFile(String path) {
        File files = new File(path);
        if (!files.exists()) {
            files = new File(path + BeanPool.FILE_SPLITER + BeanPool.FILE_POS_MARK);
        }
        if (files.exists()) {
            if (files.isDirectory()) {
                for (File file : Objects.requireNonNull(files.listFiles())) {
                    scanFile(file.getPath());
                }

            } else {
                if (files.getPath().endsWith(BeanPool.FILE_SPLITER + BeanPool.FILE_POS_MARK)) {
                    String className = files.getPath().substring(4).replace(BeanPool.FILE_SPLITER + BeanPool.FILE_POS_MARK, LOG.NO_CHAR).
                            replace(BeanPool.PATH_SPLITER_PATTERN, BeanPool.FILE_SPLITER);
                    if (!className.equals(MVC_CLASS)) {
                        result.add(executor.submit(new IOTask(className)));
                    }
                }
            }
        }
    }

    private void saveUrlPathMapping(Class<?> t, Method method, Map<Method, Object> controllers) {
        try {
            String name = Character.toLowerCase(t.getSimpleName().charAt(0)) + t.getSimpleName().substring(1);
            if (!objectMap.containsKey(name)) {
                Object bean = ClassUtils.getClass(t, beanPool, true);
                objectMap.put(name, bean);
            }
            controllers.put(method, objectMap.get(name));
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(LOG.LOG_PRE + "saveUrlPathMapping for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                    MVC_CLASS, t.getName(), LOG.EXCEPTION_DESC, e);
        }
    }

    private <T> void scanResolers(Class<T> t) {
        //Allow the Annotations are common for the same controller class
        if (t.isAnnotationPresent(Controller.class) || t.isAnnotationPresent(WebSocket.class)) {
            Controller controller = t.getAnnotation(Controller.class);
            WebSocket webSocket = t.getAnnotation(WebSocket.class);
            String parentPath1 = controller != null ? controller.value() : null;
            String parentPath2 = webSocket != null ? webSocket.value() : null;
            for (Method method : t.getDeclaredMethods()) {
                Annotation annotation = method.getAnnotation(RequestMapping.class);
                if (annotation != null) {
                    String subPath = ((RequestMapping) annotation).value();
                    if (subPath.matches(HTTPStatus.PATH_REGEX_MARK)) {
                        regexMappings.put(parentPath1 + subPath, method);
                    } else {
                        savePathRelation(subPath, parentPath1, method, mappings);
                    }
                    saveUrlPathMapping(t, method, controllers);
                }
                annotation = method.getAnnotation(WebSocketMethod.class);
                if (annotation != null) {
                    String subPath = ((WebSocketMethod) annotation).value();
                    savePathRelation(subPath, parentPath2, method, wsMappings);
                    saveUrlPathMapping(t, method, wsControllers);
                }
            }
        }
        Order order = t.getAnnotation(Order.class);
        if (order != null) {
            Class<?>[] interfaces = t.getInterfaces();
            for (Class<?> i : interfaces) {
                if (i == Interceptor.class) {
                    try {
                        interceptors.add(new Pair<>(order.value(), (Interceptor) ClassUtils.getClass(t, beanPool, true)));
                    } catch (InstantiationException | IllegalAccessException e) {
                        logger.error(LOG.LOG_PRE + "scanResolers for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                                MVC_CLASS, t.getName(), LOG.EXCEPTION_DESC, e);
                    }
                    break;
                }
            }
        }
    }

    private void savePathRelation(String subPath, String parentPath, Method method, Map<String, Method> mappings) {
        if (!StringUtils.isNotEmpty(subPath)) {
            mappings.put(parentPath + HTTPStatus.SLASH + method.getName(), method);
        } else {
            mappings.put(parentPath + subPath, method);
        }
    }

    private class IOTask implements Callable<Boolean> {
        private String className;

        IOTask(String className) {
            this.className = className;
        }

        @Override
        public Boolean call() {
            try {
                scanResolers(Class.forName(className,
                        true, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                logger.error(LOG.LOG_PRE + "exec for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                        this, className, LOG.EXCEPTION_DESC, e);
                return false;
            }
            return true;
        }
    }

    public void init(String... paths) {
        if (paths == null || paths.length == 0) {
            logger.warn("No path loaded");
            return;
        }
        result = new ArrayList<>(10);
        interceptors = Collections.synchronizedSortedSet(new TreeSet<>(CMP));
        executor = new ThreadPoolExecutor(minCore, maxCore, keepAlive, timeUnit, new LinkedBlockingQueue<>());
        for (String path : paths) {
            scanFile(BeanPool.checkPath(path));
        }
        BeanPool.waitFutures(result, timeOut, timeOutUnit);
        executor.shutdown();
        executor = null;
        result = null;
    }

}
