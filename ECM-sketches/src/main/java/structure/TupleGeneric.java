package structure;

public class TupleGeneric<T1,T2> {
	T1 first;
	T2 second;
	public TupleGeneric(T1 first, T2 second) {
		this.first=first;
		this.second=second;
	}
	public T1 getFirst() {
		return first;
	}
	public T2 getSecond() {
		return second;
	}
}
