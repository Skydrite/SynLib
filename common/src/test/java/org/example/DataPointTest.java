package org.example;

import org.example.datapoint.DataPoint;
import org.example.datapoint.GenericDataPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataPointTest {

    @Test
    public void testIntegerDataPoint() {
        DataPoint<Integer> dataPoint = new GenericDataPoint<>(42);
        assertEquals(Integer.valueOf(42), dataPoint.getValue());

        dataPoint.setValue(100);
        assertEquals(Integer.valueOf(100), dataPoint.getValue());

        System.out.println("DataPointTest.testIntegerDataPoint passed.");
    }

    @Test
    public void testDoubleDataPoint() {
        DataPoint<Double> dataPoint = new GenericDataPoint<>(3.14);
        assertEquals(Double.valueOf(3.14), dataPoint.getValue());

        dataPoint.setValue(2.71);
        assertEquals(Double.valueOf(2.71), dataPoint.getValue());

        System.out.println("DataPointTest.testDoubleDataPoint passed.");
    }

    @Test
    public void testStringDataPoint() {
        DataPoint<String> dataPoint = new GenericDataPoint<>("Hello");
        assertEquals("Hello", dataPoint.getValue());

        dataPoint.setValue("World");
        assertEquals("World", dataPoint.getValue());

        System.out.println("DataPointTest.testStringDataPoint passed.");
    }
}
