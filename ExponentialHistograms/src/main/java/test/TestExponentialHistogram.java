package test;

import dataGeneration.StreamConstructor;
import structure.Event;
import structure.ExponentialHistogramCircularInt;
import structure.Stream;

import java.util.Random;

public class TestExponentialHistogram {

    static void testSingle() {
        int repeats=10;
        int delta=0;
        int maxNumberOfEvents=10000;
        Random rn = new Random(1234);
        int windowSize=0;

        StreamConstructor sc = new StreamConstructor(rn.nextInt());

        double maxerror=0;
        for (float epsilon=0.025f;epsilon<0.21;epsilon+=0.025) {
            for (int repeat=0;repeat<repeats;repeat++) {
                int numberOfEvents = Math.max(1000, rn.nextInt(maxNumberOfEvents));
                Stream stream = sc.constructUniformStream(numberOfEvents);// sc.constructPoissonStream(numberOfEvents, rn.nextInt(10000));
                windowSize = Math.max(windowSize, stream.getCurrentTime());
                ExponentialHistogramCircularInt dw = new ExponentialHistogramCircularInt(epsilon, windowSize, maxNumberOfEvents);
//				ExponentialHistogramDeque dw2 = new ExponentialHistogramDeque(epsilon, windowSize, maxNumberOfEvents);
                dw.batchUpdate(stream);
//				dw2.batchUpdate(stream);
                // execute queries now
                Event[] events = stream.getEvents();
                final int numberOfQueries = events.length;
                int[] queryTimes = new int[numberOfQueries];
                int j=numberOfQueries;
                for (Event e:events) {
                    switch(delta) {
                        case 0:
                            queryTimes[j-1]=e.getTime();
                            break;
                        case 1:
                            queryTimes[j-1]=e.getTime()+1;
                            break;
                        case -1:
                            queryTimes[j-1]=e.getTime()-1;
                            break;
                    }
                    j--;
                }


                int[] accurateAnswers = new int[numberOfQueries]; // accurateAnswers is usually like 1,2,3,4,5... but in case of
                // multiple events at the same timestamp, this may not be the case. So we recompute it
                for (int cnt = 0; cnt < numberOfQueries; cnt++) {
                    int i = events.length;
                    int qt = queryTimes[cnt];
                    while (i > 0 && events[i - 1].comesAtOrAfter(qt)) {
                        if (events[i - 1].getEvent())
                            accurateAnswers[cnt]++;
                        i--;
                    }
                }
                double maxError = 0;
                //System.err.println(dw);
                // i have the ground truth, now execute the queries
                for (int cnt = 0; cnt < numberOfQueries; cnt++) {
                    int qt = queryTimes[cnt];
//					System.err.println("Accurate answer is " + accurateAnswers[cnt]);
                    double est = dw.getEstimationRealtime(qt);
//					double est2 = dw2.getEstimationRealtime(qt);
//					if (Math.abs(est-est2)>0.001)
//						System.err.println("Problem here");
                    double err = Math.abs(est - accurateAnswers[cnt]) / accurateAnswers[cnt];
//					if (err>0) {
//						System.err.println("Repeat " + err);
//						dw.getEstimationRealtime(qt);
//					}
                    if (accurateAnswers[cnt] == 0)
                        err = Math.abs(est - accurateAnswers[cnt]);
                    if (err > epsilon) {
                        System.err.print("\n\nQuery " + qt + " Estimated "
                                + est + " Real " + accurateAnswers[cnt]
                                + " Err " + err);
                        System.err.print("   epsilon " + epsilon
                                + " maxNumberOfEvents " + maxNumberOfEvents);

                        est = dw.getEstimationRealtime(qt);
                    }
                    maxError = Math.max(err, maxError);
                }
                maxerror=Math.max(maxerror, maxError);
            }
            System.err.println("Maximum error is " + maxerror + " and allowed is "+ epsilon);
        }
    }

    public static void main(String[]args) {
        Random rn = new Random(123);
        int queryLength=10;
        double epsilon=0.5;
        ExponentialHistogramCircularInt ex = new ExponentialHistogramCircularInt(epsilon, queryLength, 1000);
        boolean[] events = new boolean[100];
        for (int i=0;i<100;i++) {
            if (rn.nextBoolean()) {
                events[i] = true;
                ex.addAOne(i);
            }
            int realAnswer=0;
            if (i>queryLength) {
                for (int j=i;j>=i-queryLength;j--) if (events[j]) realAnswer++;
                System.err.print("Time " + i + "  startTime  "+ (i-queryLength) + " Real answer " + realAnswer);
                System.err.println("  I=" + i + " " + ex.getEstimationRealtimeWithExpiryTime(i-queryLength, queryLength) + " | " + ex);
            }
        }
    }

    public static void main2(String[]args) {
        long time1=System.currentTimeMillis();
        Random rn = new Random(123);
        int queryLength=5;
        double epsilon=0.1;
//		ExponentialHistogramDeque ex = new ExponentialHistogramDeque(epsilon, queryLength, 1000);
        ExponentialHistogramCircularInt ex = new ExponentialHistogramCircularInt(epsilon, queryLength, 1000);
        boolean[] events = new boolean[1000000];
        for (int i=0;i<events.length;i++) {
            if (rn.nextBoolean()) {
                events[i] = true;
                ex.addAOne(i);
            }
            double ans=0;
            for (int c=i;c>=i-queryLength;c--) {
                if (c<0) break;
                if (events[c]) ans++;
            }
            double v = ex.getEstimationRealtime(i-queryLength);
            if ((Math.abs(v-ans)/ans)>epsilon) {
                System.err.println("Check " + (Math.abs(v-ans)/ans) + " at round " + i);
                System.err.println(ex);
                v = ex.getEstimationRealtime(i-queryLength+1);
            }
        }
//		ex.getEstimationRealtimeWithExpiration(10000-100);
        long time2 = System.currentTimeMillis()-time1;
        System.err.println("Total time: " + (time2)/1000);
    }

}
