package org.example;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Sketch Interface. This interface is used to define the methods that a Sketch implementation must contain.
 */
public interface SynopsesSketch<T, R> extends Serializable {

    /**
     * The Sketch is initialized with the parameters that are passed in the sketchParameters HashMap.
     * @param sketchParametersMap The parameters that are used to initialize the sketch.
     * @param dataType The type of the data that is to be inserted into the sketch.
     */
    void init(HashMap<String, Float> sketchParametersMap, String dataType);

    /**
     * Returns the configuration of the sketch as a HashMap.
     * @return The configuration of the sketch.
     */
    HashMap<String, Float> getConfig();

    /**
     * Inserts a DataPoint into the sketch.
     * @param dp The DataPoint that is to be inserted into the sketch.
     * @throws UnsupportedOperationException In case removals are not supported but tried.
     */
    void insert(DataPoint<T> dp) throws UnsupportedOperationException;

    /**
     * Inserts a batch of DataPoints into the sketch.
     * @param dps The DataPoints that are to be inserted into the sketch.
     */
    void insertBatch(DataPoint<T>[] dps);

    /**
     * Read a stream of DataPoints into the sketch from an endpoint.
     * @param endpoint The endpoint of the stream that is to be inserted into the sketch.
     */
    void insertStream(String endpoint);

    /**
     * Merges the sketch with another sketch.
     * @param other The sketch that is to be merged with the current sketch.
     * @return The merged sketch.
     * @throws IncompatibleSketchException In case the sketches are not compatible (size, hash functions).
     */
    SynopsesSketch<T, R> merge(SynopsesSketch<T, R>[] other) throws IncompatibleSketchException;

    /**
     * Query the sketch for a DataPoint.
     * @param dp The DataPoint that is to be queried.
     * @return The query result
     */
    R query(DataPoint<T> dp);

    /**
     * Query the sketch for a batch of DataPoints.
     * @param dps The DataPoints that are to be queried.
     * @return The DataPoints that are queried.
     */
    R[] queryBatch(DataPoint<T>[] dps);

    /**
     * Reset the sketch to its initial state.
     */
    void reset();

    /**
     * Get the memory usage of the sketch.
     * @return The memory usage of the sketch.
     */
    long getMemoryUsage();
}
