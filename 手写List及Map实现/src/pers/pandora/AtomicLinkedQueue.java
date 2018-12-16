package pers.pandora;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * define my own no lock (CAS) queue in threads
 * queue base on FIFO
 * by pandora 2018/12/16
 */
public class AtomicLinkedQueue<T> {
    private AtomicReference<Node<T>> first,last;
    private AtomicInteger size;
    public AtomicLinkedQueue() {
        Node<T> node = new Node<>(null, null);
        last = new AtomicReference<>(node);
        first = new AtomicReference<>(node);
        size = new AtomicInteger();
    }

    private static class Node<T> {
        T data;
        volatile Node<T> next;

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public Node<T> getNext() {
            return next;
        }

        public void setNext(Node<T> next) {
            this.next = next;
        }

        public Node(T data, Node<T> next) {
            this.data = data;
            this.next = next;
        }
    }
    public void addLast(T data){
        if(data==null) throw new RuntimeException("not null element");
        Node<T> preNode;
        Node<T> newNode = new Node<>(data, null);
        preNode = last.getAndSet(newNode);
        preNode.setNext(newNode);
        size.getAndIncrement();
    }
    public  T removeFirst(){
        Node<T> preNode,nextNode;
        do {
            preNode = first.get();
            nextNode = preNode.getNext();
        }while (nextNode!=null&&!first.compareAndSet(preNode,nextNode));
        T value = nextNode!=null?nextNode.data:null;
        if(nextNode!=null){
            nextNode.data = null;//as the head node use
        }
        if(nextNode!=null) {
            size.decrementAndGet();
        }
        return value;
    }
    public int  size(){
        return size.get();
    }
    public T peek(){
        Node<T> element = first.get();
        return element!=null?element.data:null;
    }
    public boolean isEmpty(){
        return first.get().next==null?true:false;
    }
}
