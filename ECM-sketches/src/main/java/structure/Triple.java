package structure;

public class Triple implements Comparable<Triple> {
	public double val1;
	public double val2;
	public double val3;

	public Triple(double val1, double val2, double val3) {
		this.val1=val1;
		this.val2=val2;
		this.val3=val3;
	}

	@Override
	public int compareTo(Triple t) {
		if (val1==t.val1) return 0;
		else if (val1<t.val1) return -1;
		else return 1;
	}

	public String toString() {
		return "(" + val1 + "," + val2 + "," + val3 + ")";
	}
}

