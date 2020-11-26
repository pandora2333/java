package pers.pandora.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassUtils {

    public static <T> T getClass(String name, Map<String, List<Object>> params) {
        try {
            T t = (T) Class.forName(name).newInstance();
            if (params == null || params.size() == 0) {
                return t;
            }
            Map<String, Field> fieldMap = new HashMap<>();
            handleObjectField(t, fieldMap, true);
            for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
                if (params.containsKey(entry.getKey()) && params.get(entry.getKey()).size() == 1
                        && checkType(params.get(entry.getKey()).get(0).getClass(), entry.getValue().getType())) {
                    entry.getValue().set(t, params.get(entry.getKey()).get(0));
                }
            }
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T getClass(Class<T> tClass, Map<String, List<Object>> params) {
        try {
            T t = tClass.newInstance();
            if (params == null || params.size() == 0) {
                return t;
            }
            Map<String, Field> fieldMap = new HashMap<>();
            handleObjectField(t, fieldMap, true);
            for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
                if (params.containsKey(entry.getKey()) && params.get(entry.getKey()).size() == 1
                        && checkType(params.get(entry.getKey()).get(0).getClass(), entry.getValue().getType())) {
                    entry.getValue().set(t, params.get(entry.getKey()).get(0));
                }
            }
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean checkType(Class<?> aClass, Class<?> type) {
        return aClass == type || type == Object.class;
    }

    public static void initWithObjectList(Map<String, Object> objectList, Object handler) {
        if (objectList == null || handler == null) {
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
            String[] ss = name.split(JspParser.CLASS_NAME_SPLITER);
            if (ss.length != 2) {
                continue;
            }
            try {
                String query = ss[0].toLowerCase();
                if (valueMap.containsKey(query) && valueMap.get(query).containsKey(ss[1]) &&
                        checkType(valueMap.get(ss[0].toLowerCase()).get(ss[1]).getClass(), entry.getValue().getType())) {
                    entry.getValue().set(handler, valueMap.get(ss[0].toLowerCase()).get(ss[1]));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
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
                        e.printStackTrace();
                    }
                }
            }
            tClass = tClass.getSuperclass();
        }
    }
}
