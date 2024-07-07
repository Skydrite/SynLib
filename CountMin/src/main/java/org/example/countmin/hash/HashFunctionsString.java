package org.example.countmin.hash;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class HashFunctionsString extends HashFunctionsBase<String> {
    @Override
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
}
