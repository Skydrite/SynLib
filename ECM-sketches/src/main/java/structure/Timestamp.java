package structure;

class Timestampt implements Comparable<Timestampt> {
	final int time;
	public int getTime() {
		return time;
	}
	public Timestampt(int time) {
		float t = (float)time;
		if (Math.abs(t-time)>0.1) {
			System.err.println("Overflow:" + time + " becomes "  + t);
			System.exit(-1);
		}
		this.time=time;
	}

	public static Timestampt getMax(Timestampt t1, Timestampt t2) {
		if (t1.compareTo(t2)>0) 
			return t1;
		else 
			return t2;
	}
//	public void tick() {
//		time++;
//	}
	public Timestampt getExpiredTimestamp(int windowSize) {
		return new Timestampt(this.time-windowSize);
	}
	public Timestampt getExpiredTimestamp(Timestampt windowSize) {
		return new Timestampt(this.time-windowSize.time);
	}
	
	public int compareTo(Timestampt t1) {
		if (this.time>t1.time) return 1;
		else if (this.time<t1.time) return -1;
		else return 0;
	}
	public boolean isAtOrAfter(Timestampt t1) {
		return this.compareTo(t1)>=0;
	}

	public boolean isAfter(Timestampt t1) {
		return this.compareTo(t1)>0;
	}
	final static Timestampt zero = new Timestampt(0);
	public boolean isBeforeZero() {
		return this.compareTo(zero)<0;
	}
	public boolean isBefore(Timestampt t1) {
		return this.compareTo(t1)<0;
	}
	public boolean isBeforeOrAt(Timestampt t1) {
		return this.compareTo(t1)<=0;
	}
	public String toString(){
		return "" + time;
	}
}