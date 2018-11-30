package pers.pandora.impl;

import pers.pandora.MyList;
import  pers.pandora.exception.*;

//基于数组
public class MyArrayListImpl<T> implements MyList<T> {
    Object[] data;//存放元素
    int size;//容器尺寸
    int length;//容器长度
    public MyArrayListImpl(){
        this(16);
    }

    public MyArrayListImpl(int capacity){
         length = capacity;
         data = new Object[length];
    }

    @Override
    public int add(T t) {
        if(size>=length){//自动增长
            Object[] dataTemp = new Object[(length << 1)];
            System.arraycopy(data,0,dataTemp,0,data.length);
            data = dataTemp;
            length = dataTemp.length;
        }
        data[size] = t;
        size++;
        return size-1;
    }

    @Override
    public T get(int index) {
        if(index>length-1||index<0) {
            throw new IllgealIndexException("下标不合法");
        }
        return (T)data[index];
    }

    @Override
    public int size() {
        return size;
    }
    @Override
    public void delete(int index) {
        if(index>size||index<0) {
            throw new IllgealIndexException("下标不合法");
        }
        for (int i = index+1; i<size;i++){
              data[i-1]= data[i];
        }
        size--;
    }

    @Override
    public String toString() {
        StringBuilder desc = new StringBuilder("[");
        for (int i = 0;i<size;i++) {
            T t =(T)data[i];
            if(i==size-1){
               desc.append(t);
            }else{
                desc.append(t+",");
            }
        }
        desc.append("]");
        return desc.toString();
    }
}
