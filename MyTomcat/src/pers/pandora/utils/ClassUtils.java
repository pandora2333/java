package pers.pandora.utils;

public final class ClassUtils {

	public static  <T> T getClass(String name,Class<T> clazz){
		try {
			T t = (T)Class.forName(name).newInstance();
			return t;
		} catch (Exception e) {
//			e.printStackTrace();
		}
		return null;
	}
}
