package pers.pandora;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * complete my own skip list
 * {@link ConcurrentSkipListMap}
 * @author pandora
 *
 */
public class MySkipList<T> {
	
	private int height;//high level
	private final int HEAD_P = -1;
	private final int TAIL_P = -1;
    private final int DATA_P = 0;
    private int size;//the number of elements in last-level skip-list
    private Node<T> head,tail;//head & tail pointer

	static class  Node<T>{
		int dataType;
		T data;
		boolean flag;
		Node<T> up,down,left,right;//the skip list Node
		Node(int dataType,T data){
			this.data = data;
			this.dataType = dataType;
			flag = true;
		}
		boolean lt(T t){
			return t!=null?this.data.hashCode()-t.hashCode()<=0:true;
		}
	}
	
	public MySkipList() {
		head = new Node<>(HEAD_P,null);
		tail = new Node<>(TAIL_P,null);
		head.right = tail;
		tail.left = head;
	}

	public Node<T> find(T element){
		Node<T> cursor = head;//index current node location
		retry:
		for(;;){
			while(cursor.right.dataType!=TAIL_P && cursor.right.lt(element)){
				cursor = cursor.right;
			}
			if(cursor.down != null){
				cursor = cursor.down;
			}else{
				break retry;
			}
		}
		return cursor;//it sure that (left <= element < right)
	}

    /**
     * It just sure that it will remove the root elements in last-level
     * maybe it will remain many skip-level elements,them can't be the root elemenets
     * so it wasted
     * @param t
     */
	public void remove(T t){
	    Node<T> node = find(t);
        if(node.dataType!=HEAD_P&&node.data.equals(t)){
            if(node.left.right.flag&&node.right.flag&&node.right.left.flag&&node.left.flag) {
                node.left.right = node.right;
                node.right.left = node.left;
                node.flag = false;
                size--;
            }
        }
	}

	public boolean contains(T element){
	    Node<T> node = find(element);
		return node.data!=null&&node.flag?node.data.equals(element):false;
	}
	
	public int size(){
		return size;
	}
	
	public void printAllElments(){
		Node cursor = head;//index current node location
        Node headIndex = head;//save the head pointer
		int curLevel = height+1;//the first level is 1,so the height same as
		over:
		for(;;){
            System.out.print("total["+(height+1)+"],current["+curLevel-- +"]");
			while(cursor.right.dataType!=TAIL_P){
				cursor = cursor.right;
                System.out.print(cursor.data + " ");
			}
			System.out.println();
			if(headIndex.down!=null){
				headIndex = headIndex.down;
				cursor = headIndex;
			}else{
				break over;
			}
		}
	}
	
	public void add(T element){
		if(element == null){
			throw new RuntimeException("element not null");
		}
		if(size>Integer.MAX_VALUE){
			throw new RuntimeException("over int max");
		}
		Node<T> current = find(element);
		Node<T> newNode = new Node<>(DATA_P,element);
		newNode.left = current;
		newNode.right = current.right;
		current.right.left = newNode;
		current.right = newNode;
		size++;
		Node<T> autoNode = new Node<>(DATA_P,element);
		while(ThreadLocalRandom.current().nextDouble()<0.25){//auto-spread the capacity of the skip-list
		    while(current.up == null&&current.dataType!=HEAD_P){
                current = current.left;
            }
            if(current .dataType!=HEAD_P) {
                current = current.up;
                autoNode.left = current;
                autoNode.right = current.right;
                current.right.left = autoNode;
                current.right = autoNode;
                autoNode.down = newNode;
                newNode.up = autoNode;
            }else {
                Node<T> newHead = new Node<>(HEAD_P, null);
                Node<T> newTail = new Node<>(TAIL_P, null);
                newHead.down = head;
                head.up = newHead;
                newTail.down = tail;
                tail.up = newTail;
                newHead.right = autoNode;
                newTail.left = autoNode;
                autoNode.left = newHead;
                autoNode.right = newTail;
                autoNode.down = newNode;
                newNode.up = autoNode;
                head = newHead;
                tail = newTail;
                height++;
                break;
            }
		}
	}
}
