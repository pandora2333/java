package pers.pandora.utils;

public class ClassUtil {

    public static boolean checkBasicClass(Class t) {
        return t == Integer.class || t == Character.class || t == Long.class || t == String.class ||
                t == int.class || t == boolean.class || t == byte.class || t == Double.class ||
                t == Float.class || t == short.class || t == Boolean.class || t == Byte.class ||
                t == char.class || t == long.class || t == double.class;
    }

    public static Class<?> getClass(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            //ignore
        }
        return null;
    }

    public static <T> T getInstance(Class<T> tClass) {
        try {
            return (T) tClass.newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            //ignore
        }
        return null;
    }
}
