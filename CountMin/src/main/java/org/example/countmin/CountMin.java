package org.example.countmin;


import org.example.DataPoint;
import org.example.IncompatibleSketchException;
import org.example.SynopsesSketch;
import org.example.countmin.hash.*;

import java.util.Arrays;
import java.util.HashMap;

public class CountMin<T> implements SynopsesSketch<T, Integer> {

    //ArrayList<ArrayList<Sample>> CM = new ArrayList<>();

    int[][] CM;

    //ArrayList<UniformHash> hashFunctions;

    int attr;
    int width;
    int depth;
    private HashFunctions<T> hashFunctions;

    @Override
    public void init(HashMap<String, Float> sketchParametersMap, String dataType) {
        this.width = sketchParametersMap.get("width").intValue();
        this.depth = sketchParametersMap.get("depth").intValue();
        HashFunctionsBase.setSeed(sketchParametersMap.get("seed").intValue());
        // Set up hash function based on data type
        this.hashFunctions = createHashFunctions(dataType);

        this.CM = new int[depth][width];

        initCountMin();
    }

    public void setHashFunctions(HashFunctions<T> hashFunctions) {
        this.hashFunctions = hashFunctions;
    }

    @SuppressWarnings("unchecked")
    private HashFunctions<T> createHashFunctions(String dataType) {
        HashFunctions<?> hashFunction = HashFunctionsFactory.createHashFunctions(dataType);
        return (HashFunctions<T>) hashFunction;
    }

    private void initCountMin() {
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                CM[j][i] = 0;
            }
        }
    }

    @Override
    public void insert(DataPoint<T> dp) throws UnsupportedOperationException {
        // Test if all element in A and B are consistent
        int[] hashes = hashFunctions.hash(dp.getValue(), depth, width);
        for (int j = 0; j <depth; j++) {
            int w = hashes[j];
            CM[j][w] += 1; // Hash in Sample based on id
        }
    }

    @Override
    public void insertBatch(DataPoint<T>[] dps) {

    }

    @Override
    public void insertStream(String endpoint) {

    }

    @Override
    public long getMemoryUsage() {
        long memoryUsage = 0;
        for (int j = 0; j <depth; j++) {
            for (int i = 0; i < width; i++) {
                //System.out.println("CM[" + j + "][" + i + "].curSampleSize = " + CM.get(j).get(i).curSampleSize);
                memoryUsage += CM[j][i] * 4L;
            }
        }
        return memoryUsage;
    }

    @Override
    public HashMap<String, Float> getConfig() {
        return null;
    }

    @Override
    public SynopsesSketch<T, Integer> merge(SynopsesSketch<T, Integer>[] other) throws IncompatibleSketchException {
        // Check types of others
        for (SynopsesSketch<T, Integer> tIntegerSynopsesSketch : other) {
            if (!(tIntegerSynopsesSketch instanceof CountMin)) {
                throw new IncompatibleSketchException("The sketches are not compatible (type)");
            }
        }
        // Check width and depth
        for (SynopsesSketch<T, Integer> tIntegerSynopsesSketch : other) {
            if (this.width != ((CountMin<T>) tIntegerSynopsesSketch).width || this.depth != ((CountMin<T>) tIntegerSynopsesSketch).depth) {
                throw new IncompatibleSketchException("The sketches are not compatible (size)");
            }
        }
        // Check hash functions
        for (SynopsesSketch<T, Integer> tIntegerSynopsesSketch : other) {
            if (!this.hashFunctions.equals(((CountMin<T>) tIntegerSynopsesSketch).hashFunctions)) {
                throw new IncompatibleSketchException("The sketches are not compatible (hash functions)");
            }
        }
        CountMin<T> merged = this;
        // Merge sketches
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                for (SynopsesSketch<T, Integer> tIntegerSynopsesSketch : other) {
                    CM[j][i] += ((CountMin<T>) tIntegerSynopsesSketch).CM[j][i];
                }
            }
        }
        return merged;
    }

    @Override
    public Integer query(DataPoint<T> dp) {
        int[] hashes = hashFunctions.hash(dp.getValue(), depth,width);

        int[] result = new int[depth];
        for (int j = 0; j < depth; j++) {
            int w = hashes[j];
            result[j] = CM[j][w];
            //result.add(new DistinctSample(CM.get(j).get(w)));
        }
        Arrays.sort(result);
        return result[0];
    }

    @Override
    public Integer[] queryBatch(DataPoint<T>[] dps) {
        // TODO: Implement
        throw new UnsupportedOperationException();
    }

    public void reset() {
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                CM[j][i] = 0;
            }
        }
    }

    public HashFunctions<T> getHashFunctions() {
        return hashFunctions;
    }


    /*
        Input: 1. Attribute
        Output: 2. Arraylist of size depth with all relevant Distinct samples for single attribute
     */
}