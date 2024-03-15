package structure;

public class TimestampWithRank implements Comparable<TimestampWithRank> {
	public final int time;
	public final int rank;
//	short level;
//	public void setLevel(int level) {
//		this.level=(short)level;
//	}
//	public short getLevel() {
//		return this.level;
//	}
	public int getRank() {
		return rank;
	}
	
	public int getTime() {
		return time;
	}
	public TimestampWithRank(int time, int rank) {
		this.time=time;
		this.rank = rank;
	}
		
	public static TimestampWithRank getMax(TimestampWithRank t1, TimestampWithRank t2) {
		if (t1.compareTo(t2)>0) 
			return t1;
		else 
			return t2;
	}

	public int getExpiredTimestamp(int windowSize) {
		return (this.time-windowSize);
	}
	
	public int compareTo(TimestampWithRank t1) {
		if (this.time>t1.time) return 1;
		else if (this.time<t1.time) return -1;
		else 			
			return 0;//this.rank-t1.rank;
	}
	public boolean isAtOrAfter(TimestampWithRank t1) {
		return this.compareTo(t1)>=0;
	}

	public boolean isAfter(TimestampWithRank t1) {
		return this.compareTo(t1)>0;
	}
	public boolean isAfter(int t1) {
		return this.time>=t1;
	}
	public boolean isAt(TimestampWithRank t1) {
		return this.compareTo(t1)==0;
	}
	final static TimestampWithRank zero = new TimestampWithRank(0,0);
	public boolean isBeforeZero() {
		return this.compareTo(zero)<0;
	}
	public boolean isBefore(TimestampWithRank t1) {
		return this.compareTo(t1)<0;
	}
	public boolean isBefore(int t1) {
		return this.time<t1;
	}
	public boolean isBeforeOrAt(TimestampWithRank t1) {
		return this.compareTo(t1)<=0;
	}
	public String toString(){
		return "[" + time+ "] ";
//		return "[" + time+","+rank + "] ";
		
	}
}