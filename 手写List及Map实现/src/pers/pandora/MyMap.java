package pers.pandora;
/**
 * by pandora
 * 2018.11.15
 * version 1.1
 * encoding:UTF-8
 */
public interface MyMap<K,V>{
    public <K,V> void put(K k,V v);//放入元素
    public <K> V get(K k);//获取元素
    public <K> void delete(K k);//删除元素
    public int size();
}
