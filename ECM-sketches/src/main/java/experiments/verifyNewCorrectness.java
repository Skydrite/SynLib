package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import dataGeneration.ProcessedStreamLoaderGeneric;
import structure.ExponentialHistogramCircularInt;
import structure.logEventInt;
import structure.slidingCMSketch;
import structure.slidingCMSketch.sliding_window_structures;

public class verifyNewCorrectness {
	static int repeats=1;
	final static boolean debugMsgs=false;
	public enum collection_type { 
		wc98, snmp
	}
	
	static boolean singleLevel=false;
	static int streamDuration;
	static collection_type collection;
	public final static double ratio=1;

	
	static String path=null;

	static float delta=0.15f;
	static float epsilon=0.15f;
	static int windowSize=2000000;
	static int maxNumberOfEvents=windowSize*1000;
	public static int[]readDataset(String path) {
		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);
		psl.reset();
		int cnt=0;
		int[] res = new int[214740010];
		while (true) {
			logEventInt levent = psl.readNextEvent();
			if (levent==null||cnt>214740000) {
				res[cnt++]=-1;
				return res;
			}
			else {
				res[cnt++] = levent.seconds;
				res[cnt++] = levent.file;
				maxtime=levent.seconds;
			}
		}
	}
	static int maxtime=0;


	
	
	public static void withSketch(int[]al, sliding_window_structures slidingWindowType) {
		///// First run once for initializing memory etc
		{
			slidingCMSketch scm = new slidingCMSketch(delta,epsilon, windowSize, maxNumberOfEvents,slidingWindowType, 9999, false);
			int cnt=0;
			while (true) {
				int time=al[cnt++];
				int event=al[cnt++];
				if (time<0)
					break;
				else {
					scm.add(event, time);
				}
			}
		}
		/////
		long time1 = System.currentTimeMillis();
		long numberOfArrivals=0;
		for (int repeat=0;repeat<repeats;repeat++) {
			numberOfArrivals=0;
			slidingCMSketch scm = new slidingCMSketch(delta,epsilon, windowSize, maxNumberOfEvents,slidingWindowType, repeat, false);
			int cnt=0;
			while (true) {
				int time=al[cnt++];
				int event=al[cnt++];
				if (time<0)
					break;
				else {
					scm.add(event, time);
					numberOfArrivals++;
				}
			}
		}
		time1=System.currentTimeMillis()-time1;
		time1/=repeats;
		System.err.println("Epsilon: "+ epsilon + ", Structure: " + slidingWindowType+ " Updating took " + time1 +" msec" + "  and update rate is " + numberOfArrivals/(double)time1*1000d);
	}
	
	public static void withSketchQuery(int[]al, sliding_window_structures slidingWindowType) {
		slidingCMSketch scm = new slidingCMSketch(delta,epsilon, windowSize, maxNumberOfEvents,slidingWindowType, 0, false);
		HashSet<Integer> items = new HashSet<>();
		int cnt=0;
		while (true) {
			int time = al[cnt++];
			int file = al[cnt++];
			if (time<0) break;
			else {
				scm.add(file, time);
				items.add(file);
			}
		}
		int lateTime = maxtime;
		scm.prepareForQuerying();
		int[] queryStarttimes = new int[(int) (Math.log(windowSize) / Math.log(2)) ];
		for (int i = 0; i < queryStarttimes.length; i++) queryStarttimes[i] = (int) (lateTime - Math.pow(2, i+1));

		long time1 = System.currentTimeMillis();
		double numberOfQueries = 0;
		for (int repeat = 0; repeat < repeats; repeat++)
			for (int item : items)
				for (int queryid = 0; queryid < queryStarttimes.length; queryid++) {
					scm.get(item, queryStarttimes[queryid]);
					numberOfQueries++;
				}

		time1=System.currentTimeMillis()-time1;
		System.err.println("Querying with " + slidingWindowType + " took " + time1/repeats + " msec (per repeat) and query rate is " + numberOfQueries / (double) time1 * 1000d);
	}

	public static void withSketchQuery(String path, sliding_window_structures slidingWindowType, long ignoreTime) {
		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);
		slidingCMSketch scm = null;
		int lateTime = 0;
		psl.reset();
		scm = new slidingCMSketch(delta, epsilon, windowSize, maxNumberOfEvents, slidingWindowType, 0, false);
		HashSet<Integer> items = new HashSet<>();
		while (true) {
			logEventInt levent = psl.readNextEvent();
			if (levent == null) {
				break;
			}
			items.add(levent.file);
			scm.add(levent.file, levent.seconds);
			lateTime = levent.seconds;
		}
		scm.prepareForQuerying();

		int[] queryStarttimes = new int[(int) (Math.log(windowSize) / Math.log(2)) ];
		for (int i = 0; i < queryStarttimes.length; i++) queryStarttimes[i] = (int) (lateTime - Math.pow(2, i+1));
		long time1 = System.currentTimeMillis();
		double numberOfQueries = 0;
		for (int repeat = 0; repeat < repeats; repeat++)
			for (int item : items)
				for (int queryid = 0; queryid < queryStarttimes.length; queryid++) {
					scm.get(item, queryStarttimes[queryid]);
					numberOfQueries++;
				}

		time1 = System.currentTimeMillis() - time1 - ignoreTime;
		System.err.println("Querying with " + slidingWindowType + " took " + time1/repeats + " msec (per repeat) and query rate is " + numberOfQueries / (double) time1 * 1000d);
	}

	
	public static void main(String[]args) {
		System.err.println("cOut is " + structure.compositeRandWaveDeque.cOut);
		path=args[0]; 
		int[] al = readDataset(path);
		if (path.contains("ips"))
			windowSize=200000000; // max number of events remains the same
		repeats = Integer. parseInt(args[1]);
		for (String function:new String[]{"update","query"})
			for (String struct:new String[]{"EC", "DW", "RW"}) 
				for (double error:new double[]{0.15}) {
					epsilon=(float)error;
					if (function.equals("update")) {
						if (struct.equals("EC")) {
							System.err.println("\nWith EC "+ path);
							withSketch(al, sliding_window_structures.EC);
						}
						else if (struct.equals("DW")) {
							System.err.println("\nWith DW "+ path);
							withSketch(al, sliding_window_structures.DW);		
						} else {
							System.err.println("\nWith RW " + path);
							withSketch(al, sliding_window_structures.RW);
						}
					} else {	
						if (struct.equals("EC")) {
							System.err.println("\nWith EC "+ path);
							withSketchQuery(al, sliding_window_structures.EC);
						}
						else if (struct.equals("DW")) {
							System.err.println("\nWith DW "+ path);
							withSketchQuery(al, sliding_window_structures.DW);		
						} else {
							System.err.println("\nWith RW "+ path);
							withSketchQuery(al, sliding_window_structures.RW);
						}
					}
				}
	}
}
