package org.example.datapoint;

public interface DataPoint<T> {
    T getValue();
    void setValue(T value);
}
