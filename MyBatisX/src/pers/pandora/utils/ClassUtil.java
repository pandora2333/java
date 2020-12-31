package pers.pandora.utils;

public final class ClassUtil {

    public static boolean checkBasicClass(final Class tClass) {
        return tClass == Integer.class || tClass == Character.class || tClass == Long.class || tClass == String.class ||
                tClass == int.class || tClass == boolean.class || tClass == byte.class || tClass == Double.class ||
                tClass == Float.class || tClass == short.class || tClass == Boolean.class || tClass == Byte.class ||
                tClass == char.class || tClass == long.class || tClass == double.class;
    }

    public static Class<?> getClass(final String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            //ignore
        }
        return null;
    }

    public static <T> T getInstance(final Class<T> tClass) {
        try {
            return (T) tClass.newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            //ignore
        }
        return null;
    }
}
