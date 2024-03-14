package structure;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class cBufferInt implements Cloneable, Iterable<Integer> {
	public cBufferInt clone() {
		cBufferInt newcb = new cBufferInt(this);
		return newcb;
	}
	
	
    int binarySearch(int key)
    {
        int low = 0;
        int high = this.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = this.get(mid);
            int cmp=midVal-key;
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
		cBufferInt cb = new cBufferInt(10);
		for (int i=0;i<10;i++) {
			cb.addLast(i*2);
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
	int[] elements;
	int head=-1, tail=-1;  // head holds the first element added (the next element to remove), tail the place to add the NEW element
	boolean empty=true;
	int capacity;
	public cBufferInt(int size) {
		elements = new int[size];
		for (int i=0;i<elements.length;i++) elements[i]=-Integer.MAX_VALUE;
		this.capacity=size;
		this.tail=0;
	}
	public cBufferInt(cBufferInt cb) {
		elements = cb.elements.clone();
		this.capacity=cb.capacity;
		this.head=cb.head; this.tail=cb.tail; this.empty=cb.empty;
	}
	int mapLogicalPositionToReal(int logicalPos) {
		int real = logicalPos+head;
		return real%capacity;
	}
	
	public Iterator<Integer> descendingIterator() {
		return new DescendingIterator();
	}
	public Iterator<Integer> Iterator() {
		return new DeqIterator();
	}
	public Iterator<Integer> iterator() {
		return new DeqIterator();
	}
	public int removeFirst(){
		return pollFirst();
	}
	public int pollFirst() {
		if (isEmpty()) return -Integer.MAX_VALUE;
		else {
			int realPos  = (head);
			int e = elements[realPos];
			elements[realPos]=-Integer.MAX_VALUE;
			head++;
			head%=elements.length;
			empty = (elements[(head)]==-Integer.MAX_VALUE);
			if (empty) {
				head=-1;tail=0;
			}
			return e;
		}
	}

	public boolean isEmpty() {
		return empty;
	}
	public int getFirst() {
		if (isEmpty()) return -Integer.MAX_VALUE;
		else {
			return elements[(head)];
		}
	}
	public int getSecond() {
		return get(1);
	}

	public void clear() {
		elements = new int[capacity];
		head=-1;tail=0;
		empty=true;
	}
	public int getLast() {
		if (isEmpty()) return -Integer.MAX_VALUE;
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
	
	public int get(int pos) {
		if (isEmpty()) 
			return -Integer.MAX_VALUE;
		else {
			return elements[mapLogicalPositionToReal(pos)];
		}
	}
	
	public void addLast(int e) {
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
		for (int i=0;i<this.size();i++) {
			sb.append(" " + get(i));
			
//			if (elements[i]!=Integer.MIN_VALUE+1) 
//				sb.append(" " + elements[i]);
		}
		
//		sb.append ( "   Total size " + size() + ", oldest " + get((0)) + " newest " + get((size()-1)));
		return sb.toString();
	}
	
    private class DeqIterator implements Iterator<Integer> {
        private int cursor = head;
        private int lastRet = 0;

        public boolean hasNext() {
            return lastRet<elements.length && elements[cursor%elements.length]!=-Integer.MAX_VALUE;
        }

        public Integer next() {
        	lastRet++;
            int result = elements[cursor%elements.length];
            // This check doesn't catch all possible comodifications,
            // but does catch the ones that corrupt traversal
            if (result == -Integer.MAX_VALUE)
                throw new ConcurrentModificationException();
            cursor = (cursor + 1) % (elements.length);
            return result;
        }

        public void remove() {
        	System.err.println("Not supported");
        }
    }


	private class DescendingIterator implements Iterator<Integer> {
	    /*
	     * This class is nearly a mirror-image of DeqIterator, using
	     * tail instead of head for initial cursor, and head instead of
	     * tail for fence.
	     */
	    private int cursor = (tail-1+elements.length)%elements.length;
	    private int lastRet = 0;

	    public boolean hasNext() {
            return lastRet<elements.length && elements[cursor]!=-Integer.MAX_VALUE;
	    }

	    public Integer next() {
	    	lastRet++;
	        int result = elements[cursor];
	        if (result == -Integer.MAX_VALUE)
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

