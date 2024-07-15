package org.example.sketch.hash;

import java.util.Random;

public abstract class HashFunctions {
    protected final Random rn = new Random();
    protected static int seed;

    public static void setSeed(int seedToSet) {
        seed = seedToSet;
    }

    public abstract int[] hash(long attrValue, int depth, int width);
    public abstract int[] hash(String attrValue, int depth, int width);
}
