package pers.pandora.mvc;

import pers.pandora.annotation.*;
import pers.pandora.bean.Pair;
import pers.pandora.interceptor.Interceptor;
import pers.pandora.servlet.Request;
import pers.pandora.servlet.Response;
import pers.pandora.servlet.Servlet;

import java.io.File;

import java.lang.annotation.Annotation;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.处理url path与controller之间映射关系
 * 2.处理文本格式数据
 * 3.初始化所有实现Interceptor接口的拦截器
 * 4.赋值mvc方法参数
 */
public final class RequestMappingHandler implements Servlet {
    private static Map<String, String> mappings = new ConcurrentHashMap<>(16);//url - method
    private static Map<String, Object> controllers = new ConcurrentHashMap<>(16);//method - controller
    private static Map<String, ModelAndView> modelAndViews = new ConcurrentHashMap<>(16);//封装请求参数
    private static Map<String, Class> beans = new ConcurrentHashMap<>(16);//封装请求实体参数
    private static Set<Pair<Integer, Interceptor>> interceptors;
    private static final String rootPath = "src/";
    private static ThreadPoolExecutor executor;
    private static List<Future<Boolean>> result;

//    static {//类加载阶段启用多线程造成死锁：其它类引用到本类的信息，会等待本类加载完毕，但是本类自身加载过程中又等待其他类信息加载，造成死锁
//        result = new ArrayList<>();
//        interceptors = Collections.synchronizedSortedSet(new TreeSet<>((p1, p2) -> {
//            int t = p1.getK() - p2.getK();
//            return t != 0 ? t : System.identityHashCode(p1.getV().hashCode()) - System.identityHashCode(p2.getV());
//        }));
//        interceptors = new TreeSet<>((p1, p2) -> {
//            int t = p1.getK() - p2.getK();
//            return t != 0 ? t : System.identityHashCode(p1.getV().hashCode()) - System.identityHashCode(p2.getV());
//        });
//        int core = 2 * Runtime.getRuntime().availableProcessors();
//        executor = new ThreadPoolExecutor(core, core + 5, 50, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
//        scanFile(rootPath);
//        for (Future future : result) {
//            try {
//                future.get();
//            } catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//            }
//        }
//        executor.shutdown();
//        executor = null;
//        System.gc();
//    }

    public Set<Pair<Integer, Interceptor>> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(Set<Pair<Integer, Interceptor>> interceptors) {
        interceptors.addAll(interceptors);
    }

    public synchronized void setModelAndView(ModelAndView modelAndView) {
        if (modelAndView != null) {
            modelAndViews.put(modelAndView.getPage(), modelAndView);
        }
    }

