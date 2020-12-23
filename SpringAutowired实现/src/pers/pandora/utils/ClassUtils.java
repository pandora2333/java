package pers.pandora.utils;

import pers.pandora.constant.LOG;
import pers.pandora.core.BeanPool;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassUtils {

    private static final Map<String, Object> objectMap = new ConcurrentHashMap<>(16);

    private static final String MODIFIER = "modifiers";

    public static <T> T getClass(String name, BeanPool beanPool) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (T) getClass(Class.forName(name), beanPool);
    }

    public static <T> void initWithParams(T t, Map params) {
        if (t == null || !CollectionUtil.isNotEmptry(params)) {
            return;
        }
        Map<String, List<Field>> fieldMap = new HashMap<>();
        handleObjectField(t, fieldMap, true);
        String[] ss;
        String v;
        Object obj;
        Field objField;
        for (Map.Entry<String, List<Field>> entry : fieldMap.entrySet()) {
            if (params.containsKey(entry.getKey())) {
                injectValue(params.get(entry.getKey()), t, entry.getValue());
            } else {
                ss = entry.getKey().split(LOG.CLASS_NAME_SPLITER);
                v = getKeyBySeparator(ss);
                if (StringUtils.isNotEmpty(v)) {
                    obj = params.get(ss[0]);
                    if (obj != null) {
                        objField = getFieldByObject(obj.getClass(), v);
                        if (objField != null) {
                            try {
                                injectValue(objField.get(obj), t, entry.getValue());
                            } catch (IllegalAccessException e) {
                                //ignore
                            }
                        }
                    }
                }
            }
        }
    }

    private static String getKeyBySeparator(String[] ss) {
        if (ss.length < 2) {
            return null;
        }
        for (int i = 2; i < ss.length; i++) {
            ss[1] = ss[1] + LOG.CLASS_NAME_SPLITER + ss[i];
        }
        return ss[1];
    }

    private static void injectValue(Object obj, Object t, List<Field> fields) {
        if (obj == null || !CollectionUtil.isNotEmptry(fields)) {
            return;
        }
        for (Field field : fields) {
            try {
                if (obj instanceof List && ((List) obj).size() == 1 &&
                        checkType(((List) obj).get(0).getClass(), field.getType())) {
                    field.set(t, ((List) obj).get(0));
                    return;
                } else if (checkType(obj.getClass(), field.getType())) {
                    field.set(t, obj);
                    return;
                }
            } catch (Exception e) {
                //ignore
            }
        }
    }

    private static Field getFieldByObject(Class<?> objClass, String fieldName) {
        try {
            Field field = objClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            //ignore
            Class<?> superclass = objClass.getSuperclass();
            return superclass == null || superclass == Object.class ? null : getFieldByObject(superclass, fieldName);
        }
    }

    public static <T> T getClass(Class<T> tClass, BeanPool beanPool) throws IllegalAccessException, InstantiationException {
        if (tClass == null) {
            return null;
        }
        T bean = (T) objectMap.get(tClass.getName());
        if (bean != null) {
            return bean;
        }
        bean = beanPool != null ? beanPool.getBeanByType(tClass) : null;
        if (bean == null) {
            bean = tClass.newInstance();
        }
        objectMap.put(tClass.getName(), bean);
        return bean;
    }

    private static boolean checkType(Class<?> aClass, Class<?> type) {
        return aClass == type || type == Object.class;
    }

    public static void initWithObjectList(Object handler, Map<String, Object> objectList) {
        if (handler == null || !CollectionUtil.isNotEmptry(objectList)) {
            return;
        }
        Map<String, List<Field>> fieldMap = new HashMap<>();
        handleObjectField(handler, fieldMap, true);
        Map<String, Map<String, Object>> valueMap = new HashMap<>();
        String v;
        for (Map.Entry<String, Object> entry : objectList.entrySet()) {
            Map<String, Object> tmp = new HashMap<>();
            handleObjectField(entry.getValue(), tmp, false);
            valueMap.put(entry.getKey(), tmp);
        }
        for (Map.Entry<String, List<Field>> entry : fieldMap.entrySet()) {
            String name = entry.getKey();
            String[] ss = name.split(LOG.CLASS_NAME_SPLITER);
            v = getKeyBySeparator(ss);
            if (StringUtils.isNotEmpty(v)) {
                try {
                    for (Field field : entry.getValue()) {
                        String query = ss[0].toLowerCase();
                        if (valueMap.containsKey(query) && valueMap.get(query).containsKey(v) &&
                                valueMap.get(ss[0].toLowerCase()).get(v) != null
                                && checkType(valueMap.get(ss[0].toLowerCase()).get(v).getClass(), field.getType())) {
                            field.set(handler, valueMap.get(ss[0].toLowerCase()).get(v));
                            break;
                        }
                    }
                } catch (IllegalAccessException e) {
                    //ignore
                }
            }
        }
    }

    public static <T> void handleObjectField(T t, Map fieldMap, boolean isField) {
        Class<?> tClass = t.getClass();
        String fieldName;
        List<Field> fields;
        while (tClass != null && tClass != Object.class) {
            for (Field field : tClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (!fieldMap.containsKey(field.getName())) {
                    try {
                        if (!isField) {
                            fieldMap.put(field.getName(), field.get(t));
                        } else {
                            fieldName = field.getName();
                            fields = (List<Field>) fieldMap.get(fieldName);
                            if (!CollectionUtil.isNotEmptry(fields)) {
                                fields = new ArrayList<>(1);
                                fieldMap.put(fieldName, fields);
                            }
                            fields.add(field);
                        }
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
        return t == Integer.class || t == Character.class || t == Long.class || t == String.class ||
                t == int.class || t == boolean.class || t == byte.class || t == Double.class ||
                t == Float.class || t == short.class || t == Boolean.class || t == Byte.class ||
                t == char.class || t == long.class || t == double.class;
    }

    public static <T> T copy(Class<?> tClass, T t, T ret) {
        if (t == null) {
            return null;
        }
        if (ret == null) {
            try {
                ret = (T) tClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                //ignore
                return ret;
            }
        }
        for (Field field : tClass.getDeclaredFields()) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            if ((field.getModifiers() & Modifier.FINAL) != 0) {
                try {
                    Field modifiers = Field.class.getDeclaredField(MODIFIER);
                    modifiers.setAccessible(true);
                    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    //ignore
                }
            }
            try {
                field.set(ret, field.get(t));
            } catch (IllegalAccessException e) {
                //ignore
            }
        }
        Class<?> tpClass = tClass.getSuperclass();
        while (tpClass != null && tpClass != Object.class) {
            copy(tpClass, t, ret);
            tpClass = tpClass.getSuperclass();
        }
        return ret;
    }
}
