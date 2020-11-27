package pers.pandora.mvc;

import javassist.Modifier;
import jdk.internal.org.objectweb.asm.*;
import pers.pandora.annotation.*;
import pers.pandora.bean.Pair;
import pers.pandora.interceptor.Interceptor;
import pers.pandora.servlet.Dispatcher;
import pers.pandora.servlet.Request;
import pers.pandora.servlet.Response;
import pers.pandora.utils.ClassUtils;

import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * 1.处理url path与controller之间映射关系
 * 2.初始化所有实现Interceptor接口的拦截器
 * 3.赋值mvc方法参数
 */
public final class RequestMappingHandler {
    private static Map<String, Method> mappings = new ConcurrentHashMap<>(16);//url - method
    private static Map<Method, Object> controllers = new ConcurrentHashMap<>(16);//method - controller(singleton instance)
    private static Map<Method, List<String>> parameters = new ConcurrentHashMap<>(16);//method - parameter list
    private static Set<Pair<Integer, Interceptor>> interceptors;
    private static final String ROOTPATH = "src/";
    private static final String METHOD_SPLITER = "|";
    private static final char FILE_SPLITER = '.';
    private static final String FILE_POS_MARK = "java";
    private static final String CLASS_FILE_POS = "class";
    private static ThreadPoolExecutor executor;
    private static List<Future<Boolean>> result;
    private long timeout;

//    static {//类加载阶段启用多线程造成死锁：其它类引用到本类的信息，会等待本类加载完毕，但是本类自身加载过程中又等待其他类信息加载，造成死锁
//         init();
//    }


    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    public Set<Pair<Integer, Interceptor>> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(Set<Pair<Integer, Interceptor>> interceptors) {
        interceptors.addAll(interceptors);
    }


