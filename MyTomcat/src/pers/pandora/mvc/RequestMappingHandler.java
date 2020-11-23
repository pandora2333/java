package pers.pandora.mvc;

import pers.pandora.annotation.*;
import pers.pandora.servlet.Request;
import pers.pandora.servlet.Response;
import pers.pandora.servlet.Servlet;

import java.io.File;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.处理url path与controller之间映射关系
 * 2.处理文本格式数据
 */
public class RequestMappingHandler implements Servlet {
    private static Map<String,String> mappings =  new ConcurrentHashMap<>(16);//url - method
    private static Map<String,Object> controllers =  new ConcurrentHashMap<>(16);//method - controller
    private static Map<String,ModelAndView>modelAndViews = new ConcurrentHashMap<>(16);//封装请求参数
    private static  Map<String,Class> beans = new ConcurrentHashMap<>(16);//封装请求实体参数
    private static Stack<Object> valueStack = new Stack<>();//引入Struts的值栈，维护一个对象栈
    public  Stack<Object> getValueStack() {//对外使用值栈
        return valueStack;
    }

    static{
        scanFile("src/");
    }
    public synchronized void setModelAndView(ModelAndView modelAndView){
        if(modelAndView!=null) {
            modelAndViews.put(modelAndView.getPage(),modelAndView);
        }
    }

    /**
     * 执行Controller方法，返回重定向地址
     * @param url
     * @return
     * @throws Exception
     */
    public synchronized ModelAndView parseUrl(String url) {
        for (String urlMapping:mappings.keySet()) {
            if (urlMapping!=null&&urlMapping.equals(url)) {
                Class controller = controllers.get(mappings.get(urlMapping)).getClass();
                if(modelAndViews.containsKey(url)){
                    ModelAndView modelAndView = modelAndViews.get(url);
                    Method method = null;
                    if(modelAndView.size()==0) {
                    	try {
							method = controller.getDeclaredMethod(mappings.get(urlMapping));
						} catch (Exception e) {
							// TODO Auto-generated catch block
//							e.printStackTrace();
						} 
                    }
                    boolean isJson = false;
                    if(method!=null&&method.isAnnotationPresent(ResponseBody.class)){
                        isJson = true;
                    }
                    if(modelAndView.size()==0&&method!=null){
                        try {
							return  new ModelAndView(String.valueOf(method
							        .invoke(controllers.get(mappings.get(urlMapping)))),modelAndView.getParams(),isJson);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
                    }else {//处理方法参数
                        Class[] beanParam = null;//实体封装
                        Class<?>[] classParam = new Class<?>[modelAndView.size()];
                        Object[] objects = new Object[classParam.length];
                        AtomicInteger cursor = new AtomicInteger(0);
                        for (List clazzTemp:modelAndView.getParams().values()) {
                            if(clazzTemp!=null&&clazzTemp.size()>0) {
                                classParam[cursor.get()] = clazzTemp.get(0).getClass();
                                objects[cursor.get()] = clazzTemp.get(0);
                                cursor.getAndIncrement();
                            }
                        }
                        try {//进一步封装参数
                            if(beans.get(mappings.get(urlMapping))!=null){//只允许一个实体参数封装
                                beanParam = new Class[1];
                                beanParam[0] = beans.get(mappings.get(urlMapping));
                                method = controller.getDeclaredMethod(mappings.get(urlMapping),beanParam);
                            }else {
                                method = controller.getDeclaredMethod(mappings.get(urlMapping),classParam);
                            }
							cursor.set(0);
							if(method!=null&&method.getParameterAnnotations()!=null) {
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
                                            if(beanParam!=null){
                                                bean =  beanParam[0].newInstance();
                                            }else {
                                               bean =  classParam[cursor.get()].newInstance();
                                            }
                                            for (Map.Entry clazzTemp1 : modelAndView.getParams().entrySet()) {
                                                List clazzTemp2 = ((List) clazzTemp1.getValue());
                                                if (clazzTemp2 != null && clazzTemp2.size() > 0) {
                                                    Method fieldSet = bean.getClass().getDeclaredMethod("set" + (clazzTemp1.getKey()+"").substring(0,1).toUpperCase()
                                                            +(clazzTemp1.getKey()+"").substring(1),clazzTemp1.getKey().getClass());
                                                    fieldSet.invoke(bean,clazzTemp2.get(0));
                                                }
                                            }
                                            objects[cursor.get()] = bean;
                                            valueStack.push(bean);
                                        }
                                        if(beanParam!=null){
                                            break;//仅有一个参数，且为实体类封装,且不支持级联赋值
                                        }
                                        cursor.getAndIncrement();
                                    }
                                }
                            }
						} catch (Exception e) {
                            return null;
                        }

                        if(method!=null&&method.isAnnotationPresent(ResponseBody.class)){
                            isJson = true;
                        }
                        try {
							return (method!=null)?beanParam!=null?new ModelAndView(String.valueOf(
							        method.invoke(controllers.get(mappings.get(urlMapping)),objects[0])),
									modelAndView.getParams(),isJson):new ModelAndView(String.valueOf(
                                    method.invoke(controllers.get(mappings.get(urlMapping)),objects)),
                                    modelAndView.getParams(),isJson):null;
						} catch (Exception e) {
							// TODO Auto-generated catch block
							return null;
						} 
                    }
                }
            }
        }
        return null;
    }

    //判断是否为基本数据类型或String类型或包装类型
    private static boolean isAtomic(Class t){
        if(t == Integer.class||t==Character.class||t==Long.class||t==String.class||t==int.class
                ||t==boolean.class||t==byte.class||t==Double.class||t==Float.class||t==short.class||t==Boolean.class
        ||t==Byte.class||t==char.class||t==long.class||t==double.class) {
                return  true;
        }
        return false;
    }
    private static void scanFile(String path){
        File files = new File(path);
        if(files!=null) {
            if(files.isDirectory()) {
                for(File file:files.listFiles()){
                    scanFile(file.getPath());
                }

            }else{
                if(files.getPath().endsWith(".java")){
                    path = 	files.getPath();
                    try {
                        scanController(Class.forName(path.substring(4).replace(".java","").replace("\\",".")));
                    } catch (Exception e) {
                        System.out.println("类型转换异常!");
                    }
                }
            }
        }
    }

    private static  <T> void  scanController(Class<T> t) throws IllegalAccessException, InstantiationException {
        if(t.isAnnotationPresent(Controller.class)){
            for(Method method :t.getDeclaredMethods()){
                if(method.isAnnotationPresent(RequestMapping.class)){
                    Annotation annotation = method.getAnnotation(RequestMapping.class);
                    if(((RequestMapping) annotation).value().equals("")){
                        mappings.put(method.getName(),method.getName());
                    }else {
                        mappings.put(((RequestMapping) annotation).value(),method.getName());
                        for (Class<?> param:method.getParameterTypes()){
                            if(!isAtomic(param)){//只允许一个实体参数封装
                                beans.put(method.getName(),param);
                                break;
                            }
                        }
                    }
                    controllers.put(method.getName(),t.newInstance());
                }
            }
        }
    }

    //对文本格式数据处理
    @Override
    public void service() {

    }

    @Override
    public String doGet(Map params, Request request, Response response) {
        if(params!=null&&params.get(Response.PLAIN) instanceof  List){
           return (String)((List)params.get(Response.PLAIN)).get(0);
        }
        return Response.NULL;
    }

    @Override
    public String doPost(Map params,Request request, Response response) {
        return null;
    }
}
