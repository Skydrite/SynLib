package org.example.countmin;


import java.util.Arrays;
import java.util.Random;

public class CountMin {

    //ArrayList<ArrayList<Sample>> CM = new ArrayList<>();

    int[][] CM;

    //ArrayList<UniformHash> hashFunctions;

    int attr;
    int width;
    int depth;
    int seed;
    final Random rn = new Random();

    public CountMin(int width, int depth, int seed) {
        CM = new int[depth][width];
        this.width = width;
        this.depth = depth;
        this.seed = seed;
        //this.hashFunctions = new ArrayList<UniformHash>(depth);
        initSketch();
    }


    public void initSketch() {
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                CM[j][i] = 0;
            }
        }
    }

    int[] hash(long attrValue, int depth, int width) {
        int[] hash = new int[depth];
        rn.setSeed(attrValue + seed);
        for (int i = 0; i < depth; i++) hash[i] = rn.nextInt(width);
        return hash;
    }
    public void add(int id, long attrValue, long hx) {
        // Test if all element in A and B are consistent
        int[] hashes = hash(attrValue, depth, width);
        for (int j = 0; j <depth; j++) {
            int w = hashes[j];
            CM[j][w] += 1; // Hash in Sample based on id
        }
    }

    public int query(long attrValue) {
        int[] hashes = hash(attrValue, depth,width);

        int[] result = new int[depth];
        for (int j = 0; j < depth; j++) {
            int w = hashes[j];
            result[j] = CM[j][w];
            //result.add(new DistinctSample(CM.get(j).get(w)));
        }
        Arrays.sort(result);
        return result[0];
    }

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

    public void reset() {
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                CM[j][i] = 0;
            }
        }
    }


    /*
        Input: 1. Attribute
        Output: 2. Arraylist of size depth with all relevant Distinct samples for single attribute
     */
}