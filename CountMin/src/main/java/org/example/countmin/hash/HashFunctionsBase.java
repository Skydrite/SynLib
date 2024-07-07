package org.example.countmin.hash;

import java.util.Random;

public abstract class HashFunctionsBase<T> implements HashFunctions<T> {
    protected final Random rn = new Random();
    protected static int seed;

    public static void setSeed(int seedToSet) {
        seed = seedToSet;
    }
}
