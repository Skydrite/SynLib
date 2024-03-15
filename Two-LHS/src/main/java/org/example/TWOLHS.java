package org.example;
import org.example.Main;

import java.util.Random;

public class TWOLHS extends Sample {
    int M;
    int K;
    //long hashDomain;
    // Maintain k count signatures of size logM + 1
    int[][] countSignatures;
    int a;
    int b;
    int c;
    int seed;

    //public int bitSize = 31;
    public TWOLHS(int seed) {
        this.seed = seed;
        this.M = Integer.MAX_VALUE;
        this.K = 2;
        this.hashDomain = (long) Math.pow(M, K);
        initialize();
    }


    public void initialize() {
        // initialize count signature structure
        countSignatures = new int[bitSize][bitSize + 1];
        // initialize count signatures to 0
        for (int i = 0; i < bitSize; i++) {
            for (int j = 0; j < bitSize + 1; j++) {
                countSignatures[i][j] = 0;
            }
        }
        // initialize hash functions
        int[] primes = randomPrimes();
        a = primes[0];
        b = primes[1];
        c = primes[2];
        // constants are rand primes


        // initialize hash tables
    }

    public long hashH(long x) {
        // similar to fm. hash x to [0, M^k] uniformly.
        // hash x to [0, M^k] uniformly
        long hash = ((a * x + b) % c % hashDomain + 1);//(((a * x + b) % c % hashDomain + hashDomain) % hashDomain) + 1;
//        if (hash <= 0) {
//            System.out.println("Hash is negative for x = " + x);
//            throw new RuntimeException("Hash is negative");
//        }
        return hash;
    }


    private boolean[] lsb(long h) {
        boolean[] bits = new boolean[bitSize];
        // determine lsb;
        int lsb = bitSize - 1;
        long bit = 0;
        while (h > 0) {
            bit = h % 2;
            h /= 2;
            bits[lsb] = bit == 1;
            lsb--;
        }
//        if (lsb -1 < 0) {
//            System.out.println("lsb is negative for h = " + h);
//            throw new RuntimeException("lsb is negative");
//        }
        return bits;// lsb - 1; // Find way to get lsb AND to do efficient bitwise ands later on.
    }


    @Override
    public void add(long hx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ingest(long hx, int sign) {
        // get lsb of hx. increment total count by v. increment all 1 .. log M counts by v if bit is 1.
        long h = hashH(hx);
        // bit repr of h:

        int sketch_index = -1;
        for (int i = 0; i < bitSize; i++) {
            int mask = 1 << i;
            long val = (h & mask);
            if (val == 0) {
                continue;
            }
            if (sketch_index == -1) {
                sketch_index = i;
                countSignatures[i][0] += sign;
            }
            countSignatures[sketch_index][i + 1] += sign;
        }
//
//
//        boolean[] bits = lsb(h);
//        int lsb = 0; // first bit in bits that is 1.
//        for (int i = bits.length - 1; i > 0; i--) {
//            if (bits[i]) {
//                lsb = i;
//                break;
//            }
//        }
//        int v = 1;
//        countSignatures[lsb][0] += v;
//        // Add v to every bit.
//
//        for (int i = 0; i < bits.length; i++) {
//            if (bits[i]) {
//                countSignatures[lsb][i + 1] += v;
//            }
//        }
    }

    @Override
    public void ingest(long[] vals, int sign) {
        // get lsb of hx. increment total count by v. increment all 1 .. log M counts by v if bit is 1.
        //long h = hashH(hx);
        // bit repr of h:

        int sketch_index = -1;
        for (int i = 0; i < bitSize; i++) {
//            int mask = 1 << i;
//            long val = (h & mask);
            if (vals[i] == 0) {
                continue;
            }
            if (sketch_index == -1) {
                sketch_index = i;
                countSignatures[i][0] += sign;
            }
            countSignatures[sketch_index][i + 1] += sign;
        }
//
//
//        boolean[] bits = lsb(h);
//        int lsb = 0; // first bit in bits that is 1.
//        for (int i = bits.length - 1; i > 0; i--) {
//            if (bits[i]) {
//                lsb = i;
//                break;
//            }
//        }
//        int v = 1;
//        countSignatures[lsb][0] += v;
//        // Add v to every bit.
//
//        for (int i = 0; i < bits.length; i++) {
//            if (bits[i]) {
//                countSignatures[lsb][i + 1] += v;
//            }
//        }
    }
                //
                //    public void add(long hx, int v) {
                //        // get lsb of hx. increment total count by v. increment all 1 .. log M counts by v if bit is 1.
                //        long h = hashH(hx);
                //        boolean[] bits = lsb(h);
                //        int lsb = 0; // first bit in bits that is 1.
                //        for (int i = 0; i < bits.length; i++) {
                //            if (bits[bits.length - i - 1]) {
                //                lsb = i;
                //                break;
                //            }
                //        }
                //        countSignatures[lsb][0] += v;
                //        for (int i = 1; i < countSignatures[lsb].length; i++) {
                //            if ((h & (1L << (i - 1))) != 0) {
                //                countSignatures[lsb][i] += v;
                //            }
                //        }
                //    }

    public boolean emptyBucket(int lsb) {
        // check if bucket is empty
        return countSignatures[lsb][0] == 0;
    }

    public boolean singletonBucket(int lsb) {
        // check if bucket is singleton
        if (emptyBucket(lsb)) {
            return false;
        }
        for (int i = 1; i < countSignatures[lsb].length; i++) {
            if (countSignatures[lsb][i] > 0 && countSignatures[lsb][i] < countSignatures[lsb][0]) {
                return false;
            }
        }
        return true;
    }



    @Override
    public void reset() {

    }



    private int[] randomPrimes() {
        // generate 3 random primes
        int[] primes = new int[3];
        Random rand = new Random(Main.repetition + seed);
        for (int i = 0; i < 3; i++) {
            int prime = 0;
            int cnt = 0;
            while (!Hash.isPrime(prime) && cnt < 1000) {
                prime = rand.nextInt();
                cnt++;
            }
            primes[i] = prime;
        }

        return primes;
    }



    @Override
    public long getMemoryUsage() {
        return ((long) bitSize * (bitSize + 1) * 32);
    }
}
