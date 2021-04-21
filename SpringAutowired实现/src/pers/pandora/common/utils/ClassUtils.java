package pers.pandora.common.utils;

import pers.pandora.common.constant.LOG;
import pers.pandora.om.core.BeanPool;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassUtils {

    private static final Map<String, Object> objectMap = new ConcurrentHashMap<>(16);

    private static final String MODIFIER = "modifiers";

    private static final String VALUEOF = "valueOf";

    public static <T> T getClass(final String name, final BeanPool beanPool, final boolean cache) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (T) getClass(Class.forName(name), beanPool, cache);
    }

    public static <T> void initWithParams(final T t, final Map params) {
        if (t == null || !CollectionUtil.isNotEmptry(params)) {
            return;
        }
        final Map<String, List<Field>> fieldMap = new HashMap<>(16);
        handleObjectField(t, fieldMap, true);
        fieldMap.forEach((k, v) -> {
            if (params.containsKey(k)) {
                injectValue(params.get(k), t, v);
            } else {
                final String[] ss = k.split(LOG.CLASS_NAME_SPLITER);
                final String value = getKeyBySeparator(ss);
                if (StringUtils.isNotEmpty(value)) {
                    final Object obj = params.get(ss[0]);
                    if (obj != null) {
                        final Field objField = getFieldByObject(obj.getClass(), value);
                        if (objField != null) {
                            try {
                                injectValue(objField.get(obj), t, v);
                            } catch (IllegalAccessException e) {
                                //ignore
                            }
                        }
                    }
                }
            }
        });
        for (Map.Entry<String, List<Field>> entry : fieldMap.entrySet()) {

        }
    }

    public static String getKeyBySeparator(final String[] ss) {
        if (ss.length < 2) {
            return null;
        }
        for (int i = 2; i < ss.length; i++) {
            ss[1] = ss[1] + LOG.CLASS_NAME_SPLITER + ss[i];
        }
        return ss[1];
    }

    public static void injectValue(final Object obj, final Object t, final List<Field> fields) {
        if (obj == null || !CollectionUtil.isNotEmptry(fields)) {
            return;
        }
        fields.forEach(field -> {
            try {
                if (obj instanceof List) {
                    if (((List) obj).size() == 1 && checkType(((List) obj).get(0).getClass(), field.getType())) {
                        field.set(t, ((List) obj).get(0));
                        return;
                    }
                    if (checkBasicClass(field.getType()) && ((List) obj).get(0).getClass() == String.class) {
                        field.set(t, getBascialObjectByString((String) ((List) obj).get(0), field.getType()));
                        return;
                    }
                }
                if (checkType(obj.getClass(), field.getType())) {
                    field.set(t, obj);
                    return;
                }
            } catch (Exception e) {
                //ignore
            }
        });
    }

    public static List<Object> convertBasicObject(final List list, final Class<?> basicClass) {
        final List<Object> tmpList = new LinkedList<>();
        try {
            final Method method = basicClass.getMethod(VALUEOF, String.class);
            list.forEach(obj -> {
                try {
                    tmpList.add(method.invoke(null, (String) obj));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    //ignore
                }
            });
        } catch (NoSuchMethodException e) {
            //ignore
        }
        return tmpList;
    }

    public static Field getFieldByObject(final Class<?> objClass, final String fieldName) {
        try {
            final Field field = objClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            //ignore
            final Class<?> superclass = objClass.getSuperclass();
            return superclass == null || superclass == Object.class ? null : getFieldByObject(superclass, fieldName);
        }
    }

    public static <T> T getClass(final Class<T> tClass, final BeanPool beanPool, boolean cache) throws IllegalAccessException, InstantiationException {
        if (tClass == null) {
            return null;
        }
        T bean;
        if (cache) {
            bean = (T) objectMap.get(tClass.getName());
            if (bean != null) {
                return bean;
            }
        }
        bean = beanPool != null ? beanPool.getBeanByType(tClass) : null;
        if (bean == null) {
            bean = tClass.newInstance();
        }
        if (cache) {
            objectMap.put(tClass.getName(), bean);
        }
        return bean;
    }

    public static boolean checkType(final Class<?> tClass, final Class<?> type) {
        return tClass == type || type == Object.class;
    }

    public static void initWithObjectList(final Object handler, final Map<String, Object> objectList) {
        if (handler == null || !CollectionUtil.isNotEmptry(objectList)) {
            return;
        }
        final Map<String, List<Field>> fieldMap = new HashMap<>(16);
        handleObjectField(handler, fieldMap, true);
        final Map<String, Map<String, Object>> valueMap = new HashMap<>(16);
        objectList.forEach((k, v) -> {
            final Map<String, Object> tmp = new HashMap<>(16);
            handleObjectField(v, tmp, false);
            valueMap.put(k, tmp);
        });
        fieldMap.forEach((k, v) -> {
            final String[] ss = k.split(LOG.CLASS_NAME_SPLITER);
            final String query = ss[0].toLowerCase();
            final String value = getKeyBySeparator(ss);
            if (StringUtils.isNotEmpty(value)) {
                v.stream().anyMatch(field -> {
                    if (valueMap.containsKey(query) && valueMap.get(query).containsKey(value) &&
                            valueMap.get(ss[0].toLowerCase()).get(value) != null
                            && checkType(valueMap.get(ss[0].toLowerCase()).get(value).getClass(), field.getType())) {
                        try {
                            field.set(handler, valueMap.get(ss[0].toLowerCase()).get(value));
                        } catch (IllegalAccessException e) {
                            //ignore
                            return false;
                        }
                        return true;
                    }
                    return false;
                });
            }
        });
    }

    public static Object getBascialObjectByString(final String orign, final Class<?> basicClass) {
        Method method = null;
        try {
            method = basicClass.getMethod(VALUEOF, String.class);
        } catch (NoSuchMethodException e) {
            //ignore
        }
        try {
            return method.invoke(null, orign);
        } catch (IllegalAccessException | InvocationTargetException e) {
            //ignore
        }
        return null;
    }

    public static <T> void handleObjectField(final T t, final Map fieldMap, final boolean isField) {
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
                                fields = new LinkedList<>();
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
    public static boolean checkBasicClass(final Class tClass) {
        return tClass == Integer.class || tClass == Character.class || tClass == Long.class || tClass == String.class ||
                tClass == int.class || tClass == boolean.class || tClass == byte.class || tClass == Double.class ||
                tClass == Float.class || tClass == short.class || tClass == Boolean.class || tClass == Byte.class ||
                tClass == char.class || tClass == long.class || tClass == double.class;
    }

    public static <T> T copy(final Class<?> tClass, T t, T ret) {
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
        Field modifiers;
        for (Field field : tClass.getDeclaredFields()) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            if ((field.getModifiers() & Modifier.FINAL) != 0) {
                try {
                    modifiers = Field.class.getDeclaredField(MODIFIER);
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
