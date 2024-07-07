package org.example.countmin.hash;

public interface HashFunctions<T> {
    int[] hash(T attrValue, int depth, int width);
}
