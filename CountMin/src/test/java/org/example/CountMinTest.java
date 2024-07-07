package org.example;

import org.example.countmin.CountMin;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class CountMinTest {

    private CountMin<Long> countMin;

    @Before
    public void setUp() {
        HashMap<String, Float> parameters = new HashMap<>();
        parameters.put("width", 10f);
        parameters.put("depth", 5f);
        parameters.put("seed", 42f);
        countMin = new CountMin<>();
        countMin.init(parameters, "Long");
    }

    @Test
    public void testInsert() {
        DataPoint<Long> dp1 = new GenericDataPoint<>(100L);
        DataPoint<Long> dp2 = new GenericDataPoint<>(200L);

        countMin.insert(dp1);
        countMin.insert(dp2);

        // Expect the counts for these inserted elements to be 1
        assertEquals(1, countMin.query(dp1).intValue());
        assertEquals(1, countMin.query(dp2).intValue());

        System.out.println("CountMinTest.testInsert passed.");
    }

    @Test
    public void testMerge() throws Exception {
        HashMap<String, Float> parameters = new HashMap<>();
        parameters.put("width", 10f);
        parameters.put("depth", 5f);
        parameters.put("seed", 42f);
        CountMin<Long> countMin1 = new CountMin<>();
        countMin1.init(parameters, "Long");

        CountMin<Long> countMin2 = new CountMin<>();
        countMin2.init(parameters, "Long");
        // Ensure they have the same hash functions
        countMin2.setHashFunctions(countMin1.getHashFunctions());

        DataPoint<Long> dp1 = new GenericDataPoint<>(100L);
        DataPoint<Long> dp2 = new GenericDataPoint<>(200L);

        countMin1.insert(dp1);
        countMin2.insert(dp2);

        CountMin<Long>[] countMinsToCombine = new CountMin[]{countMin2};
        CountMin<Long> mergedSketch = (CountMin<Long>) countMin1.merge(countMinsToCombine);

        // Expect the counts for these inserted elements to be larger or equal to 1 after merging
        assert(mergedSketch.query(dp1) >= 1);
        assert(mergedSketch.query(dp2) >= 1);

        System.out.println("CountMinTest.testMerge passed.");
    }

}
