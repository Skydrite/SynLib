package org.example.countmin.hash;

public class HashFunctionsLong extends HashFunctionsBase<Long> {

    @Override
    public int[] hash(Long attrValue, int depth, int width) {
        int[] hash = new int[depth];
        rn.setSeed(attrValue + seed);
        for (int i = 0; i < depth; i++) hash[i] = rn.nextInt(width);
        return hash;
    }
}
