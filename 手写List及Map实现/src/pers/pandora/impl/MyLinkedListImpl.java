package pers.pandora.impl;


import pers.pandora.MyList;
import pers.pandora.exception.IllgealIndexException;

//基于链表的list：采用了双向链表
public class MyLinkedListImpl<T> implements MyList<T> {
    Entity<T> preEntity;//头元素
    Entity<T> curEntity;//当前元素
    int size;//集合尺寸
    @Override
    public int add(T t) {
        Entity<T> temp = new Entity<>();
        temp.setData(t);
        temp.setNext(null);
        if (preEntity == null) {
            preEntity = temp;
            curEntity = preEntity;
            curEntity.setPre(preEntity);
            temp.setPre(null);
        }else{
            Entity<T> exc = curEntity;
            while(curEntity.getNext()!=null){
                exc = curEntity;
                curEntity = curEntity.getNext();
                curEntity.setPre(exc);
            }
            curEntity.setNext(temp);
            temp.setPre(curEntity);
        }
        return size++;
    }

    @Override
    public T get(int index) {
        Entity<T> temp = preEntity;
        if(index>size||index<0) {
            throw new IllgealIndexException("下标异常");
        }else if(index == 0){
            return temp.getData();
        }
        int cursor = 1;
        if(index>=size/2){//每次判断一下索引在size/2的前部分还是后部分，使其遍历变为一半，缩短时间
            cursor = size/2;
        }
        for (int i = cursor; i <= index; i++) {
            temp = temp.getNext();
        }

        return temp.getData();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void delete(int index) {
        Entity<T> temp = preEntity;
        if(index>size||index<0) {
            throw new IllgealIndexException("下标异常");
        }else if(index == 0){
            preEntity = preEntity.getNext();
            size--;
            return;
        }
        int cursor = 1;
        if(index>=size/2){//每次判断一下索引在size/2的前部分还是后部分，使其遍历变为一半，缩短时间
            cursor = size/2;
        }
        for (int i = cursor; i <=index; i++) {
            temp = temp.getNext();
        }
        temp.getNext().setPre(temp.getPre());
        temp.getPre().setNext(temp.getNext());//断开引用连接，让删除元素能被gc回收
        size --;
    }

    //内部类实用
    static class Entity<T>{
        T  data;//数据域
        Entity<T> next;//下一个元素
        Entity<T> pre;//上一个元素
        void setData(T data){
            this.data = data;
        }
        T getData(){
            return data;
        }
        void setNext(Entity<T> next){
            this.next = next;
        }
        Entity<T> getNext(){
            return next;
        }
        void setPre(Entity<T> pre){
            this.pre = pre;
        }
        Entity<T> getPre(){
            return pre;
        }
        @Override
        public String toString(){
            return data+"";
        }
    }

    @Override
    public String toString() {
        StringBuilder desc = new StringBuilder("[");
        Entity temp = preEntity;
        while(temp!=null) {
            desc.append(temp.getData());
            if(temp.getNext()!=null){
                desc.append(",");
            }
            temp = temp.getNext();
        }
        desc.append("]");
        return desc.toString();
    }
}
