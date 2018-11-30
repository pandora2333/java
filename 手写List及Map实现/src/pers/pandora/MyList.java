package pers.pandora;

/**
 * by pandora
 * 2018.11.13
 * version 1.0
 * encoding:utf-8
 * @param <T>
 */
public interface MyList <T>{
    //放值
    public int add(T t);
    //取值
   public  T get(int index);
    //list容量
   public  int size();
   //删除
   public void delete(int index);
}
