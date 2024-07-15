package org.example;

import org.example.countmin.CountMin;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class CountMinTest {

    private CountMin countMin;

    @Before
    public void setUp() {
        HashMap<String, Float> parameters = new HashMap<>();
        parameters.put("width", 10f);
        parameters.put("depth", 5f);
        parameters.put("seed", 42f);
        countMin = new CountMin();
        countMin.init(parameters, "Long");
    }

    @Test
    public void testInsert() {
        countMin.insertLong(100L);
        countMin.insertLong(200L);

        // Expect the counts for these inserted elements to be 1
        assertEquals(1, countMin.query(100L).intValue());
        assertEquals(1, countMin.query(200L).intValue());

        System.out.println("CountMinTest.testInsert passed.");
    }

    @Test
    public void testMerge() throws Exception {
        HashMap<String, Float> parameters = new HashMap<>();
        parameters.put("width", 10f);
        parameters.put("depth", 5f);
        parameters.put("seed", 42f);
        CountMin countMin1 = new CountMin();
        countMin1.init(parameters, "Long");

        CountMin countMin2 = new CountMin();
        countMin2.init(parameters, "Long");
        // Ensure they have the same hash functions
        countMin2.setHashFunctions(countMin1.getHashFunctions());

        countMin1.insertLong(100L);
        countMin2.insertLong(200L);

        CountMin[] countMinsToCombine = new CountMin[]{countMin2};
        CountMin mergedSketch = (CountMin) countMin1.merge(countMinsToCombine);

        // Expect the counts for these inserted elements to be larger or equal to 1 after merging
        assert(mergedSketch.query(100L) >= 1);
        assert(mergedSketch.query(200L) >= 1);

        System.out.println("CountMinTest.testMerge passed.");
    }

}
