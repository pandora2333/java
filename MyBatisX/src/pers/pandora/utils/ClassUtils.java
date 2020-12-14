package pers.pandora.utils;

public class ClassUtils {

    public static boolean checkBasicClass(Class t) {
        return t == Integer.class || t == Character.class || t == Long.class || t == String.class ||
                t == int.class || t == boolean.class || t == byte.class || t == Double.class ||
                t == Float.class || t == short.class || t == Boolean.class || t == Byte.class ||
                t == char.class || t == long.class || t == double.class;
    }
}
