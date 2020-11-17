package per.pandora.fullCopy.core;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * deeply copy pojo
 */
public class ObjectDeepCopyStrategyFactory extends  ObjectCopyStrategyFactory {

    public static Logger log = Logger.getLogger(ObjectCopyStrategyFactory.class.getName());

    public static  volatile  ObjectDeepCopyStrategyFactory instance;

    public static ObjectDeepCopyStrategyFactory getInstance(){
        if(instance == null){
            synchronized(log){
                if(instance == null){
                    instance = new ObjectDeepCopyStrategyFactory();
                }
            }
        }
        return instance;
    }

    private  <T> T copy0(Class<?> tClass,T t,T ret) {
        if(ret == null){
            try {
                ret = (T)tClass.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
                log.log(Level.SEVERE,"Object create instance cause some errors: "+e.getMessage());
                return ret;
            }
        }
        for(Field field : tClass.getDeclaredFields()){
            if(!field.isAccessible()) {
                field.setAccessible(true);
            }
            if(!isBasicClass(field)){
//                try {
//                    Thread.currentThread().getContextClassLoader().loadClass(field.getType().getName());
//                } catch (ClassNotFoundException e) {
//                    log.log(Level.SEVERE,e.getMessage());
//                }
                Class<?> fClass = null;
                try {
                    fClass = Class.forName(field.getType().getName(),true,Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    log.log(Level.SEVERE,"Object class load and initial is failed: "+e.getMessage());
                }
                if(fClass == null){
                    log.log(Level.WARNING,"Object copy result is null: "+field.getType().getName());
                    continue;
                }
                try {
                    field.set(ret,copy0(fClass,field.get(t),null));
                } catch (IllegalAccessException e) {
                    log.log(Level.SEVERE,"Object field class copy cause some errors: "+e.getMessage());
                }
            }else{
                try {
                    field.set(ret,field.get(t));
                } catch (IllegalAccessException e) {
                    log.log(Level.SEVERE,"Object basic field value copy cause some errors: "+e.getMessage());
                }
            }
        }
        Class<?> tpClass = tClass.getSuperclass();
        while(tpClass != null && tpClass != Object.class){
            copy0(tpClass,t,ret);
            tpClass = tpClass.getSuperclass();
        }
//        List<Class<?>> parents = new ArrayList<>();
//        parents.add(tClass.getSuperclass());
//        for(Class<?> interfaces : tClass.getInterfaces()) {
//            parents.add(interfaces);
//        }
//        for(Class<?> tpClass : parents){
//            while(tpClass != null && tpClass != Object.class){
//                copy0(tpClass,t,ret);
//                tpClass = tpClass.getSuperclass();
//            }
//        }
        return ret;
    }

    private boolean isBasicClass(Field field) {
        Class<?> type = field.getType();
        if(type == Long.class || type == Integer.class || type == Short.class || type == String.class
                || type == Byte.class || type == Float.class || type == Double.class || type == Character.class
                || type == Boolean.class) return  true;
        if(type == long.class || type == int.class || type == short.class || type == byte.class || type == float.class
                || type == double.class || type == char.class || type == boolean.class) return  true;
        return  false;
    }

    @Override
    public <T> T copy(T t) {
        return copy0(t.getClass(),t,null);
    }
}
