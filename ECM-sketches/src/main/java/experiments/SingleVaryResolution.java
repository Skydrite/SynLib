package experiments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import structure.logEventInt;
import structure.slidingCMSketch;
import structure.slidingCMSketch.sliding_window_structures;
import dataGeneration.ProcessedStreamLoaderGeneric;


public class SingleVaryResolution {
//	final static boolean debugMsgs=false;
//	public enum collection_type {
//		wc98, snmp
//	}
//	
//	static boolean singleLevel=false;
//	static int streamDuration;
//	static collection_type collection;
//	public final static double ratio=1;
//
//	static sliding_window_structures slidingWindowType = null;
//	
//	public static void main(String[]args) {
//		// first find the time difference
//		String path = args[0];
//		int SWtype = Integer.parseInt(args[1]);
//		int repeats = Integer.parseInt(args[2]);
//		double delta = Double.parseDouble(args[3]);
//		double epsilon = Double.parseDouble(args[4]);
//		
//		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);
//		int start,stop;
//		start = psl.getStart();
//		stop = psl.getStop();
//
//		streamDuration=1000000;//864000; // 10 days
//		long maxEvents=(long)(streamDuration*1000d*ratio); // one event per msec on average
//		System.err.println("Sliding window length is " + streamDuration +  " seconds and maxEvents in SW is " + maxEvents);
//		System.err.println("Repeats: " + repeats + " and memory results printed in Kbytes");
//		long time1 = System.currentTimeMillis();
//		repeatTest(repeats, maxEvents, streamDuration, path, start, stop, SWtype, delta,epsilon, collection);
//		time1=System.currentTimeMillis()-time1;
//		System.err.println("Execution took " + time1/1000 +" seconds");
//		System.err.println("Sample execution times are SNMP: 2 seconds  WC-ALL:  260 seconds  ");
//	}
//	
//	static void repeatTest(int repetitions, long maxEvents, int windowSizeInt, String path, int start, int stop, int type, double delta, double epsilon, collection_type collection) {
//		String strSlidingWindowType = "";
//		switch (type) {
//		case 0:
//			slidingWindowType = sliding_window_structures.EC;
//			strSlidingWindowType = "ExpHist-EC";
//			break;
//		case 1:
//			slidingWindowType = sliding_window_structures.DW;
//			strSlidingWindowType = "DetWave-DW";
//			break;
//		case 2:
//			slidingWindowType = sliding_window_structures.RW;
//			strSlidingWindowType = "RandWave-RW";
//			break;
//		}
//		boolean useOriginalAssignmentToStreams = false;
//
//		int numberOfStreams = 1;
//
//		// now compute
//		int siblings = 1;
//		int height = 1;
//		double maxRelativeError = 0;
//		double avgRelativeError = 0;
//		double avgMemory = 0;
//		double avgNetwork = 0;
//		double avgAbsError = 0;
//		double maxAbsError = 0;
//		double ratioOfSeriousErrors = 0;
//		double totalEvents = 0;
//		double avgInnerProductRelativeError = 0;
//		double maxInnerProductRelativeError = 0;
//		double avgRealRelError = 0, avgInnerProductRealRelError = 0, maxRealRelError = 0, maxInnerProductRealRelError = 0;
//		for (int i = 0; i < repetitions; i++) {
//			double[] results = testMerging(numberOfStreams, useOriginalAssignmentToStreams, maxEvents, windowSizeInt, siblings, height, delta, epsilon, path, start, stop, i, collection);
//			double absoluteError = results[0];
//			double maxAbsoluteError = results[1];
//			double singleExecutionMaxRelErr = results[2];
//			double singleExecutionAvgRelErr = results[3];
//			double singleRatioOfSeriousErrors = results[4];
//			double requiredMemory = results[5];
//			double requiredNetwork = results[6];
//			double totalReadEvents = results[7];
//			avgInnerProductRelativeError += results[8];
//			maxInnerProductRelativeError = Math.max(results[9], maxInnerProductRelativeError);
//
//			avgRealRelError += results[10];
//			avgInnerProductRealRelError += results[11];
//			maxRealRelError = Math.max(maxRealRelError, results[12]);
//			maxInnerProductRealRelError = Math.max(maxInnerProductRealRelError, results[13]);
//
//			avgAbsError += absoluteError;
//			maxAbsError = Math.max(maxAbsError, maxAbsoluteError);
//
//			maxRelativeError = Math.max(maxRelativeError, singleExecutionMaxRelErr);
//			avgRelativeError += singleExecutionAvgRelErr;
//
//			ratioOfSeriousErrors += singleRatioOfSeriousErrors;
//			avgMemory += requiredMemory;
//			avgNetwork += requiredNetwork;
//			totalEvents += totalReadEvents;
//		}
//		results r = new results(path, strSlidingWindowType, epsilon, delta, numberOfStreams, siblings, height, (int)(totalEvents/(double) repetitions), avgRelativeError / (double) repetitions, maxRelativeError, 
//				ratioOfSeriousErrors/ (double) repetitions, avgInnerProductRelativeError / (double) repetitions, maxInnerProductRelativeError, avgMemory / (double) repetitions, avgNetwork/ (double) repetitions, 
//				avgAbsError / (double) repetitions, maxAbsError, avgRealRelError / (double) repetitions, 
//				avgInnerProductRealRelError / (double) repetitions, maxRealRelError, maxInnerProductRealRelError);
//		System.err.println("|||" + r.printConfiguration(true) + "," + r.printResults(true) + "," + r.printDetailedResults(true));
//		System.err.println("<<<" + r.printConfiguration(false) + "," + r.printResults(false));
//	}
//	
//
//	public static double computeAllowedError(int height, double epsilon, sliding_window_structures slidingWindowType) {
//		double epsilonsw,epsiloncm;
//		epsilonsw = slidingCMSketch.getEpsilonSW(slidingWindowType, epsilon);
//		epsiloncm = slidingCMSketch.getEpsilonCM(slidingWindowType, epsilon);
//
//		double newEpsilon=0;
//		switch (slidingWindowType) {
//		case EC:
//		case DW:
//			double newEpsilonSW=height*epsilonsw*(1d+epsilonsw)+epsilonsw;
//			newEpsilon=epsiloncm+newEpsilonSW+epsiloncm*newEpsilonSW;
//			break;
//		case RW:
//			newEpsilon=epsilon;
//			break;
//		}
//		return newEpsilon;
//	}
//		
//	static double[] testMerging(int numberOfRandWaves, boolean useOriginalAssignmentToStreams, long maxNumberOfEvents, 
//			int windowSizeInt, int siblings, int height, double delta, double epsilon, String path, long startTime, 
//			long endTime, int repeat, collection_type collection) {
//		System.err.println("Repeat " + repeat);
//		int numberOfSeriousErrors=0;
//		double requiredMemory=0;
//		double requiredNetwork=0;
//		int windowSize=windowSizeInt;
//		slidingCMSketch[] scm = new slidingCMSketch[numberOfRandWaves];
//		for (int cnt=0;cnt<scm.length;cnt++) {
//			scm[cnt]=new slidingCMSketch(delta,epsilon, windowSize, maxNumberOfEvents,slidingWindowType, repeat);
//		}
//		double allowedError=computeAllowedError(height, epsilon, slidingWindowType);
//		
//		// now read stream
//		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);
//
//		int numberOfStreams=numberOfRandWaves;
//		if (useOriginalAssignmentToStreams) {
//			numberOfStreams=-1;
//		}
//
//
//		long duration=windowSizeInt;//startTime;
//		int numberOfQueries = (int)Math.ceil(Math.log(duration)/Math.log(10))-2; // remove queries for time=10 and time=100
//
//		int[] queryStartTimes=new int[numberOfQueries];
//		for (int cnt=1;cnt<=numberOfQueries;cnt++) {
//			queryStartTimes[cnt-1]=(int) (Math.max(0,endTime-Math.pow(10,cnt+2)));
//		}
//		int currentQueryId=numberOfQueries-1;
//		int currentQueryStart=queryStartTimes[currentQueryId];
//		int nextQueryStart=queryStartTimes[currentQueryId-1];
//		HashMap<Integer, Integer> frequencies[] = new HashMap[numberOfQueries];
//		for (int cnt=0;cnt<numberOfQueries;cnt++)frequencies[cnt]=new HashMap<Integer, Integer>(10000);
//
//		while (true) {
//			logEventInt levent = psl.readNextEvent(numberOfStreams);
//
//			if (levent==null) 
//				break;
//			scm[levent.streamid].add(levent.file, levent.seconds);
//			if (levent.seconds<currentQueryStart) 
//				continue;
//			else {
//				if (levent.seconds>=nextQueryStart) {
//					currentQueryId--;
//					if (currentQueryId<0) 
//						break; // i executed all queries
//					else {
//						currentQueryStart=queryStartTimes[currentQueryId];
//						if (currentQueryId>0)
//							nextQueryStart=queryStartTimes[currentQueryId-1];
//						else 
//							nextQueryStart=Integer.MAX_VALUE;
//					}
//				}
//				Integer val = frequencies[currentQueryId].get(levent.file);
//				if (val!=null)
//					frequencies[currentQueryId].put(levent.file, val+1);
//				else
//					frequencies[currentQueryId].put(levent.file, 1);
//			}
//		}
//		// now average the required memory (NOT THE NETWORK, JUST MEMORY)
//		for (slidingCMSketch singleSketch:scm) {
//			requiredMemory+=singleSketch.getRequiredMemory(); // in kilobytes: TO SEND TO PARENTS
//		}
//		if (debugMsgs)
//			System.err.println("Now preparing for queries...");
//
//		for (slidingCMSketch singleSketch:scm) {
//			singleSketch.prepareForQuerying();
//		}
//		
//		if (debugMsgs)
//			System.err.println("Now starting the merging...");
//
//		// Form the hierarchy
//		ArrayList<slidingCMSketch> remaining = new ArrayList<slidingCMSketch>(scm.length);
//		for (slidingCMSketch singleSketch:scm) {
//			remaining.add(singleSketch);
//		}
//		for (int i=0;i<scm.length;i++) scm[i]=null;
//		ArrayList<slidingCMSketch> remainingResults = new ArrayList<slidingCMSketch>(scm.length/siblings);
//		do {
//			slidingCMSketch[] workingSet=new slidingCMSketch[Math.min(siblings, remaining.size())];
//			
//			for (int cnt=0;cnt<remaining.size();cnt++) {
//				workingSet[cnt%siblings]=remaining.get(cnt);
//				if ((cnt+1)%siblings==0||(cnt+1==remaining.size())) { // merge
//					for (slidingCMSketch singleSketch:workingSet) { 
//						requiredNetwork+=singleSketch.getRequiredNetwork();
//					}
//
//					slidingCMSketch merged = slidingCMSketch.mergeSlidingCMSketches(workingSet, delta, epsilon, windowSize, repeat);
//					merged.prepareForQuerying();
//					remainingResults.add(merged);
//					for (int i=0;i<workingSet.length;i++)workingSet[i]=null;
//					workingSet=new slidingCMSketch[Math.min(siblings, remaining.size()-cnt-1)];
//				}
//			}
//			remaining=remainingResults;
//			remainingResults= new ArrayList<slidingCMSketch>(remaining.size()/siblings);
//		} while (remaining.size()>1);
//
//		slidingCMSketch merged=remaining.get(0);
//		remaining=null;
//		merged.cloneForQuerying(); // i do not need to call prepare, because the sliding windows are already full. Only clone is needed!
//		// now execute queries 
//		HashMap<Integer, Integer> totalAnswers=new HashMap<Integer, Integer>(frequencies[numberOfQueries-1].size()*2);
//		double numberOfEvents=0;
//		double maxRelError=0;
//		double avgRelError=0;
//		double avgAbsError=0;
//		double maxAbsError=0;
//		double maxRelErrorIP=0;
//		double avgRelErrorIP=0;
//		double avgTrueRelError=0,avgTrueRelErrorIP=0,TrueMaxRelError=0,TrueMaxRelErrorIP=0;
//		int queries = 0;
//		double totalReadEvents=0;
//		// print first line of merged
//		if (debugMsgs)
//			System.err.println("Now starting the queries...");
//		for (int queryId=0;queryId<numberOfQueries;queryId++) {
//			int queryTime=queryStartTimes[queryId];
//			int queryTimeT=queryTime;
//			// add answers of sliding window in totalAnswers
//			for (Entry<Integer,Integer> entry:frequencies[queryId].entrySet()) {
//				// first is file, second is frequency
//				numberOfEvents+=entry.getValue();
//				Integer val = totalAnswers.get(entry.getKey());
//				if (val==null) totalAnswers.put(entry.getKey(),entry.getValue()); else totalAnswers.put(entry.getKey(), entry.getValue()+val);
//			}
//
//			for (Entry<Integer,Integer> entry:totalAnswers.entrySet()) {
//				// first is file, second is frequency
//				Integer val = entry.getValue();
//				// now execute the query to the slidingwindow cm sketch
//				double estimatedVal = merged.get(entry.getKey(), queryTimeT);
//				double diff = Math.abs(val-estimatedVal);
//				avgAbsError+=diff;
//				maxAbsError=Math.max(diff,maxAbsError);
//				double relativeError=diff/(double)numberOfEvents;
//				double TrueRelativeError=diff/val;
//				avgTrueRelError+=TrueRelativeError;
//				TrueMaxRelError=Math.max(TrueMaxRelError, avgTrueRelError);
//				if (relativeError>allowedError)numberOfSeriousErrors++;
//				queries++;
//				maxRelError=Math.max(relativeError,maxRelError);
//				avgRelError+=relativeError;
//			}
//			totalReadEvents=Math.max(totalReadEvents,numberOfEvents);
//			
//			// now compute inner join
//			double innerproduct=0;
//			for (Entry<Integer,Integer> entry:totalAnswers.entrySet()) {
//				// first is file, second is frequency
//				double val=entry.getValue();
//				innerproduct+=Math.pow(val, 2);
//			}
//			// and estimate inner join
//			double estInnerProduct = merged.getInnerProduct(queryTimeT);
//			double diffIP = Math.abs(estInnerProduct-innerproduct);
//			double relDiff=diffIP/(numberOfEvents*numberOfEvents);
//			double trueRelDiff=diffIP/estInnerProduct;
//			avgTrueRelErrorIP+=trueRelDiff;
//			TrueMaxRelErrorIP = Math.max(TrueMaxRelErrorIP,trueRelDiff);
//			if (numberOfEvents==0) relDiff=0;
//			avgRelErrorIP+=relDiff;
//			maxRelErrorIP=Math.max(maxRelErrorIP,relDiff);
//		}
//		avgRelError=avgRelError/(double)queries;
//		double[]results=new double[14];
//		results[0] = avgAbsError/(double)queries;
//		results[1] = maxAbsError;
//		results[2] = maxRelError;
//		results[3] = avgRelError;
//		results[4] = numberOfSeriousErrors/(double)queries;
//		results[5] = requiredMemory; // in kilobytes
//		results[6] = requiredNetwork;
//		results[7] = totalReadEvents;
//		results[8] = avgRelErrorIP/(double)numberOfQueries;
//		results[9] = maxRelErrorIP;
//		results[10]=avgTrueRelError/(double)numberOfQueries;
//		results[11]=avgTrueRelErrorIP/(double)numberOfQueries;
//		results[12]=TrueMaxRelError;
//		results[13]=TrueMaxRelErrorIP;
//		return results;
//	}
}
