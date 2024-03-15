package org.example;

import java.util.ArrayList;

import static java.lang.Math.*;
import static java.lang.Math.ceil;

public class TWOLHSStruct {
    int numRepetitions;
    TWOLHS[][] sketch;
    double epsilon;
    boolean twoLHSFast = true;
    public TWOLHSStruct(int numRepetitions, double epsilon) {
        this.epsilon = epsilon;
        this.numRepetitions = numRepetitions;
        sketch = new TWOLHS[2][];
        for (int i = 0; i < 2; i++) {
            sketch[i] = new TWOLHS[numRepetitions];
            for (int j = 0; j < numRepetitions; j++) {
                sketch[i][j] = new TWOLHS(j);
            }
        }
    }

    public int query() {
        return queryEstAcrossRowsTwoLHS(sketch);
    }

    public void add(int value, int lhsIndex) {
        for (int j = 0; j < numRepetitions; j++) {
            sketch[lhsIndex][j].add(value);
        }
    }


    private int queryEstAcrossRowsTwoLHS(TWOLHS[][] samples) {

        double u = setUnionEstimator(samples, epsilon/3); // Est of union size.
        //System.out.println("u: " + u + ", unionSize: " + unionSize + " diff: " + (u - unionSize));
        return setIntersectEstimator(samples, u, epsilon/3);
    }

    private int setUnionEstimator(TWOLHS[][] samples, double eps) {
        double f =(1 + eps) * numRepetitions / 8;
        int index =0;
        int count;
        while(true) {
            count = 0;
            boolean incrCount = false;
            for (int i = 0; i < numRepetitions; i++) {
                incrCount = false;
                for (TWOLHS[] sample : samples) {
                    if (!sample[i].emptyBucket(index)) {
                        incrCount = true;
                        break;
                    }
                }
                if (incrCount) {
                    count++;
                }
            }
            if (count <= f) {
                break;
            } else {
                index++;
            }
        }
        double phat = (double) (count + 1) /(numRepetitions + 1);//samples.length;
        double R = pow(2, index + 1);
        double S = (log(1 - phat)/log(2)) / (log(1 - 1 / R)/log(2));
        if (twoLHSFast) {
            return (int) ceil(S) * numRepetitions;
        } else {
            return (int) ceil(S);
        }
    }

    private int atomicDiffEstimator(TWOLHS[][] samples, double u, double eps, int repetition) {
        double beta=1.5;
        int index = max((int) ceil(log((beta * u)/(1-eps))/log(2)), 0);
        // SingletonUnionBucket with multiple samples.
        if (notSingletonUnionBucket(samples, repetition, index)) {
            return -1;
        }
        // if all are singletons, found witness
        for (TWOLHS[] sample : samples) {
            if (!sample[repetition].singletonBucket(index)) {
                return 0; // no witness found
            }
        }
        return 1; // witness found of intersection.
    }

    private int bucketDiffEstimator(TWOLHS[][] samples, double unionEstimate, double eps, int repetition) {
        int index;
        if (twoLHSFast) {
            index = (int) ceil(log((2 * unionEstimate) / (numRepetitions * pow((1 - eps), 2))) / log(2));
        } else {
            double Beta = 1.5;
            index =(int) ceil(log((Beta * unionEstimate) / (1 - eps)) / log(2));

        }
        if (index < 0) {
            throw new IllegalArgumentException("Index <= 0");
        }
        // SingletonUnionBucket with multiple samples.
        if (notSingletonUnionBucket(samples, repetition, index)) {
            return -1;
        }
        // if all are singletons, found witness
        for (TWOLHS[] sample : samples) {
            if (!sample[repetition].singletonBucket(index)) {
                return 0; // no witness found
            }
        }
        return 1; // witness found of intersection.
    }

    private boolean notSingletonUnionBucket(TWOLHS[][] samples, int repetition, int lsb) {
        ArrayList<TWOLHS> singletonElements = new ArrayList<>();
        for (TWOLHS[] sample : samples) {
            if (sample[repetition].singletonBucket(lsb)) {
                singletonElements.add(sample[repetition]);
            } else if (!sample[repetition].emptyBucket(lsb)) {
                return true;
            }
        }
        // Now check if all singleton elements are identical.
        if (!singletonElements.isEmpty()) {
            TWOLHS[] singletons =  new TWOLHS[singletonElements.size()];
            for (int i=0;i<singletonElements.size();i++){
                singletons[i] = singletonElements.get(i);
            }
            return !identicalSingletonBucket(singletons, lsb);
        } else {
            return true;
        }
    }

    private boolean identicalSingletonBucket(TWOLHS[] samples, int lsb) {
        for (TWOLHS sample : samples) {
            if (!sample.singletonBucket(lsb)) {
                return false;
            }
        }
        int j = 1;
        while (j < samples[0].countSignatures[lsb].length) {
            // Check if no sample has a count of 0 and the other has a count > 0.
            for (int i = 0; i < samples.length - 1; i++) {
                for (int k = i + 1; k < samples.length; k++) {
                    if ((samples[i].countSignatures[lsb][j] > 0) != (samples[k].countSignatures[lsb][j] > 0)) { // true if they contain same sinleton element.
                        // if one is 0 and the other is not, they are not identical.
                        // They do not have to have the same count.
                        return false;
                    }
                }
            }
            j++;
        }
        return true;
    }

    private int setIntersectEstimator(TWOLHS[][] samples, double unionEstimate, double eps) {
        int sum = 0;
        int count = 0;
        for (int i = 0; i < numRepetitions; i++) {
            int diff = bucketDiffEstimator(samples, unionEstimate, eps, i); // atomicDiffEstimator if section 3 of paper.
            if (diff != -1) {
                sum += diff;
                count++;
            }
        }
        double result;
        result = ceil(((double) sum / count) * unionEstimate);
        int resultInt = (int) result;
        if (resultInt < 0) {
            System.out.println("Error: result > unionSize");
            System.exit(1);
        }

        return resultInt;
    }
}
