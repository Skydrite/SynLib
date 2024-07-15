package org.example.sketch.types;

import org.example.sketch.IncompatibleSketchException;

public interface LongSketch<R> extends Sketch<R> {

    void insertLong(long value);
    void insertBatchLong(long[] values);

    /**
     * Query the sketch for a DataPoint.
     * @param attribute The DataPoint that is to be queried.
     * @return The query result
     */
    R query(long attribute);

    /**
     * Query the sketch for a batch of DataPoints.
     * @param attribute The DataPoints that are to be queried.
     * @return The DataPoints that are queried.
     */
    R[] queryBatch(long[] attribute);

    /**
     * Merges the sketch with another sketch.
     * @param other The sketch that is to be merged with the current sketch.
     * @return The merged sketch.
     * @throws IncompatibleSketchException In case the sketches are not compatible (size, hash functions).
     */
    LongSketch<Integer> merge(LongSketch<Integer>[] other) throws IncompatibleSketchException;
}
