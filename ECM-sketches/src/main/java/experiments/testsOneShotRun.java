package experiments;

import structure.slidingCMSketch.sliding_window_structures;

public class testsOneShotRun {
	public static int repeats=-1;
	final static boolean debugMsgs=false;
	public enum collection_type { 
		wc98, snmp
	}
	
	static boolean singleLevel=false;
	static int streamDuration;
	static collection_type collection;
	public final static double ratio=1;

	
	static String path=null;
	static double defaultDelta=0.15;

	public static void main(String[]args) {
		repeats = Integer.parseInt(args[13]);
		System.err.println("Repetitions: " + repeats);
		for (boolean optimizeForSJ:new boolean[]{false,true}) {
			//swtype: 0-EC, 1-DW, 2-RW
			// first find the time difference
			
			String s = "";
			
			for (String ss:args) s = s + ss + " ";
			s=s.trim();
			System.err.println("java -cp ContinuousEH.jar -Xmx3g experiments.testsOneShotRun " + s);
			configuration c = configuration.fromString(s);
			path = c.path;
	
			{
				if (optimizeForSJ &&c.slidingWindowType==sliding_window_structures.RW) continue;
				
				double AbsoluteError = 0;
				double MaxAbsoluteError = 0;
				double MaxRelativeError = 0;
				double AvgRelativeError = 0;
				double RatioOfSeriousErrors = 0;
				double RequiredMemory = 0;
				double RequiredNetwork = 0;
				double TotalEvents = 0;
				double AvgInnerProductRelativeError = 0;
				double MaxInnerProductRelativeError = 0;
				double AvgRealRelErr = 0;
				double AvgInnerProductRealRelError = 0;
				double MaxRealRelErr = 0;
				double MaxInnerProductRealRelError = 0;

//				double maxRelativeError = 0;
//				double avgRelativeError = 0;
//				double avgMemory = 0;
//				double avgNetwork = 0;
//				double avgAbsError = 0;
//				double maxAbsError = 0;
//				double ratioOfSeriousErrors = 0;
//				double totalEvents = 0;
//				double avgInnerProductRelativeError = 0;
//				double maxInnerProductRelativeError = 0;
//				double avgRealRelError = 0, avgInnerProductRealRelError = 0, maxRealRelError = 0, maxInnerProductRealRelError = 0;
	
				long time1 = System.currentTimeMillis();
				for (int repeat = 0; repeat< repeats; repeat++) {
					double[] results =tests.testMerging(optimizeForSJ, c.numberOfStreams, c.slidingWindowType, c.useOriginalAssignmentToStreams, c.maxEvents, 
							c.windowSizeInt, c.siblings, c.height, c.delta, c.epsilon, c.path, c.start, c.stop, repeat, c.collection);
					double SingleAbsoluteError = results[0];
					double SingleMaxAbsoluteError = results[1];
					double SingleMaxRelativeError = results[2];
					double SingleAvgRelativeError = results[3];
					double SingleRatioOfSeriousErrors = results[4];
					double SingleRequiredMemory = results[5];
					double SingleRequiredNetwork = results[6];
					double SingleTotalEvents = results[7];
					double SingleAvgInnerProductRelativeError = results[8];
					double SingleMaxInnerProductRelativeError = results[9];
					double SingleAvgRealRelErr = results[10];
					double SingleAvgInnerProductRealRelError = results[11];
					double SingleMaxRealRelErr = results[12];
					double SingleMaxInnerProductRealRelError = results[13];

					results r = new results(optimizeForSJ, path, tests.strSlidingWindowType(c.slidingWindowType), c.epsilon, c.delta, 
							c.numberOfStreams, c.siblings, c.height,  (int) SingleTotalEvents, SingleAvgRelativeError, SingleMaxRelativeError, 
							SingleRatioOfSeriousErrors, SingleAvgInnerProductRelativeError, SingleMaxInnerProductRelativeError,
							SingleRequiredMemory, SingleRequiredNetwork, 
							SingleAbsoluteError, SingleMaxAbsoluteError, SingleAvgRealRelErr, 
							SingleAvgInnerProductRealRelError, SingleMaxRealRelErr, SingleMaxInnerProductRealRelError);					
					
					System.err.println("===" + r.printConfiguration(true) + "," + r.printResults(true) + "," + r.printDetailedResults(true));
					System.err.println("---" + r.printConfiguration(false) + "," + r.printResults(false) + "," + r.printDetailedResults(false));

					AbsoluteError+=SingleAbsoluteError;
					MaxAbsoluteError+=SingleMaxAbsoluteError;
					MaxRelativeError +=SingleMaxRelativeError;
					AvgRelativeError+=SingleAvgRelativeError;
					RatioOfSeriousErrors+=SingleRatioOfSeriousErrors;
					RequiredMemory+=SingleRequiredMemory;
					RequiredNetwork+=SingleRequiredNetwork;
					TotalEvents+=SingleTotalEvents;
					AvgInnerProductRelativeError+=SingleAvgInnerProductRelativeError;
					MaxInnerProductRelativeError+=SingleMaxInnerProductRelativeError;
					AvgRealRelErr+=SingleAvgRealRelErr;
					AvgInnerProductRealRelError+=SingleAvgInnerProductRealRelError;
					MaxRealRelErr+=SingleMaxRealRelErr;
					MaxInnerProductRealRelError+=SingleMaxInnerProductRealRelError;

//					int rr=repeat+1;
//					results r = new results(optimizeForSJ, path, tests.strSlidingWindowType(c.slidingWindowType), c.epsilon, c.delta, 
//							c.numberOfStreams, c.siblings, c.height,  (int)TotalEvents, singleExecutionAvgRelErr, singleExecutionMaxRelErr, 
//							ratioOfSeriousErrors/ (double) rr, avgInnerProductRelativeError / (double) rr, 
//							maxInnerProductRelativeError, avgMemory / (double) rr, avgNetwork/ (double) rr, 
//							avgAbsError / (double) rr, maxAbsError, avgRealRelError / (double) rr, 
//							avgInnerProductRealRelError / (double) rr, maxRealRelError, maxInnerProductRealRelError);
//					System.err.println("===" + r.printConfiguration(true) + "," + r.printResults(true) + "," + r.printDetailedResults(true));
//					System.err.println("---" + r.printConfiguration(false) + "," + r.printResults(false) + "," + r.printDetailedResults(false));
				}
				results r = new results(optimizeForSJ, path, tests.strSlidingWindowType(c.slidingWindowType), c.epsilon, c.delta, c.numberOfStreams, c.siblings, c.height, 
						(int)(TotalEvents/(double) repeats), AvgRelativeError/repeats, MaxRelativeError/repeats, RatioOfSeriousErrors/repeats, AvgInnerProductRelativeError/repeats, 
						MaxInnerProductRelativeError/repeats, RequiredMemory/repeats, RequiredNetwork/repeats, AbsoluteError/repeats, MaxAbsoluteError/repeats, AvgRealRelErr/repeats, 
						AvgInnerProductRealRelError/repeats, MaxRealRelErr/repeats, MaxInnerProductRealRelError/repeats);
				System.err.println("|||" + r.printConfiguration(true) + "," + r.printResults(true) + "," + r.printDetailedResults(true));
				System.err.println("<<<" + r.printConfiguration(false) + "," + r.printResults(false));
				System.err.println("");
		
				time1=System.currentTimeMillis()-time1;
				System.err.println("Execution took " + time1/1000/repeats +" seconds");
				System.err.println("READ-ONLY times are SNMP: 2 seconds  WC-ALL:  260 seconds  ");
			}
		}
	}

}
