package pers.pandora.core;

import org.dom4j.Document;
import org.dom4j.Element;
import pers.pandora.annotation.Column;
import pers.pandora.annotation.Id;
import pers.pandora.annotation.Table;
import pers.pandora.core.utils.Dom4JUtil;


import java.io.File;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//Mapper注解注入类
public class Configuration {
	private static   Map<String ,Object>beans = new ConcurrentHashMap<>(16);
	private static Map<String,String> alias = new ConcurrentHashMap<>(16);//实体类及属性对应表与字段别名关联
	private static Map<String,Class> poClassTableMap =new ConcurrentHashMap<>();//实体类与数据表关联
	private static String table;//table键名
	public  static Map<String, String> getAlias() {
		return alias;
	}

	public static Map<String, Class> getPoClassTableMap() {
		return poClassTableMap;
	}

	static {
		//初始化当前工作的目录
		scanFile("src/");
	}
	//返回代理mapper
	public static synchronized  <T> T createMapperProxy(String file,Class<T> template) throws Exception {
		Document doc = Dom4JUtil.getDocument("src/"+file);
		String proxyClass= doc.getRootElement().attributeValue("namespace");
		/**
		 * CRUD集合
		 */
		List<Element> select = doc.getRootElement().elements("select");
		List<Element> insert = doc.getRootElement().elements("insert");
		List<Element> update = doc.getRootElement().elements("update");
		List<Element> delete = doc.getRootElement().elements("delete");
		List<DynamicSql> proxys = new LinkedList<>();
		/**
		 * CRUD的对应sql放入
		 */
		for(Element ele:select) {
			String proxyMethod = ele.attributeValue("id");
			String resultType = ele.attributeValue("resultType");
			String sql = ele.getTextTrim();
			DynamicSql dynamicSql = null;
			if(sql!=null&&!sql.equals("")) {
				dynamicSql = new DynamicSql("select", proxyMethod, resultType, sql);
			}
			if(dynamicSql!=null) {
				proxys.add(dynamicSql);
			}
		}
		for(Element ele:insert) {
			String proxyMethod = ele.attributeValue("id");
			String sql = ele.getTextTrim();
			DynamicSql dynamicSql = null;
			if(sql!=null&&!sql.equals("")) {
				dynamicSql = new DynamicSql("insert", proxyMethod,null, sql);
			}
			if(dynamicSql!=null) {
				proxys.add(dynamicSql);
			}
		}
		for(Element ele:delete) {
			String proxyMethod = ele.attributeValue("id");
			String sql = ele.getTextTrim();
			DynamicSql dynamicSql = null;
			if(sql!=null&&!sql.equals("")) {
				dynamicSql = new DynamicSql("delete", proxyMethod,null, sql);
			}
			if(dynamicSql!=null) {
				proxys.add(dynamicSql);
			}
		}
		for(Element ele:update) {
			String proxyMethod = ele.attributeValue("id");
			String sql = ele.getTextTrim();
			DynamicSql dynamicSql = null;
			if(sql!=null&&!sql.equals("")) {
				dynamicSql = new DynamicSql("update", proxyMethod,null, sql);
			}
			if(dynamicSql!=null) {
				proxys.add(dynamicSql);
			}
		}
		/**
		 * Mapper代理生成
		 */
		try {
			List<DynamicSql> makeMethod = new LinkedList<>();
			Class clazz = Class.forName(proxyClass);
			if(clazz.isInterface()){
				for(Method method:clazz.getDeclaredMethods()){
					for(DynamicSql dynamicSql:proxys){
						if(dynamicSql.getId().equals(method.getName())){
							makeMethod.add(dynamicSql);
						}
					}
				}
				return (T) MapperProxyClass.parseMethod(makeMethod,template);
			}else{
				System.out.println("mapper配置文件异常!");
			}
		} catch (ClassNotFoundException e) {
			System.out.println(e);
		}
		return null;
	}
	//返回实体
	public static <T> T getBean(String beanName,Class<T> clazz){
		if(beanName!=null&&!beanName.equals("")) {
			if (beans.get(beanName) != null) {
				return (T) beans.get(beanName);
			}
		}
		return null;
	}
	//路径下文件扫描
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
						scanBean(Class.forName(path.substring(4).replace(".java","").replace("\\","."))
								,null, Table.class);
					} catch (Exception e) {
						System.out.println("缺少空构造器!");
					}
				}
			}
		}
	}

	//实体扫描
	private static  <T> void  scanBean(Class<T> t,Field field,Class template) throws Exception {
		  if(template ==Table.class){
				    for(Annotation annotation:t.getDeclaredAnnotations()) {
				    	if(annotation instanceof Table) {
				    		  table = ((Table)annotation).value();
				    		if(table.equals("")){
				    			table = t.getSimpleName().substring(0,1).toLowerCase()+t.getSimpleName().substring(1);
							}
							beans.put(table,t.newInstance());
				    		poClassTableMap.put(table,t);
							scanField(t);
					}
					}
			    }else if(template ==Id.class||template == Column.class) {
				if (field != null) {
					try {
						String fieldValue = "";
						if(template ==Id.class) {
							Id id = field.getAnnotation(Id.class);
							if (id != null) {
								fieldValue = id.value();
								if (fieldValue .equals("")) {
									fieldValue = field.getName();
								}
							}
						}else {
							Column column = field.getAnnotation(Column.class);
							if (column != null) {
								fieldValue = column.value();
								if (fieldValue.equals("")) {
									fieldValue = field.getName();
								}
							}
						}
						alias.put(fieldValue,field.getName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
	}
	//实体类字段扫描
	private static void scanField(Class clazz){
		try {
			for(Field field:clazz.getDeclaredFields()){
				field.setAccessible(true);
				if(field.isAnnotationPresent(Id.class)){
					scanBean(clazz, field, Id.class);
				}else if(field.isAnnotationPresent(Column.class)){
					scanBean(clazz, field, Column.class);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
