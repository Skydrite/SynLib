package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import dataGeneration.ProcessedStreamLoaderGeneric;
import dataGeneration.StreamConstructor;
import structure.Event;
import structure.EventHD;
import structure.ExponentialHistogramCircularInt;
import structure.Stream;
import structure.StreamHD;
import structure.compositeRandWaveDeque;
import structure.logEventInt;
import structure.randWaveDeque;
import structure.slidingCMSketch;
import structure.slidingCMSketch.sliding_window_structures;

public class verifyCorrectnessOfEH {
	public static int repeats = -1;
	final static boolean debugMsgs = false;

	public enum collection_type {
		wc98, snmp
	}

	static boolean singleLevel = false;
	static int streamDuration;
	static collection_type collection;
	public final static double ratio = 1;

	static String path = null;
	static double defaultDelta = 0.15;

	public static void main(String[] args) {
		Random rn = new Random(123);
		double epsilon=0.01;
		int windowSize=100000;
		int numberOfQueries=100;
		ExponentialHistogramCircularInt eh = new ExponentialHistogramCircularInt(epsilon, windowSize, -1);
		int[] queryStartTimes = new int[numberOfQueries];
		int[] queryAnswers = new int[numberOfQueries];
		ArrayList<Integer> al = new ArrayList<Integer>();
		for (int i=0;i<numberOfQueries;i++) al.add(rn.nextInt(windowSize)); 
		Collections.sort(al);
		for (int i=0;i<numberOfQueries;i++) queryStartTimes[i]=al.get(i);
		queryStartTimes[queryStartTimes.length-1] = windowSize-10;
		int currentAnswerLocation=0;
		StreamConstructor sc = new StreamConstructor(0, windowSize);
		Stream str = sc.constructUniformStream(windowSize);
		StreamHD shd = sc.constructHighDimensionalStream(str, 2); // rtue and false
		EventHD[] evs = shd.getEvents();
		for (int i = 0;i<evs.length;i++) {
			if (evs[i].event==1) {
				System.err.println(i);
				eh.addAOne(i);
				if (currentAnswerLocation+1<queryStartTimes.length && queryStartTimes[currentAnswerLocation+1]<=i) currentAnswerLocation++;
				if (queryStartTimes[currentAnswerLocation]<=i) queryAnswers[currentAnswerLocation]++;
			}
		}
		for (int i=queryStartTimes.length-1;i>0;i--) 
			queryAnswers[i-1]+=queryAnswers[i];

		double relErr=0;
		for (int i=queryStartTimes.length-1;i>=0;i--) {
			double est = eh.getEstimationRealtime(queryStartTimes[i]);
			double err = Math.abs(est-queryAnswers[i]);
			relErr = Math.max(relErr,err/Math.max(1,queryAnswers[i]));
			System.err.println("Max " + queryStartTimes[i] +  " " + relErr);
		}
		System.err.println(relErr);
	}

}
