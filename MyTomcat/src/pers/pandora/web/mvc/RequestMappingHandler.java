package pers.pandora.web.mvc;

import javassist.Modifier;
import jdk.internal.org.objectweb.asm.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.common.constant.LOG;
import pers.pandora.common.utils.ClassUtils;
import pers.pandora.common.utils.CollectionUtil;
import pers.pandora.common.utils.StringUtils;
import pers.pandora.common.vo.Pair;
import pers.pandora.common.web.Controller;
import pers.pandora.common.web.WebSocket;
import pers.pandora.om.core.BeanPool;
import pers.pandora.web.annotation.*;
import pers.pandora.web.interceptor.Interceptor;
import pers.pandora.web.core.WebSocketSession;
import pers.pandora.web.constant.HTTPStatus;
import pers.pandora.web.core.Request;
import pers.pandora.web.core.Response;

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

    public static final String MVC_CLASS = "pers.pandora.web.mvc.RequestMappingHandler";

    private static final Logger logger = LogManager.getLogger(MVC_CLASS);
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
    public void parseUrl(final ModelAndView modelAndView) {
        final Map<String, Object> valueObject = new HashMap<>(4);
        Method method = mappings.get(modelAndView.getRequest().getReqUrl());
        boolean restful = false;
        String[] tmp = null;
        if (method == null) {
            final String reqUrl = modelAndView.getRequest().getReqUrl();
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
                //The corresponding path was not found
                modelAndView.setPage(null);
                return;
            }
        }
        final Object controller = controllers.get(method);
        if (controller == null) {
            //Failed to initialize controller class to generate instance
            modelAndView.setPage(null);
            return;
        }
        if (method != null && method.isAnnotationPresent(ResponseBody.class)) {
            modelAndView.setJson(true);
        }
        final RequestMapping requestMethod = method.getAnnotation(RequestMapping.class);
        if (requestMethod == null || !requestMethod.method().equals(modelAndView.getRequest().getMethod())) {
            logger.warn(LOG.LOG_PRE + "target method isn't supported for this method:" + LOG.LOG_PRE,
                    modelAndView.getRequest().getServerName(), modelAndView.getRequest().getMethod());
            modelAndView.getResponse().setCode(HTTPStatus.CODE_405);
            modelAndView.setPage(null);
            return;
        }
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Object objects[] = new Object[parameterTypes.length];
        final String[] paramNames = new String[parameterTypes.length];
        final String[] restfulParamNames = new String[parameterTypes.length];
        final Class<?>[] genericTypes = new Class<?>[parameterTypes.length];
        final Map<String, String> restfulParamValues = new HashMap<>(4);
        final Map<String, String> defaultValues = new HashMap<>(4);
        final Map<String, List<Object>> params = modelAndView.getRequest().getParams();
        final Annotation[][] annotations = method.getParameterAnnotations();
        final java.lang.reflect.Type[] types = method.getGenericParameterTypes();
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
                    final RequestParam param = (RequestParam) annotation;
                    paramNames[i] = param.value();
                    if (StringUtils.isNotEmpty((param.defaultValue()))) {
                        defaultValues.put(param.value(), param.defaultValue());
                    }
                } else if (restful && annotation instanceof PathVariable) {
                    final PathVariable param = (PathVariable) annotation;
                    restfulParamNames[i] = param.value();
                } else if (annotation instanceof RequestBody) {
                    final RequestBody param = (RequestBody) annotation;
                    paramNames[i] = param.value();
                }
            }
        }
        if (restful && tmp != null) {
            final List<String> pathParams = modelAndView.getRequest().getPathParams();
            if (CollectionUtil.isNotEmptry(pathParams)) {
                final int len = pathParams.size() == tmp.length ? tmp.length : -1;
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
        final Map<String, List<Object>> requestMap = modelAndView.getRequest().getParams();
        final Map<String, Object> attrbuites = modelAndView.getRequest().getSession().getAttrbuites();
        final Map<String, Map<Integer, Map<String, String>>> listTypeParams = modelAndView.getRequest().getListTypeParams();
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
                    target = requestMap.get(paramNames[i]);
                    if (target == null) {
                        target = attrbuites.get(paramNames[i]);
                    }
                    if (parameterTypes[i] == List.class && genericTypes[i] != null) {
                        if (target == null) {
                            typeParams = listTypeParams.get(paramNames[i]);
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
                        ClassUtils.initWithParams(target, attrbuites);
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
                    objects[i] = defaultValues.get(paramNames[i]);
                }
                if (parameterTypes[i] != objects[i].getClass() && objects[i].getClass() == String.class) {
                    objects[i] = getValueByType(parameterTypes[i], (String) objects[i]);
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
                final List<Object> temp = new LinkedList<>();
                temp.add(result);
                requestMap.put(Response.PLAIN, temp);
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
    public void execWSCallBack(final WebSocketSession webSocketSession, final Map<String, WebSocketSession> clients) {
        if (webSocketSession == null || clients == null) {
            return;
        }
        //uri-all-match pattern
        final String reqUrl = webSocketSession.getReqUrl();
        if (!StringUtils.isNotEmpty(reqUrl)) {
            return;
        }
        final Method method = wsMappings.get(reqUrl);
        final Object wsController = wsControllers.get(method);
        if (method != null && wsController != null) {
            final Class<?>[] params = method.getParameterTypes();
            final Object[] values = new Object[params.length];
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

    private Object getValueByType(final Class<?> parameterType, final String defaultValue) {
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
    public void handleMethodParamNames(final Class<?> t) {
        final Map<String, Integer> modifers = new HashMap<>(4);
        final Map<String, String[]> paramNames = new HashMap<>(4);
        StringBuilder key = new StringBuilder();
        for (Method method : t.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                key.append(t.getName());
                key.append(METHOD_SPLITER);
                key.append(method.getName());
                key.append(METHOD_SPLITER);
                for (Class<?> param : method.getParameterTypes()) {
                    key.append(param.getName());
                    key.append(BeanPool.FILE_SPLITER);
                }
                modifers.put(key.toString(), method.getModifiers());
                paramNames.put(key.toString(), new String[method.getParameterTypes().length]);
                key.delete(0, key.length());
            }
        }
        String className = t.getName();
        int lastDotIndex = className.lastIndexOf(BeanPool.FILE_SPLITER);
        className = className.substring(lastDotIndex + 1) + BeanPool.FILE_SPLITER + CLASS_FILE_POS;
        final InputStream is = t.getResourceAsStream(className);
        try {
            final ClassReader classReader = new ClassReader(is);
            classReader.accept(new ClassVisitor(Opcodes.ASM4) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM4) {
                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                            // The first parameter of a static method is the parameter of the method. If it is an instance method, the first parameter is this
                            final StringBuilder key = new StringBuilder(t.getName() + METHOD_SPLITER + name + METHOD_SPLITER);
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

    private void scanFile(final String path) {
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
                    final String className = files.getPath().substring(4).replace(BeanPool.FILE_SPLITER + BeanPool.FILE_POS_MARK, LOG.NO_CHAR).
                            replace(BeanPool.PATH_SPLITER_PATTERN, BeanPool.FILE_SPLITER);
                    if (!className.equals(MVC_CLASS)) {
                        result.add(executor.submit(new IOTask(className)));
                    }
                }
            }
        }
    }

    private void saveUrlPathMapping(final Class<?> tClass, final Method method, final Map<Method, Object> controllers) {
        try {
            final String name = Character.toLowerCase(tClass.getSimpleName().charAt(0)) + tClass.getSimpleName().substring(1);
            if (!objectMap.containsKey(name)) {
                Object bean = ClassUtils.getClass(tClass, beanPool, true);
                objectMap.put(name, bean);
            }
            controllers.put(method, objectMap.get(name));
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(LOG.LOG_PRE + "saveUrlPathMapping for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                    MVC_CLASS, tClass.getName(), LOG.EXCEPTION_DESC, e);
        }
    }

    private <T> void scanResolers(final Class<T> tClass) {
        //Allow the Annotations are common for the same controller class
        if (tClass.isAnnotationPresent(Controller.class) || tClass.isAnnotationPresent(WebSocket.class)) {
            final Controller controller = tClass.getAnnotation(Controller.class);
            final WebSocket webSocket = tClass.getAnnotation(WebSocket.class);
            final String parentPath1 = controller != null ? controller.value() : null;
            final String parentPath2 = webSocket != null ? webSocket.value() : null;
            for (Method method : tClass.getDeclaredMethods()) {
                Annotation annotation = method.getAnnotation(RequestMapping.class);
                if (annotation != null) {
                    final String subPath = ((RequestMapping) annotation).value();
                    if (subPath.matches(HTTPStatus.PATH_REGEX_MARK)) {
                        regexMappings.put(parentPath1 + subPath, method);
                    } else {
                        savePathRelation(subPath, parentPath1, method, mappings);
                    }
                    saveUrlPathMapping(tClass, method, controllers);
                }
                annotation = method.getAnnotation(WebSocketMethod.class);
                if (annotation != null) {
                    final String subPath = ((WebSocketMethod) annotation).value();
                    savePathRelation(subPath, parentPath2, method, wsMappings);
                    saveUrlPathMapping(tClass, method, wsControllers);
                }
            }
        }
        final Order order = tClass.getAnnotation(Order.class);
        if (order != null) {
            final Class<?>[] interfaces = tClass.getInterfaces();
            for (Class<?> i : interfaces) {
                if (i == Interceptor.class) {
                    try {
                        interceptors.add(new Pair<>(order.value(), (Interceptor) ClassUtils.getClass(tClass, beanPool, true)));
                    } catch (InstantiationException | IllegalAccessException e) {
                        logger.error(LOG.LOG_PRE + "scanResolers for class:" + LOG.LOG_PRE + LOG.LOG_POS,
                                MVC_CLASS, tClass.getName(), LOG.EXCEPTION_DESC, e);
                    }
                    break;
                }
            }
        }
    }

    private void savePathRelation(final String subPath, final String parentPath, final Method method, final Map<String, Method> mappings) {
        if (!StringUtils.isNotEmpty(subPath)) {
            mappings.put(parentPath + HTTPStatus.SLASH + method.getName(), method);
        } else {
            mappings.put(parentPath + subPath, method);
        }
    }

    private final class IOTask implements Callable<Boolean> {

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
        result = new LinkedList<>();
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
