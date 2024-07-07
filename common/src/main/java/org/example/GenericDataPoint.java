package org.example;

public class GenericDataPoint<T> implements DataPoint<T> {

    private T value;

    public GenericDataPoint(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue(T value) {
        this.value = value;
    }

}
