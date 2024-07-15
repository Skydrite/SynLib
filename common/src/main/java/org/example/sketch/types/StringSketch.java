package org.example.sketch.types;

import org.example.sketch.IncompatibleSketchException;

public interface StringSketch<R> extends Sketch<R> {

    void insertString(String value);
    void insertBatchString(String[] values);

    /**
     * Query the sketch for a DataPoint.
     * @param attribute The DataPoint that is to be queried.
     * @return The query result
     */
    R query(String attribute);

    /**
     * Query the sketch for a batch of DataPoints.
     * @param attribute The DataPoints that are to be queried.
     * @return The DataPoints that are queried.
     */
    R queryBatch(String[] attribute);

    /**
     * Merges the sketch with another sketch.
     * @param other The sketch that is to be merged with the current sketch.
     * @return The merged sketch.
     * @throws IncompatibleSketchException In case the sketches are not compatible (size, hash functions).
     */
    StringSketch<R> merge(StringSketch<R>[] other) throws IncompatibleSketchException;
}
