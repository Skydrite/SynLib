package org.example.countmin;


import org.example.sketch.IncompatibleSketchException;
import org.example.sketch.hash.HashFunctions;
import org.example.sketch.hash.HashFunctionsFactory;
import org.example.sketch.types.LongSketch;

import java.util.Arrays;
import java.util.HashMap;

public class CountMin implements LongSketch<Integer> {

    //ArrayList<ArrayList<Sample>> CM = new ArrayList<>();

    int[][] CM;

    //ArrayList<UniformHash> hashFunctions;

    int attr;
    int width;
    int depth;
    private HashFunctions hashFunctions;

    HashMap<String, Float> config;

    @Override
    public void init(HashMap<String, Float> sketchParametersMap, String dataType) {
        this.config = sketchParametersMap;
        this.width = sketchParametersMap.get("width").intValue();
        this.depth = sketchParametersMap.get("depth").intValue();
        HashFunctions.setSeed(sketchParametersMap.get("seed").intValue());
        // Set up hash function based on data type
        this.hashFunctions = createHashFunctions(dataType);

        this.CM = new int[depth][width];

        initCountMin();
    }

    public void setHashFunctions(HashFunctions hashFunctions) {
        this.hashFunctions = hashFunctions;
    }

    private HashFunctions createHashFunctions(String dataType) {
        return HashFunctionsFactory.createHashFunctions(dataType);
    }

    private void initCountMin() {
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                CM[j][i] = 0;
            }
        }
    }

    @Override
    public void insertLong(long value) {
        // Test if all element in A and B are consistent
        int[] hashes = hashFunctions.hash(value, depth, width);
        for (int j = 0; j <depth; j++) {
            int w = hashes[j];
            CM[j][w] += 1; // Hash in Sample based on id
        }
    }

    @Override
    public void insertBatchLong(long[] values) {
        for (long value : values) {
            insertLong(value);
        }
    }

    @Override
    public void insertStream(String endpoint) {
        // TODO: Support streams
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
    public LongSketch<Integer> merge(LongSketch<Integer>[] other) throws IncompatibleSketchException {
        // Check types of others
        for (LongSketch<Integer> tIntegerSynopsesSketch : other) {
            if (!(tIntegerSynopsesSketch instanceof CountMin)) {
                throw new IncompatibleSketchException("The sketches are not compatible (type)");
            }
        }
        // Check width and depth
        for (LongSketch<Integer> longSketch : other) {
            if (this.width != ((CountMin) longSketch).width || this.depth != ((CountMin) longSketch).depth) {
                throw new IncompatibleSketchException("The sketches are not compatible (size)");
            }
        }
        // Check hash functions
        for (LongSketch<Integer> longSketch : other) {
            if (!this.hashFunctions.equals(((CountMin) longSketch).hashFunctions)) {
                throw new IncompatibleSketchException("The sketches are not compatible (hash functions)");
            }
        }
        CountMin merged = this;
        // Merge sketches
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                for (LongSketch<Integer> tIntegerSynopsesSketch : other) {
                    CM[j][i] += ((CountMin) tIntegerSynopsesSketch).CM[j][i];
                }
            }
        }
        return merged;
    }

    public void reset() {
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                CM[j][i] = 0;
            }
        }
    }

    public HashFunctions getHashFunctions() {
        return hashFunctions;
    }

    @Override
    public Integer query(long attribute) {
        int[] hashes = hashFunctions.hash(attribute, depth,width);

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
    public Integer[] queryBatch(long[] attribute) {
        // TODO: Implement
        return new Integer[0];
    }


    /*
        Input: 1. Attribute
        Output: 2. Arraylist of size depth with all relevant Distinct samples for single attribute
     */
}