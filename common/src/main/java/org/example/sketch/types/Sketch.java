package org.example.sketch.types;

import java.io.Serializable;
import java.util.HashMap;

/**
 * A synopses sketch is a probabilistic data structure that is used to summarize a data stream.
 * This interface covers all the basic operations that a sketch should support.
 * @param <R> The type of the query result.
 */
interface Sketch<R> extends Serializable {

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
     * Insert a Stream of data given the endpoint of the stream.
     * @param endpoint the endpoint of the stream.
     */
    void insertStream(String endpoint);

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