    /**
     * 执行Controller方法，返回请求转发地址
     *
     * @param modelAndView
     * @return
     * @throws Exception
     */
    public void parseUrl(ModelAndView modelAndView) {
        Map<String, Object> valueObject = new HashMap<>();
        Method method = mappings.get(modelAndView.getRequest().getReqUrl());
        Object controller = controllers.get(method);
        if (controller == null) {
            return;//生成实例失败
        }
        if (method != null && method.isAnnotationPresent(ResponseBody.class)) {
            modelAndView.setJson(true);
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object objects[] = new Object[parameterTypes.length];
        String paramNames[] = new String[parameterTypes.length];
        Map<String, List<Object>> params = modelAndView.getRequest().getParams();
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof RequestParam) {
                    paramNames[i] = ((RequestParam) annotation).value();
                }
            }
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == Request.class) {
                objects[i] = modelAndView.getRequest();
            } else if (parameterTypes[i] == Response.class) {
                objects[i] = modelAndView.getResponse();
            } else if (parameterTypes[i] == ModelAndView.class) {
                objects[i] = modelAndView;
            } else if (!checkBasicClass(parameterTypes[i])) {
                //不允许参数简单类名重复，即使全类名不一致
                Object target = ClassUtils.getClass(parameterTypes[i], params);
                valueObject.put(parameterTypes[i].getSimpleName().toLowerCase(), target);
                objects[i] = target;
            } else if (paramNames[i] != null) {
                List<Object> list = params.get(paramNames[i]);
                objects[i] = list != null && list.size() == 1 ? list.get(0) : null;
            } else {
                throw new RuntimeException("基本类型变量缺少注解:" + method.getName());
            }
        }
        try {
            Object result = method.invoke(controller, objects);
            if (modelAndView.isJson()) {
                List<Object> temp = new LinkedList<>();
                temp.add(result);
                modelAndView.getRequest().getParams().put(Response.PLAIN, temp);
                modelAndView.setPage(Response.PLAIN);
                modelAndView.getResponse().setServlet(Dispatcher.getMvcClass());
            } else {
                modelAndView.setPage(result.toString());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("方法变量值与类型不匹配：" + method.getName());
        }
        modelAndView.getRequest().setObjectList(valueObject);
    }

    //利用ASM操作class字节码文件获取参数名
    @Deprecated
    public void handleMethodParamNames(Class<?> t) {
        Map<String, Integer> modifers = new HashMap<>();
        Map<String, String[]> paramNames = new HashMap<>();
        for (Method method : t.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                StringBuilder key = new StringBuilder(t.getName());
                key.append(METHOD_SPLITER);
                key.append(method.getName());
                key.append(METHOD_SPLITER);
                for (Class<?> param : method.getParameterTypes()) {
                    key.append(param.getName());
                    key.append(FILE_SPLITER);
                }
                modifers.put(key.toString(), method.getModifiers());
                paramNames.put(key.toString(), new String[method.getParameterTypes().length]);
            }
        }
        String className = t.getName();
        int lastDotIndex = className.lastIndexOf(FILE_SPLITER);
        className = className.substring(lastDotIndex + 1) + FILE_SPLITER + CLASS_FILE_POS;
        InputStream is = t.getResourceAsStream(className);
        try {
            ClassReader classReader = new ClassReader(is);
            classReader.accept(new ClassVisitor(Opcodes.ASM4) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM4) {
                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                            // 静态方法第一个参数就是方法的参数，如果是实例方法，第一个参数是this
                            StringBuilder key = new StringBuilder(t.getName() + METHOD_SPLITER + name + METHOD_SPLITER);
                            for (Type type : Type.getArgumentTypes(desc)) {
                                key.append(type.getClassName());
                                key.append(FILE_SPLITER);
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
            e.printStackTrace();
        }
    }

    //判断是否为基本数据类型或String类型或包装类型
    private static boolean checkBasicClass(Class t) {
        if (t == Integer.class || t == Character.class || t == Long.class || t == String.class || t == int.class
                || t == boolean.class || t == byte.class || t == Double.class || t == Float.class || t == short.class || t == Boolean.class
                || t == Byte.class || t == char.class || t == long.class || t == double.class) {
            return true;
        }
        return false;
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
                    result.add(executor.submit(new IOTask(files.getPath())));
                }
            }
        }
    }

    private static <T> void scanResolers(Class<T> t) {
        if (t.isAnnotationPresent(Controller.class)) {
            for (Method method : t.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    Annotation annotation = method.getAnnotation(RequestMapping.class);
                    if (((RequestMapping) annotation).value().equals("")) {
                        mappings.put(method.getName(), method);
                    } else {
                        mappings.put(((RequestMapping) annotation).value(), method);
                    }
                    try {
                        controllers.put(method, t.newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (t.isAnnotationPresent(Order.class)) {
            Class<?>[] interfaces = t.getInterfaces();
            for (Class<?> i : interfaces) {
                if (i == Interceptor.class) {
                    try {
                        interceptors.add(new Pair<>(t.getAnnotation(Order.class).value(), (Interceptor) t.newInstance()));
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }


    static class IOTask implements Callable<Boolean> {
        private String path;

        public IOTask(String path) {
            this.path = path;
        }

        @Override
        public Boolean call() {
            try {
                scanResolers(Class.forName(path.substring(4).replace(FILE_SPLITER + FILE_POS_MARK, "").replace("\\", "."),
                        true, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    public void init() {
        result = new ArrayList<>();
        interceptors = Collections.synchronizedSortedSet(new TreeSet<>((p1, p2) -> {
            int t = p1.getK() - p2.getK();
            return t != 0 ? t : System.identityHashCode(p1.getV().hashCode()) - System.identityHashCode(p2.getV());
        }));
        int core = 2 * Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(core, core + 5, 50, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        scanFile(ROOTPATH);
        for (Future future : result) {
            try {
                future.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        executor = null;
        result.clear();
        result = null;
        System.gc();
    }

    public static void main(String[] args) {
        new RequestMappingHandler().init();
        System.out.println(interceptors.size());
        System.out.println("load over!");
    }
}
