package org.example.sketch.hash;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class StringHashFunctions extends HashFunctions {
    // TODO: Check .hashCode() method
    public int[] hash(String attrValue, int depth, int width) {
        int[] hash = new int[depth];
        rn.setSeed(seed);
        CRC32 crc = new CRC32();
        crc.update(attrValue.getBytes(StandardCharsets.UTF_8));
        long hashValue = crc.getValue();
        for (int i = 0; i < depth; i++) {
            rn.setSeed(hashValue + i);
            hash[i] = rn.nextInt(width);
        }
        return hash;
    }

    @Override
    public int[] hash(long attrValue, int depth, int width) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

}
