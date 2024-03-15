package org.example;

import java.util.Random;

public class Hash {

    public static boolean isPrime(int prime) {
        if (prime <= 1) {
            return false;
        }
        if (prime <= 3) {
            return true;
        }
        if (prime % 2 == 0 || prime % 3 == 0) {
            return false;
        }
        if (prime % 5 == 0 || prime % 7 == 0) {
            return false;
        }
        if (prime % 11 == 0 || prime % 13 == 0) {
            return false;
        }
        if (prime % 17 == 0 || prime % 19 == 0) {
            return false;
        } if (prime % 23 == 0 || prime % 29 == 0) {
            return false;
        } if (prime % 31 == 0 || prime % 37 == 0) {
            return false;
        } if (prime % 41 == 0 || prime % 43 == 0) {
            return false;
        } if (prime % 47 == 0 || prime % 53 == 0) {
            return false;
        } if (prime % 59 == 0 || prime % 61 == 0) {
            return false;
        } if (prime % 67 == 0 || prime % 71 == 0) {
            return false;
        } if (prime % 73 == 0 || prime % 79 == 0) {
            return false;
        } if (prime % 83 == 0 || prime % 89 == 0) {
            return false;
        } if (prime % 97 == 0 || prime % 101 == 0) {
            return false;
        } if (prime % 103 == 0 || prime % 107 == 0) {
            return false;
        } if (prime % 109 == 0 || prime % 113 == 0) {
            return false;
        } if (prime % 127 == 0 || prime % 131 == 0) {
            return false;
        } if (prime % 137 == 0 || prime % 139 == 0) {
            return false;
        } if (prime % 149 == 0 || prime % 151 == 0) {
            return false;
        } if (prime % 157 == 0 || prime % 163 == 0) {
            return false;
        } if (prime % 167 == 0 || prime % 173 == 0) {
            return false;
        } if (prime % 179 == 0 || prime % 181 == 0) {
            return false;
        } if (prime % 191 == 0 || prime % 193 == 0) {
            return false;
        } if (prime % 197 == 0 || prime % 199 == 0) {
            return false;
        } if (prime % 211 == 0 || prime % 223 == 0) {
            return false;
        } else {
            return true;
        }
    }



    public static int[][] randomPrimes(int depth, int attr) {
        // generate 3 random primes
        int[][] primes = new int[3][depth];
        Random rand = new Random(Main.repetition + attr);
        for (int j = 0; j < depth; j++) {
            int prime = 0;
            int cnt = 0;
            while (!Hash.isPrime(prime) && cnt < 1000) {
                prime = rand.nextInt();
                cnt++;
            }
            primes[2][j] = prime;
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < depth; j++) {
                int constant = 0;
                constant = rand.nextInt(primes[2][j]);
                primes[i][j] = constant;
            }
        }
        return primes;
    }
}
