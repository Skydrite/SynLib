package org.example.sketch.hash;

public class LongHashFunctions extends HashFunctions {
    public int[] hash(long attrValue, int depth, int width) {
        int[] hash = new int[depth];
        rn.setSeed(attrValue + seed);
        for (int i = 0; i < depth; i++) hash[i] = rn.nextInt(width);
        return hash;
    }

    @Override
    public int[] hash(String attrValue, int depth, int width) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
