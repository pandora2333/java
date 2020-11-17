package per.pandora.serial.core;


import per.pandora.ObjectStrategyFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * json parser
 */
public class ObjectSerialFactory implements ObjectStrategyFactory {

    public static Logger log = Logger.getLogger(ObjectSerialFactory.class.getName());

    public static  volatile ObjectSerialFactory instance;

    public static ObjectSerialFactory getInstance(){
        if(instance == null){
            synchronized(log){
                if(instance == null){
                    instance = new ObjectSerialFactory();
                }
            }
        }
        return instance;
    }
    @Override
    public <T> T handle(T t) {
        return null;
    }
    public <T> String serialObject(T t){
        if(t == null){
            return "{null}";
        }
        StringBuffer res =  new StringBuffer();
        res.append('{');
        Class<?> tClass = t.getClass();
        List<Field> fields = new ArrayList<>();
        while(tClass != null && tClass != Object.class){
            for(Field field : tClass.getDeclaredFields()){
                fields.add(field);
            }
            tClass = tClass.getSuperclass();
        }
        for(Field field : fields){
            if(!field.isAccessible()){
                field.setAccessible(true);
            }
            Object value = null;
            try {
                value = field.get(t);
            } catch (IllegalAccessException e) {
                log.log(Level.SEVERE,"Object basic field value serial cause some errors: "+e.getMessage());
            }
            res.append(field.getName()).append(':');
            if(isBasicClass(field)){
                res.append(field.getType() == String.class ? (value != null ? "\"^\""+value+"\"$\"" : null) : value);
            }else{
                try {
                    Class.forName(field.getType().getName(),true,Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    log.log(Level.SEVERE,"Object class load and initial is failed: "+e.getMessage());
                }
                res.append(serialObject(value));
            }
            res.append(',');
        }
        res.append( '}');
        return res.toString();
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

    private int i;//global json string index offset
    public <T> T unSerialObject(String json,Class<T> tClass){
        i = 0;
        return unSerialObject0(json,tClass,null);
    }

    private <T> T unSerialObject0(String json, Class<?> tClass,T ret){
        if(tClass == null || json == null ||  json.equals("{null}") || json.length() <= 0) {
            return  null;
        }
        if(ret == null) {
            try {
                ret = (T) tClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                log.log(Level.SEVERE, "Object create instance cause some errors: "+e.getMessage());
                return ret;
            }
        }
        if(json.charAt(i) == '{'){
            if(i + 5 < json.length() && json.substring(i+1,i+5).equals("null")){
                i += 6;
                return null;
            }
            Map<String,Field> fields = new HashMap<>();
            Class<?> ptClass = tClass;
            while(ptClass != null && ptClass != Object.class){
                for(Field field : ptClass.getDeclaredFields()){
                    if(!fields.containsKey(field.getName())){
                        fields.put(field.getName(),field);
                    }
                }
                ptClass = ptClass.getSuperclass();
            }
            StringBuffer fvar = new StringBuffer();
            boolean str = false;//string flag on or switch off
            for(i++;i < json.length() && json.charAt(i) != '}';i++) {
                if(json.charAt(i) == '\"' && i + 2 < json.length() && json.charAt(i+1) == '^' && json.charAt(i+2)=='\"'){
                    log.log(Level.SEVERE,"Object variable's value string has invaild characters: \"^\"");
                    return null;
                }else if(json.charAt(i) == '\"' && i + 2 < json.length() && json.charAt(i+1) == '$' && json.charAt(i+2)=='\"'){
                    log.log(Level.SEVERE,"Object variable's value string has invaild characters: \"$\"");
                    return null;
                }else if(json.charAt(i) != ':' || str){
                    fvar.append(json.charAt(i));
                }else{
                    if(fields.containsKey(fvar.toString())){
                        Field field = fields.get(fvar.toString());
                        if(!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        StringBuffer value = null;
                        i++;
                        if(isBasicClass(field)) {
                            value = new StringBuffer();
                            while (i < json.length() && json.charAt(i) != ',') {
                                if(json.charAt(i) == '\"' && i + 2 < json.length() && json.charAt(i+1) == '^'  && json.charAt(i+2)=='\"'){
                                    if(str){
                                        log.log(Level.SEVERE,"Object variable's value string has invaild characters: \"^\"");
                                        return null;
                                    }
                                    str = true;
                                    i += 2;
                                }else if(json.charAt(i) == '\"' && i + 2 < json.length() && json.charAt(i+1) == '$' && json.charAt(i+2)=='\"'){
                                    if(!str){
                                        log.log(Level.SEVERE,"Object variable's value string has invaild characters: \"$\"");
                                        return null;
                                    }
                                    str = false;
                                    i += 2;
                                }
                                value.append(json.charAt(i++));
                            }
                            if (i == json.length()) {
                                log.log(Level.SEVERE, "Object variable's json value must have the ending of the ,");
                                return null;
                            }
                            if(value.toString().equals("null") || field.getType() == String.class){
                                try {
                                    if(value.toString().equals("null")){
                                        field.set(ret, value.toString());
                                    }else {
                                        field.set(ret, value.toString().substring(1,value.length()-1));
                                    }
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                            //long
                            else if(field.getType() == Long.class || field.getType() == long.class){
                                try {
                                    field.set(ret,Long.valueOf(value.toString()));
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                            //int
                            else if(field.getType() == Integer.class || field.getType() == int.class){
                                try {
                                    field.set(ret,Integer.valueOf(value.toString()));
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                            //short
                            else if(field.getType() == Short.class || field.getType() == short.class){
                                try {
                                    field.set(ret,Short.valueOf(value.toString()));
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                            //byte
                            else if(field.getType() == Byte.class || field.getType() == byte.class){
                                try {
                                    field.set(ret,Byte.valueOf(value.toString()));
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                            //char
                            else if(field.getType() == Character.class || field.getType() == char.class){
                                try {
                                    field.set(ret,Character.valueOf(value.charAt(0)));
                                    if(value.length() != 1){
                                        log.log(Level.SEVERE,"Object variable's character json value length must 1: "+value);
                                    }
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                            //boolean
                            else if(field.getType() == Boolean.class || field.getType() == boolean.class){
                                try {
                                    field.set(ret,Boolean.valueOf(value.toString()));
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                            //float
                            else if(field.getType() == Float.class || field.getType() == float.class){
                                try {
                                    field.set(ret,Float.valueOf(value.toString()));
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                            //Double
                            else if(field.getType() == Double.class || field.getType() == double.class){
                                try {
                                    field.set(ret,Double.valueOf(value.toString()));
                                } catch (IllegalAccessException e) {
                                    log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                                }
                            }
                        }else{
                            //reference object pointer
                            try {
                                ptClass = Class.forName(field.getType().getName(),true,Thread.currentThread().getContextClassLoader());
                            } catch (ClassNotFoundException e) {
                                log.log(Level.SEVERE,"Object class load and initial is failed: "+e.getMessage());
                            }
                            try {
                                field.set(ret, unSerialObject0(json,ptClass,null));
                            } catch (IllegalAccessException e) {
                                log.log(Level.SEVERE,"Object variable's json value type is error: "+e.getMessage());
                            }
                        }
                        fvar = new StringBuffer();
                    }else{
                        log.log(Level.SEVERE,"Object variable's name can't find it in Object and it's fathers: end index("+i+"),value:"+fvar);
                        return null;
                    }
                }
            }
        }else{
            log.log(Level.SEVERE,"Object instance json must have the beginning of the {");
            return null;
        }
        if(json.charAt(i) != '}'){
            log.log(Level.SEVERE,"Object instance json must have the ending of the }");
            return null;
        }
        i++;
        return ret;
    }

}
