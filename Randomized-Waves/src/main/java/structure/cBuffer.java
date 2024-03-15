package structure;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class cBuffer<E extends Comparable<E>> implements Cloneable, Iterable<E> {
	public cBuffer<E> clone() {
		cBuffer<E> newcb = new cBuffer<E>(this);
		return newcb;
	}
	
	
    int binarySearch(E key)
    {
        int low = 0;
        int high = this.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<? super E> midVal = this.get(mid);
            int cmp = midVal.compareTo(key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }


	public static void main(String[]args) {
		cBuffer<Integer> cb = new cBuffer<>(10);
		for (int i=0;i<10;i++) {
			cb.addLast((Integer)i*2);
		}
		System.err.println(cb.toString());
		for (int i=-1;i<22;i++) {
			System.err.println("Binary search  " + i +" result " + cb.binarySearch(i));			
		}

		
		for (int i=0;i<20;i++) {
			cb.addLast(i);
			System.err.println(cb);
			if (i==5) {
				System.err.println("Val " + cb.pollFirst() + " removed");
				System.err.println("Val " + cb.pollFirst() + " removed");
				System.err.println("Val " + cb.pollFirst() + " removed");
				System.err.println("Val " + cb.pollFirst() + " removed");
			}
		}
	}
	E[] elements;
	int head=-1, tail=-1;  // head holds the first element added (the next element to remove), tail the place to add the NEW element
	boolean empty=true;
	int capacity;
	public cBuffer(int size) {
		elements = (E[]) new Comparable[size];
		this.capacity=size;
		this.tail=0;
	}
	public cBuffer(cBuffer<E> cb) {
		elements = cb.elements.clone();
		this.capacity=cb.capacity;
		this.head=cb.head; this.tail=cb.tail; this.empty=cb.empty;
	}
	int mapLogicalPositionToReal(int logicalPos) {
		int real = logicalPos+head;
		return real%capacity;
	}
	
	public Iterator<E> descendingIterator() {
		return new DescendingIterator();
	}
	public Iterator<E> Iterator() {
		return new DeqIterator();
	}
	public Iterator<E> iterator() {
		return new DeqIterator();
	}
	public E removeFirst(){
		return pollFirst();
	}
	public E pollFirst() {
		if (isEmpty()) return null;
		else {
			int realPos  = (head);
			E e = elements[realPos];
			elements[realPos]=null;
			head++;
			head%=elements.length;
			empty = (elements[(head)]==null);
			if (empty) {
				head=-1;tail=0;
			}
			return e;
		}
	}

	public boolean isEmpty() {
		return empty;
	}
	public E getFirst() {
		if (isEmpty()) return null;
		else {
			return elements[(head)];
		}
	}
	public E getSecond() {
		return get(1);
	}

	public void clear() {
		elements = (E[]) new Comparable[capacity];
		head=-1;tail=0;
		empty=true;
	}
	public E getLast() {
		if (isEmpty()) return null;
		else {
			return get(size()-1);
		}
	}
	public int size() {
		if (isEmpty()) 
			return 0;
		else if (head==tail || head+capacity==tail)
			return capacity;
		else
			return (tail + capacity - head)%capacity;
	}
	
	public E get(int pos) {
		if (isEmpty()) 
			return null;
		else {
			return elements[mapLogicalPositionToReal(pos)];
		}
	}
	
	public void addLast(E e) {
		if (this.tail==this.head) { // overwrite one element
			this.head=(this.head+1)%capacity;
		}
		else if (head==-1) {
			head=0;
		}
		empty=false;
		elements[(this.tail++)]=e;
		this.tail%=capacity;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
//		Iterator<E> it = this.Iterator();
//		while (it.hasNext()) {
//			sb.append(" " + it.next());
//		}
		for (int i=0;i<size();i++) {
			sb.append(" " + elements[i]);
		}
		
		sb.append ( "   Total size " + size() + " and oldest object is " + get((0)) + " and newest " + get((size()-1)));
		return sb.toString();
	}
	
    private class DeqIterator implements Iterator<E> {
        private int cursor = head;
        private int lastRet = 0;

        public boolean hasNext() {
            return lastRet<elements.length && elements[cursor%elements.length]!=null;
        }

        public E next() {
        	lastRet++;
            E result = elements[cursor%elements.length];
            // This check doesn't catch all possible comodifications,
            // but does catch the ones that corrupt traversal
            if (result == null)
                throw new ConcurrentModificationException();
            cursor = (cursor + 1) % (elements.length);
            return result;
        }

        public void remove() {
        	System.err.println("Not supported");
        }
    }


	private class DescendingIterator implements Iterator<E> {
	    /*
	     * This class is nearly a mirror-image of DeqIterator, using
	     * tail instead of head for initial cursor, and head instead of
	     * tail for fence.
	     */
	    private int cursor = (tail-1+elements.length)%elements.length;
	    private int lastRet = 0;

	    public boolean hasNext() {
            return lastRet<elements.length && elements[cursor]!=null;
	    }

	    public E next() {
	    	lastRet++;
	        E result = elements[cursor];
	        if (result == null)
	            throw new ConcurrentModificationException();
	        cursor = (cursor - 1 + elements.length) % (elements.length);
	        return result;
	    }

	    public void remove() {
	    	System.err.println("Not supported");
//	        if (lastRet < 0)
//	            throw new IllegalStateException();
//	        if (!delete(lastRet)) {
//	            cursor = (cursor + 1) & (elements.length - 1);
//	            fence = head;
//	        }
//	        lastRet = -1;
	    }
	}
}

