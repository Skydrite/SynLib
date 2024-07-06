package src.main.java.com.synopses;

public interface DataPoint<T> {
    T getValue();
    void setValue(T value);
}
