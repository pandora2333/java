package pers.pandora.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import pers.pandora.annotation.Bean;
import pers.pandora.annotation.ConfigureScan;
import pers.pandora.annotation.PropertySource;
import pers.pandora.annotation.Value;
import pers.pandora.bean.TaskBean;

/**
 * by pandora
 * 2018.11.15
 * version 1.2
 * encoding:GBK
 */
//ʹ��Spring���Զ�ע��
public class BeanUtils {
	private static  String path;//ɨ�赱ǰ�������ļ�·��
	private static Properties prop = new Properties();//ά��һ����Դ�ļ���ȡ��
	private  static  Object singleton;//ȫ��ά��һ��singleton��ʵ��
	private static  Map<String ,Object>beans = new ConcurrentHashMap<>(16);
	static{
		scanFile("src/");
	}

	public static  <T> T getBean(String beanName,Class<T> clazz){
		if(beanName!=null&&!beanName.equals("")) {
			if (beans.get(beanName) != null && beans.get(beanName).getClass() == clazz) {
//			System.out.println("Object:"+beans.get(beanName));
				return (T) beans.get(beanName);
			}
		}
		return null;
	}

	private static void scanFile(String path){
		File files = new File(path);
//		System.out.println(files.isDirectory());
		if(files!=null) {
			if(files.isDirectory()) {
//				System.out.println("path:"+files.getAbsolutePath());
				for(File file:files.listFiles()){
					scanFile(file.getPath());
				}

			}else{
				if(files.getPath().endsWith(".java")){
					path = 	files.getPath();
					try {
					    //System.out.println("path:"+Class.forName(path.substring(4).replace(".java","").replace("\\",".")));
						scanBean(Class.forName(path.substring(4).replace(".java","").replace("\\","."))
								,null,Bean.class,null,null);
					} catch (ClassNotFoundException e) {
						System.out.println("����ת���쳣!");
					}
				}
			}
		}
	}

	private static  <T> void  scanBean(Class<T> t,Field field,Class template,Properties prop,String beanName){
			if(t.isAnnotationPresent(ConfigureScan.class)){
				for(Method method:t.getDeclaredMethods()){
//					System.out.println(method.getName());
					 Annotation annotation  = method.getAnnotation(Bean.class);
					   if(annotation!=null){
						   try {
							   Object obj =method.invoke(t.newInstance());
							   singleton = obj;
							   String nameTemp = ((Bean) annotation).value();
							   if(nameTemp.equals("")||nameTemp==null) {
								   nameTemp = method.getName();
							   }
							   injectValue(singleton.getClass(),nameTemp);
							   beans.put(nameTemp,obj);
						   } catch (Exception e) {
							   System.out.println("�޿չ�������");
//								System.out.println(e);
						   }
					   }
				   }
				}else if(template ==PropertySource.class){
				    for(Annotation annotation:t.getDeclaredAnnotations()) {
				    	if(annotation instanceof  PropertySource) {
				    		String filePath = ((PropertySource)annotation).value();
				    		if(filePath.equals("")){
				    			filePath = t.getSimpleName()+".properties";
							}
							loadProperties(t,filePath);//���������ļ�
						}
					}
			    }else if(template ==Value.class) {
				if (field != null) {
					try {
						Value fieldSrc = field.getAnnotation(Value.class);
						String fieldValue = fieldSrc.value();
						if (fieldValue.equals("")) {
							fieldValue = field.getName();
						}
						String key = "" + prop.get(fieldValue);
						if (field.getType() == int.class) {
							field.set(singleton, Integer.valueOf(key));
						} else if (field.getType() == long.class) {
							field.set(singleton, Long.valueOf(key));
						} else if (field.getType() == float.class) {
							field.set(singleton, Float.valueOf(key));
						} else if (field.getType() == double.class) {
							field.set(singleton, Double.valueOf(key));
						} else {

							field.set(singleton, key);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
	}

	//�Զ�ע������ֵ
	private static  <T> void injectValue(Class<T> clazz,String beanName){
          scanBean(clazz,null,PropertySource.class,null,beanName);
	}
	//������Դ�ļ�
	private static void loadProperties(Class clazz,String file){
		try {
			prop.load(new FileInputStream("src/"+file));
			for(Field field:clazz.getDeclaredFields()){
				field.setAccessible(true);
				if (field.isAnnotationPresent(Value.class)) {
					scanBean(clazz, field, Value.class, prop, null);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}


 	
