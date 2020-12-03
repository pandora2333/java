package pers.pandora.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassUtils {

    public static <T> T getClass(String name) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (T) getClass(Class.forName(name));
    }

    public static <T> void initWithParams(T t, Map params) {
        if (t == null || !CollectionUtil.isNotEmptry(params)) {
            return;
        }
        Map<String, Field> fieldMap = new HashMap<>();
        handleObjectField(t, fieldMap, true);
        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            if (params.containsKey(entry.getKey())) {
                Object obj = params.get(entry.getKey());
                try {
                    if (obj instanceof List && ((List) obj).size() == 1 &&
                            checkType(((List) obj).get(0).getClass(), entry.getValue().getType())) {
                        entry.getValue().set(t, ((List) obj).get(0));
                    } else if (checkType(obj.getClass(), entry.getValue().getType())) {
                        entry.getValue().set(t, obj);
                    }
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    public static <T> T getClass(Class<T> tClass) throws IllegalAccessException, InstantiationException {
        return tClass.newInstance();
    }

    private static boolean checkType(Class<?> aClass, Class<?> type) {
        return aClass == type || type == Object.class;
    }

    public static void initWithObjectList(Object handler, Map<String, Object> objectList) {
        if (handler == null || !CollectionUtil.isNotEmptry(objectList)) {
            return;
        }
        Map<String, Field> fieldMap = new HashMap<>();
        handleObjectField(handler, fieldMap, true);
        Map<String, Map<String, Object>> valueMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : objectList.entrySet()) {
            Map<String, Object> tmp = new HashMap<>();
            handleObjectField(entry.getValue(), tmp, false);
            valueMap.put(entry.getKey(), tmp);
        }
        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            String name = entry.getKey();
            String[] ss = name.split(JspParser.CLASS_NAME_SPLITER, -1);
            if (ss.length != 2) {
                continue;
            }
            try {
                String query = ss[0].toLowerCase();
                if (valueMap.containsKey(query) && valueMap.get(query).containsKey(ss[1]) && valueMap.get(ss[0].toLowerCase()).get(ss[1]) != null
                        && checkType(valueMap.get(ss[0].toLowerCase()).get(ss[1]).getClass(), entry.getValue().getType())) {
                    entry.getValue().set(handler, valueMap.get(ss[0].toLowerCase()).get(ss[1]));
                }
            } catch (IllegalAccessException e) {
                //ignore
            }
        }

    }

    public static <T> void handleObjectField(T t, Map fieldMap, boolean isField) {
        Class<?> tClass = t.getClass();
        while (tClass != null && tClass != Object.class) {
            for (Field field : tClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (!fieldMap.containsKey(field.getName())) {
                    try {
                        fieldMap.put(field.getName(), isField ? field : field.get(t));
                    } catch (IllegalAccessException e) {
                        //ignore
                    }
                }
            }
            tClass = tClass.getSuperclass();
        }
    }

    //It determines whether it is a basic data type or a string type or a wrapper type
    public static boolean checkBasicClass(Class t) {
        if (t == Integer.class || t == Character.class || t == Long.class || t == String.class ||
                t == int.class || t == boolean.class || t == byte.class || t == Double.class ||
                t == Float.class || t == short.class || t == Boolean.class || t == Byte.class ||
                t == char.class || t == long.class || t == double.class) {
            return true;
        }
        return false;
    }
}
