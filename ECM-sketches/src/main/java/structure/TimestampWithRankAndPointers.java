package structure;

public class TimestampWithRankAndPointers implements Comparable<TimestampWithRankAndPointers> {
	public final int time;
	public final int rank;
	short level;
	public TimestampWithRankAndPointers previous;
	public TimestampWithRankAndPointers next;
	
	public void setPrevious(TimestampWithRankAndPointers p) {
		this.previous=p;
	}
	public void setNext(TimestampWithRankAndPointers p) {
		this.next=p;
	}
	
	public void setLevel(int level) {
		this.level=(short)level;
	}
	public short getLevel() {
		return this.level;
	}
	public int getRank() {
		return rank;
	}
	
	public int getTime() {
		return time;
	}
	public TimestampWithRankAndPointers(int time, int rank) {
		this.time=time;
		this.rank = rank;
	}
		
	public static TimestampWithRankAndPointers getMax(TimestampWithRankAndPointers t1, TimestampWithRankAndPointers t2) {
		if (t1.compareTo(t2)>0) 
			return t1;
		else 
			return t2;
	}

	public int getExpiredTimestamp(int windowSize) {
		return (this.time-windowSize);
	}
	
	public int compareTo(TimestampWithRankAndPointers t1) {
		if (this.time>t1.time) return 1;
		else if (this.time<t1.time) return -1;
		else 
			return (int)(this.rank-t1.rank);
	}
	public boolean isAtOrAfter(TimestampWithRankAndPointers t1) {
		return this.compareTo(t1)>=0;
	}

	public boolean isAfter(TimestampWithRankAndPointers t1) {
		return this.compareTo(t1)>0;
	}
	public boolean isAt(TimestampWithRankAndPointers t1) {
		return this.compareTo(t1)==0;
	}
	final static TimestampWithRankAndPointers zero = new TimestampWithRankAndPointers(0,0);
	public boolean isBeforeZero() {
		return this.compareTo(zero)<0;
	}
	public boolean isBefore(TimestampWithRankAndPointers t1) {
		return this.compareTo(t1)<0;
	}
	public boolean isBeforeOrAt(TimestampWithRankAndPointers t1) {
		return this.compareTo(t1)<=0;
	}
	public String toString(){
		return "[t" + time+",r"+rank + "] ";
		
	}
}