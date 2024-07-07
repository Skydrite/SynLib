package org.example;

public interface DataPoint<T> {
    T getValue();
    void setValue(T value);
}