    /**
     * 执行Controller方法，返回重定向地址
     *
     * @param url
     * @return
     * @throws Exception
     */
    public Pair<ModelAndView, List<Object>> parseUrl(String url) {
        List<Object> valueObject = new ArrayList<>();
        for (String urlMapping : mappings.keySet()) {
            if (urlMapping != null && urlMapping.equals(url)) {
                Class controller = controllers.get(mappings.get(urlMapping)).getClass();
                if (modelAndViews.containsKey(url)) {
                    ModelAndView modelAndView = modelAndViews.get(url);
                    Method method = null;
                    if (modelAndView.size() == 0) {
                        try {
                            method = controller.getDeclaredMethod(mappings.get(urlMapping));
                        } catch (Exception e) {
//							e.printStackTrace();
                        }
                    }
                    boolean isJson = false;
                    if (method != null && method.isAnnotationPresent(ResponseBody.class)) {
                        isJson = true;
                    }
                    if (modelAndView.size() == 0 && method != null) {
                        try {
                            return new Pair<>(new ModelAndView(String.valueOf(method
                                    .invoke(controllers.get(mappings.get(urlMapping)))), modelAndView.getParams(), isJson), valueObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {//处理方法参数
                        Class[] beanParam = null;//实体封装
                        Class<?>[] classParam = new Class<?>[modelAndView.size()];
                        Object[] objects = new Object[classParam.length];
                        AtomicInteger cursor = new AtomicInteger(0);
                        for (List clazzTemp : modelAndView.getParams().values()) {
                            if (clazzTemp != null && clazzTemp.size() > 0) {
                                classParam[cursor.get()] = clazzTemp.get(0).getClass();
                                objects[cursor.get()] = clazzTemp.get(0);
                                cursor.getAndIncrement();
                            }
                        }
                        try {//进一步封装参数
                            if (beans.get(mappings.get(urlMapping)) != null) {//只允许一个实体参数封装
                                beanParam = new Class[1];
                                beanParam[0] = beans.get(mappings.get(urlMapping));
                                method = controller.getDeclaredMethod(mappings.get(urlMapping), beanParam);
                            } else {
                                method = controller.getDeclaredMethod(mappings.get(urlMapping), classParam);
                            }
                            cursor.set(0);
                            if (method != null && method.getParameterAnnotations() != null) {
                                for (Annotation[] an : method.getParameterAnnotations()) {
                                    for (Annotation a : an) {
                                        if (a instanceof RequestParam) {
                                            String param = ((RequestParam) a).value();
                                            if (!param.equals("")) {
                                                objects[cursor.get()] = modelAndView.getParams().get(param).get(0);
                                            }
                                        } else if (a instanceof RequestBody) {
                                            //参数封装为bean
                                            Object bean = null;
                                            if (beanParam != null) {
                                                bean = beanParam[0].newInstance();
                                            } else {
                                                bean = classParam[cursor.get()].newInstance();
                                            }
                                            for (Map.Entry clazzTemp1 : modelAndView.getParams().entrySet()) {
                                                List clazzTemp2 = ((List) clazzTemp1.getValue());
                                                if (clazzTemp2 != null && clazzTemp2.size() > 0) {
                                                    Method fieldSet = bean.getClass().getDeclaredMethod("set" + (clazzTemp1.getKey() + "").
                                                            substring(0, 1).toUpperCase()
                                                            + (clazzTemp1.getKey() + "").substring(1), clazzTemp1.getKey().getClass());
                                                    fieldSet.invoke(bean, clazzTemp2.get(0));
                                                }
                                            }
                                            objects[cursor.get()] = bean;
                                            valueObject.add(bean);
                                        }
                                        if (beanParam != null) {
                                            break;//仅有一个参数，且为实体类封装,且不支持级联赋值
                                        }
                                        cursor.getAndIncrement();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            return null;
                        }

                        if (method != null && method.isAnnotationPresent(ResponseBody.class)) {
                            isJson = true;
                        }
                        try {
                            return new Pair<>((method != null) ? beanParam != null ? new ModelAndView(String.valueOf(
                                    method.invoke(controllers.get(mappings.get(urlMapping)), objects[0])),
                                    modelAndView.getParams(), isJson) : new ModelAndView(String.valueOf(
                                    method.invoke(controllers.get(mappings.get(urlMapping)), objects)),
                                    modelAndView.getParams(), isJson) : null, valueObject);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    //判断是否为基本数据类型或String类型或包装类型
    private static boolean isAtomic(Class t) {
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
                if (files.getPath().endsWith(".java")) {
                    result.add(executor.submit(new IOTask(files.getPath())));
//                    try {
//                        scanResolers(Class.forName(path.substring(4).replace(".java", "").replace("\\", "."),
//                                true, Thread.currentThread().getContextClassLoader()));
//                    } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
//                        e.printStackTrace();
//                    }
                }
            }
        }
    }

    private static <T> void scanResolers(Class<T> t) throws IllegalAccessException, InstantiationException {
        if (t.isAnnotationPresent(Controller.class)) {
            for (Method method : t.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    Annotation annotation = method.getAnnotation(RequestMapping.class);
                    if (((RequestMapping) annotation).value().equals("")) {
                        mappings.put(method.getName(), method.getName());
                    } else {
                        mappings.put(((RequestMapping) annotation).value(), method.getName());
                        for (Class<?> param : method.getParameterTypes()) {
                            if (!isAtomic(param)) {//只允许一个实体参数封装
                                beans.put(method.getName(), param);
                                break;
                            }
                        }
                    }
                    controllers.put(method.getName(), t.newInstance());
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

    //对文本格式数据处理
    @Override
    public void service() {

    }

    @Override
    public String doGet(Request request, Response response) {
        Map params = request.getParams();
        if (params != null && params.get(Response.PLAIN) instanceof List) {
            return (String) ((List) params.get(Response.PLAIN)).get(0);
        }
        return Response.NULL;
    }

    @Override
    public String doPost(Request request, Response response) {
        return null;
    }

    static class IOTask implements Callable<Boolean> {
        private String path;

        public IOTask(String path) {
            this.path = path;
        }

        @Override
        public Boolean call() {
            try {
                scanResolers(Class.forName(path.substring(4).replace(".java", "").replace("\\", "."),
                        true, Thread.currentThread().getContextClassLoader()));
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
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
        scanFile(rootPath);
        for (Future future : result) {
            try {
                future.get(500, TimeUnit.MILLISECONDS);
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
