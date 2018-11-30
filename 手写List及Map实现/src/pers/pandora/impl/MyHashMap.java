package pers.pandora.impl;

import pers.pandora.MyMap;
public class MyHashMap<K,V> implements MyMap<K,V> {
    private int size;//当前集合容量
    private int length;//定义集合容量
    private float loader;//加载因素
    private Entry<K,V>[] entry;
    public MyHashMap(int capacity,float loader){
        if (length < 0) {
            throw new IllegalArgumentException("参数不能为负数" + length);
        }
        if (capacity <= 0 || Double.isNaN(loader)) {
            throw new IllegalArgumentException("扩容标准必须是大于0的数字" + loader);
        }
        length = capacity;
        this.loader = loader;
        entry = new Entry[length];

    }
    public MyHashMap(){
         this(16,0.75f);
    }

    @Override
    public <K,V> void put(K k, V v) {
        if(size >= loader*length){
            rehash();
        }
        if(v !=null&& k!=null) {
            Entry temp = entry[k.hashCode() % length];
            if (temp == null) {
                entry[k.hashCode() % length] = new Entry(k,v);
                entry[k.hashCode() % length].setNext(null);
            } else {
                insertScan(k,v,temp);
            }
            size++;
        }
    }
    private <K,V> void insertScan(K k,V v,Entry<K,V> temp){
        Entry<K,V> cursor =temp;
        while(cursor.getNext()!=null){
            if(cursor.getK().equals(k)){
                cursor.setV(v);
                return;
            }
            cursor = cursor.getNext();
        }
        //insert
        cursor = new Entry(k,v);
        cursor.setNext(null);
        temp.setNext(cursor);

    }
    private  void rehash(){
        Entry<K,V>[] tempEntry = new Entry[length<<1];
        for (Entry<K,V> temp1 :entry){
            Entry<K,V> cursor = temp1;
            while(cursor!=null){
                Entry<K,V> temp2 =  tempEntry[cursor.getK().hashCode()%tempEntry.length];
                if(temp2 ==null){
                    tempEntry[cursor.getK().hashCode()%tempEntry.length] = new Entry<>(cursor.getK(),cursor.getV());
                    tempEntry[cursor.getK().hashCode()%tempEntry.length].setNext(null);
                }else {
                    insertScan(cursor.getK(),cursor.getV(),temp2);
                }
                cursor = cursor.getNext();
            }
        }
        entry = tempEntry;
        length = entry.length;
    }
    private  Entry<K,V> getEntry(Object obj){
        K k=(K)obj;
        if(k!=null){
            Entry<K,V> query =entry[k.hashCode()%length];
            while(query!=null){
                if(query.getK().equals(k)){
                    return query;
                }
                query = query.getNext();
            }
        }
        return null;
    }
    @Override
    public <K> V get(K k) {
        return getEntry(k).getV();
    }

    @Override
    public <K> void delete(K k) {
         if(k!=null){
             Entry query = entry[k.hashCode()%length];
             Entry temp = query;
             while(query!=null){
                if(query.getK().equals(k)){
                    if(temp == query){
                        entry[k.hashCode()%length] = query.getNext();
                    }else {
                        temp.setNext(query.getNext());
                    }
                    size--;
                    return;
                }
                temp = query;
                query = query.getNext();
             }
         }
    }

    @Override
    public String toString() {
        StringBuilder sb  = new StringBuilder("{");
        for (Entry<K,V> tempEntry :entry){
                while(tempEntry!=null) {
                    sb.append(tempEntry + ",");
                    tempEntry = tempEntry.getNext();
                }
        }
        String str = sb.toString().substring(0,sb.length()-1);
        str.intern();//重用字符串
        return str.equals("")?"":str+"}";
    }

    @Override
    public int size() {
        return size;
    }
    //内部类存储K,V值对
    static class Entry<K,V>{
        private K k;
        private V v;
        private Entry<K,V>  next;//基于链表

        public Entry<K, V> getNext() {
            return next;
        }

        public void setNext(Entry<K, V> next) {
            this.next = next;
        }

        public  Entry(K k, V v){
              this.k = k;
              this.v=v;
        }

        public K getK() {
            return k;
        }

        public void setK(K k) {
            this.k = k;
        }

        public V getV() {
            return v;
        }

        public void setV(V  v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return k+"="+v;
        }
    }
}
