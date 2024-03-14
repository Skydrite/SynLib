package structure;

public class Tuple implements Comparable<Tuple>{
	public int t;
	public int val;
	public Tuple(int t, int val) {
		this.t=t;
		this.val=val;
	}
	public int compareTo(Tuple newtuple) {
		if (this.t>(newtuple.t)) return 1;
		else if (this.t<(newtuple.t)) return -1;
		else return 0;
	}
	
	public String toString() {
		return ("(" + t + "," + val +") ");
	}
}
class IntDoubleTuple {
	public int i;
	public double d;
	public IntDoubleTuple(int i, double d) {
		this.i=i;
		this.d=d;
	}
	public String toString() {
		return ("(" + i + "," + d +") ");
	}
}


class BucketTuple implements Comparable<BucketTuple>{
	public Bucket b;
	public int val;
	public BucketTuple(Bucket b, int val) {
		this.b=b;
		this.val=val;
	}
	public int compareTo(BucketTuple newtuple) {
		if (this.b.getTime()>(newtuple.b.getTime())) return 1;
		else if (this.b.getTime()<(newtuple.b.getTime())) return -1;
		else return 0;
	}
	
	public String toString() {
		return ("(" + b.getTime() + ":" + b.getTrueBits() + "," + val +") ");
	}
}

