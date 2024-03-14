package experiments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import structure.logEventInt;
import structure.slidingCMSketch;
import structure.slidingCMSketch.sliding_window_structures;
import structure.slidingwindow;
import dataGeneration.ProcessedStreamLoaderGeneric;
import experiments.tests.collection_type;

class configuration {
	int numberOfStreams; sliding_window_structures slidingWindowType; boolean useOriginalAssignmentToStreams; long maxEvents; 
	int windowSizeInt; int siblings; int height; double delta; double epsilon; 
	String path; int start; int stop; tests.collection_type collection; int repeatId;

	public String toShortString() {
		return ".N" + numberOfStreams+".Type"+slidingWindowType.toString() + ".Ass" +
				   useOriginalAssignmentToStreams + ".Sibl" +  siblings + ".Hei" + height + 
				   ".D" + (float)delta +".E" + (float)epsilon + ".Coll" + collection.toString();

	}
	public String toString() {
		return numberOfStreams+" "+slidingWindowType.toString() + " " +
			   useOriginalAssignmentToStreams + " " + maxEvents + " " +
			   windowSizeInt + " " +  siblings + " " + height + " " + (float)delta +" " + (float)epsilon + " " + path + " " + 
			   start +" " + stop + " " + collection.toString();
	}
	public static configuration fromString(String s) {
		String[] ss = s.split(" ");
		int numberOfStreams=Integer.parseInt(ss[0]);
		sliding_window_structures slidingWindowType = sliding_window_structures.valueOf(ss[1]); 
		boolean useOriginalAssignmentToStreams=Boolean.parseBoolean(ss[2]); 
		long maxEvents = Long.parseLong(ss[3]); 
		int windowSizeInt = Integer.parseInt(ss[4]); 
		int siblings = Integer.parseInt(ss[5]); 
		int height = Integer.parseInt(ss[6]); 
		double delta = Float.parseFloat(ss[7]); 
		double epsilon = Float.parseFloat(ss[8]); 
		String path = ss[9]; 
		int start = Integer.parseInt(ss[10]); 
		int stop = Integer.parseInt(ss[11]); 
		tests.collection_type collection = tests.collection_type.valueOf(ss[12]);
		configuration c = new configuration(numberOfStreams, slidingWindowType, useOriginalAssignmentToStreams, maxEvents, windowSizeInt, siblings, height, delta, epsilon, path, start, stop, collection);
		return c;
	}
	public configuration(int numberOfStreams, sliding_window_structures slidingWindowType, boolean useOriginalAssignmentToStreams, long maxEvents, int windowSizeInt, 
			int siblings, int height, double delta, double epsilon, String path,
			int start, int stop, collection_type collection) {
		super();
		this.numberOfStreams = numberOfStreams;
		this.slidingWindowType=slidingWindowType;
		this.useOriginalAssignmentToStreams = useOriginalAssignmentToStreams;
		this.maxEvents = maxEvents;
		this.windowSizeInt = windowSizeInt;
		this.siblings = siblings;
		this.height = height;
		this.delta = delta;
		this.epsilon = epsilon;
		this.path = path;
		this.start = start;
		this.stop = stop;
		this.collection = collection;
	}
	
    @Override
    public int hashCode() {
    	return this.toString().hashCode();
//    	StringBuilder sb = new StringBuilder();
//    	sb.append("Streams:" + numberOfStreams + ", SWType:" + slidingWindowType + ", UseOriginalAssToStreams:" + useOriginalAssignmentToStreams+ 
//    			", MaxEvents:" + maxEvents+ ", windowSize:" + windowSizeInt+ ", Siblings:" + siblings+ ", Height:" + height+ ", Delta:" + (int)(delta*100d)+ ", Epsilon:" + (int)(epsilon*100d)+ 
//    			", CollectionPath:" + path + ", Start:" + start + ", Stop:" + stop  + ", Collection:" + collection);
//        return sb.toString().hashCode();
    }
}

public class tests {
	final static boolean debugMsgs=false;
	public enum collection_type {
		wc98, snmp, ips
	}
	
	static boolean singleLevel=false;
	static int streamDuration;
	static collection_type collection;
	public final static double ratio=1;

	
	static int repeats=-1;
	static String path=null;
	static double defaultDelta=0.15;
//	public static ArrayList<String[]> changeResolution() {
//		ArrayList<String[]> al = new ArrayList<>();
//		for (double delta:new double[]{defaultDelta}) {
//			for (double epsilon:new double[]{0.05,0.1,0.15,0.2,0.25}) {
//				int numberOfStreams=1, siblings=1,height=1; 
//				al.add(new String[]{path, ""+0,""+repeats,""+delta,""+epsilon, ""+numberOfStreams, ""+siblings, ""+height});
//				al.add(new String[]{path, ""+1,""+repeats,""+delta,""+epsilon, ""+numberOfStreams, ""+siblings, ""+height});
//				al.add(new String[]{path, ""+2,""+repeats,""+delta,""+epsilon, ""+numberOfStreams, ""+siblings, ""+height});
//			}
//		}
//		return al;
//	}
//	public static ArrayList<String[]> changeNetworkConf() {
//		ArrayList<String[]> al = new ArrayList<>();
//		int[] netSizes= new int[]{-1,2,4,8,16,32,64,128,256};
//		for (int netSize:netSizes) {
//			for (double epsilon:new double[]{0.05,0.1,0.15,0.2,0.25}) {
//				int numberOfStreams=netSize, siblings=2;
//				int height=computeHeight(siblings, numberOfStreams); 
//				al.add(new String[]{path, ""+0,""+repeats,""+defaultDelta,""+epsilon, ""+numberOfStreams, ""+siblings, ""+height});
//			}
//		}
//		return al;		
//	}

	public static ArrayList<configuration> singleVaryResolutionMain(long maxEvents, int start, int stop, int windowSizeInt, int repeats) {
		sliding_window_structures slidingWindowType = null;
		ArrayList<configuration> al = new ArrayList<>();
		int numberOfStreams=1, siblings=1,height=1; 
		for (double delta:new double[]{defaultDelta}) {
			for (double epsilon:new double[]{0.05,0.1,0.15,0.2,0.25,0.3}) {
				for (int type=0;type<3;type++) {					
					switch (type) {
					case 0:
						slidingWindowType = sliding_window_structures.EC;
						break;
					case 1:
						slidingWindowType = sliding_window_structures.DW;
						break;
					case 2:
						slidingWindowType = sliding_window_structures.RW;
						break;
					}
					boolean useOriginalAssignmentToStreams = false;
					for (int r=0;r<repeats;r++) {
						configuration c = new configuration(numberOfStreams, slidingWindowType, useOriginalAssignmentToStreams, maxEvents, windowSizeInt, siblings, height, delta, epsilon, path, start, stop, collection);
						al.add(c);
					}
				}
			}
		}
		return al;
	}

	public static String strSlidingWindowType(int type) {
		String strSlidingWindowType = "";
		switch (type) {
		case 0:
			strSlidingWindowType = "ExpHist-EC";
			break;
		case 1:
			strSlidingWindowType = "DetWave-DW";
			break;
		case 2:
			strSlidingWindowType = "RandWave-RW";
			break;
		}
		return strSlidingWindowType;
	}

	public static String strSlidingWindowType(sliding_window_structures type) {
		String strSlidingWindowType = "";
		switch (type) {
		case EC:
			strSlidingWindowType = "ExpHist-EC";
			break;
		case DW:
			strSlidingWindowType = "DetWave-DW";
			break;
		case RW:
			strSlidingWindowType = "RandWave-RW";
			break;
		}
		return strSlidingWindowType;
	}
	public static ArrayList<configuration> networkFixedSizeMain(long maxEvents, int start, int stop, int windowSizeInt, int repeats) {
		sliding_window_structures slidingWindowType = null;
		ArrayList<configuration> al = new ArrayList<>();		
		for (double delta:new double[]{defaultDelta}) {
			for (double epsilon:new double[]{0.05,0.1,0.15,0.2,0.25,0.3}) {
				for (int type=0;type<3;type++) {
					switch (type) {
					case 0:
						slidingWindowType = sliding_window_structures.EC;
						break;
					case 1:
						slidingWindowType = sliding_window_structures.DW;
						break;
					case 2:
						slidingWindowType = sliding_window_structures.RW;
						break;
					}
					boolean useOriginalAssignmentToStreams = true;
					int siblings=2,height=-1;
					if (collection==collection_type.snmp) {
						height = computeHeight(siblings, 535);
					} else if (collection==collection_type.wc98) {
						height = computeHeight(siblings, 33);
					} else if (collection==collection_type.ips) {
						height = computeHeight(siblings, 32);
					}
					
					for (int r=0;r<repeats;r++) {
						configuration c = new configuration(-1, slidingWindowType, useOriginalAssignmentToStreams, maxEvents, windowSizeInt, siblings, height, delta, epsilon, path, start, stop, collection);
						al.add(c);
					}
				}
			}
		}
		return al;
	}
	public static ArrayList<configuration> networkFixedSize16(long maxEvents, int start, int stop, int windowSizeInt, int repeats) {
		sliding_window_structures slidingWindowType = null;
		ArrayList<configuration> al = new ArrayList<>();		
		for (double delta:new double[]{defaultDelta}) {
			for (double epsilon:new double[]{0.05,0.1,0.15,0.2,0.25,0.3}) {
				for (int type=0;type<3;type++) {
					switch (type) {
					case 0:
						slidingWindowType = sliding_window_structures.EC;
						break;
					case 1:
						slidingWindowType = sliding_window_structures.DW;
						break;
					case 2:
						slidingWindowType = sliding_window_structures.RW;
						break;
					}
					boolean useOriginalAssignmentToStreams = false;
					int siblings=2,height=-1,numberOfNodes=16;
					height = computeHeight(siblings, numberOfNodes);
					
					for (int r=0;r<repeats;r++) {
						configuration c = new configuration(numberOfNodes, slidingWindowType, 
								useOriginalAssignmentToStreams, maxEvents, windowSizeInt, siblings, 
								height, delta, epsilon, path, start, stop, collection);
						al.add(c);
					}
				}
			}
		}
		return al;
	}

	public static ArrayList<configuration> networkIncreasingSizeMain(long maxEvents, int start, int stop, int windowSizeInt, int repeats) {
		sliding_window_structures slidingWindowType = null;
		ArrayList<configuration> al = new ArrayList<>();
		for (int netSize=2;netSize<=256;netSize*=2) {
			for (double delta:new double[]{defaultDelta}) {
				for (double epsilon:new double[]{0.15}) {
					for (int type=0;type<3;type++) {
						{						
						switch (type) {
						case 0:
							slidingWindowType = sliding_window_structures.EC;
							break;
						case 1:
							slidingWindowType = sliding_window_structures.DW;
							break;
						case 2:
							slidingWindowType = sliding_window_structures.RW;
							break;
						}
						boolean useOriginalAssignmentToStreams = false;
						int siblings=2,height=-1;
						height = computeHeight(siblings, netSize);
						for (int r=0;r<repeats;r++) {
							configuration c = new configuration(netSize, slidingWindowType, useOriginalAssignmentToStreams, maxEvents, windowSizeInt, siblings, height, delta, epsilon, path, start, stop, collection);
							al.add(c);
						}
						}		
					}
				}
			}
		}
		return al;
	}
	
	
	public static void main(String[]args) {
		//swtype: 0-EC, 1-DW, 2-RW
		// first find the time difference
		path = args[0];
		repeats = Integer.parseInt(args[1]);

		if (path.contains("snmp"))
			collection = collection_type.snmp;
		else if (path.contains("wc"))
			collection = collection_type.wc98;
		else if (path.contains("ips"))
			collection = collection_type.ips;
		
		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);
		int start,stop;
		start = psl.getStart();
		stop = psl.getStop();

		streamDuration=2000000;//864000; // 10 days
		long maxEvents=(long)(streamDuration*1000); // one event per msec on average

		if (collection==collection_type.ips) { 
			streamDuration=200000000;
			maxEvents=(long)(streamDuration*10);
		}
		ArrayList<configuration> al = new ArrayList<>();
		al.addAll(singleVaryResolutionMain(maxEvents, start, stop, streamDuration, repeats));
		al.addAll(networkFixedSize16(maxEvents, start, stop, streamDuration, repeats));
		al.addAll(networkIncreasingSizeMain(maxEvents, start, stop, streamDuration, repeats));
		
		HashMap<String,configuration> hm = new HashMap<>(); // remove duplicates
		for (configuration c:al) {
			hm.put(c.toString(), c);
		}
		for (configuration c:hm.values()) {
			int mem=0;
			if (c.slidingWindowType==sliding_window_structures.EC) {
				if (c.numberOfStreams<128)
					mem=2;
				else 
					mem=3;
			}
			if (c.slidingWindowType==sliding_window_structures.DW) {
				if (c.numberOfStreams<64)
					mem=2;
				else if (c.numberOfStreams<256)
					mem=3;
				else 
					mem=4;
			}
			if (c.slidingWindowType==sliding_window_structures.RW) {
				double startupCost=2;
				double memPerStream=0.0625*Math.pow(0.15/c.epsilon,1.5);
				double totalMem=memPerStream*c.numberOfStreams;
				mem=(int)Math.max(startupCost, totalMem+startupCost);
//				System.err.println(c.epsilon + " " + c.numberOfStreams + " " + mem );
//				if (c.numberOfStreams==1)
//					mem=2;
//				mem+=(int)Math.ceil(c.numberOfStreams/16);
//				if (0.1==(float)c.epsilon) mem=mem+mem/2;
//				if (0.05==(float)c.epsilon) mem=2*mem;
//				mem/=4;
//				mem=Math.max(2, mem);
			}
			if (mem>28)
				System.out.println("echo java -cp ContinuousEH.jar -Xmx" + mem + "g experiments.testsOneShotRun " + c.toString() + " " + repeats + " &> S" + c.toShortString());
			else
				System.out.println("java -cp ContinuousEH.jar -Xmx" + mem + "g experiments.testsOneShotRun " + c.toString() + " " + repeats + " &> S" + c.toShortString());
		}
	}
		
	
	public static double computeAllowedError(int height, double epsilon, sliding_window_structures slidingWindowType, boolean optimizeForIP) {
		double epsilonsw,epsiloncm;
		if (optimizeForIP) {
			epsilonsw = slidingCMSketch.getEpsilonSWIP(slidingWindowType, epsilon);
			epsiloncm = slidingCMSketch.getEpsilonCMIP(slidingWindowType, epsilon);
		}
		else {
			epsilonsw = slidingCMSketch.getEpsilonSW(slidingWindowType, epsilon);
			epsiloncm = slidingCMSketch.getEpsilonCM(slidingWindowType, epsilon);
			
		}
		double newEpsilon=0;
		switch (slidingWindowType) {
		case EC:
		case DW:
			double newEpsilonSW=height*epsilonsw*(1d+epsilonsw)+epsilonsw;
			newEpsilon=newEpsilonSW;//epsiloncm+newEpsilonSW+epsiloncm*newEpsilonSW;
			break;
		case RW:
			newEpsilon=epsilon;
			break;
		}
		return newEpsilon;
	}
	public static double computeAllowedErrorOnlyForPoint(int height, double epsilon, sliding_window_structures slidingWindowType) {
		double epsilonsw,epsiloncm;
		epsilonsw = slidingCMSketch.getEpsilonSW(slidingWindowType, epsilon);
		epsiloncm = slidingCMSketch.getEpsilonCM(slidingWindowType, epsilon);

		double newEpsilon=0;
		switch (slidingWindowType) {
		case EC:
		case DW:
			double newEpsilonSW=height*epsilonsw*(1d+epsilonsw)+epsilonsw;
			newEpsilon=epsiloncm+newEpsilonSW+epsiloncm*newEpsilonSW;
			break;
		case RW:
			newEpsilon=epsilon;
			break;
		}
		return newEpsilon;
	}
	static int computeHeight(int siblings, int numberOfrandWaves) {
		return (int) Math.ceil(Math.log(numberOfrandWaves)/Math.log(siblings));
	}

	static double[] testMerging(boolean optimizeForSJ, int numberOfRandWaves, sliding_window_structures slidingWindowType, boolean useOriginalAssignmentToStreams, long maxNumberOfEvents, 
			int windowSizeInt, int siblings, int height, double delta, double epsilon, String path, long startTime, 
			long endTime, int repeat, collection_type collection) {
		long datasetSize=endTime-startTime;
		long dataset10percent=datasetSize/10;
		System.err.println("Repeat " + repeat);
		int numberOfSeriousErrors=0;
		double requiredMemory=0;
		double requiredNetwork=0;
		int windowSize=windowSizeInt;
		if (numberOfRandWaves<=0) {
			if (collection==collection_type.snmp) {
				numberOfRandWaves = 547;
			} else if (collection==collection_type.wc98) {
				numberOfRandWaves = 33;
			} else if (collection==collection_type.ips) {
				numberOfRandWaves = 32;
			} 
			numberOfRandWaves=16;
		}
		height = computeHeight(siblings, numberOfRandWaves);

		slidingCMSketch[] scm = new slidingCMSketch[numberOfRandWaves];
		for (int cnt=0;cnt<scm.length;cnt++) {
			scm[cnt]=new slidingCMSketch(delta,epsilon, windowSize, maxNumberOfEvents,slidingWindowType, repeat, optimizeForSJ);
		}
		double allowedError=computeAllowedError(height, epsilon, slidingWindowType,optimizeForSJ);

		// now read stream
		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);
		
		int numberOfStreams=numberOfRandWaves;
		if (useOriginalAssignmentToStreams) {
			numberOfStreams=-1;
		}

		long duration=windowSizeInt;//startTime;
		int numberOfQueries = (int)Math.ceil(Math.log(duration)/Math.log(10))-2; // remove queries for time=10 and time=100

		int[] queryStartTimes=new int[numberOfQueries];
		for (int cnt=1;cnt<=numberOfQueries;cnt++) {
			queryStartTimes[cnt-1]=(int) (Math.max(0,endTime-Math.pow(10,cnt+2)));
		}
		int currentQueryId=numberOfQueries-1;
		int currentQueryStart=queryStartTimes[currentQueryId];
		int nextQueryStart=queryStartTimes[currentQueryId-1];
		HashMap<Integer, Integer> frequencies[] = new HashMap[numberOfQueries];
		for (int cnt=0;cnt<numberOfQueries;cnt++)frequencies[cnt]=new HashMap<Integer, Integer>(100000);
		int latestEvent=0;
//		psl.readDump(currentQueryStart-1000, numberOfStreams);
		int eventCount=0;
		long previousTime=0;
		int percentCount=0;
		while (true) {
			logEventInt levent = psl.readNextEvent(numberOfStreams);	
			if (levent==null)  {
				break;
			}
			if (levent.seconds-previousTime>=dataset10percent) {
				System.err.print(++percentCount);
				previousTime=levent.seconds;
			} 
			
			eventCount++;
			latestEvent=levent.seconds;
			scm[levent.streamid].add(levent.file, levent.seconds);
			if (levent.seconds<currentQueryStart) 
				continue;
			else {
				if (levent.seconds>=nextQueryStart) {
					currentQueryId--;
					if (currentQueryId<0) 
						break; // i executed all queries
					else {
						currentQueryStart=queryStartTimes[currentQueryId];
						if (currentQueryId>0)
							nextQueryStart=queryStartTimes[currentQueryId-1];
						else 
							nextQueryStart=Integer.MAX_VALUE;
					}
				}
				Integer val = frequencies[currentQueryId].get(levent.file);
				if (val!=null)
					frequencies[currentQueryId].put(levent.file, val+1);
				else
					frequencies[currentQueryId].put(levent.file, 1);
			}
		}
		System.err.println("Event count is " + eventCount);
		if (slidingWindowType==sliding_window_structures.RW)
			for (slidingCMSketch singleSketch:scm) {
				singleSketch.prepareForQuerying();
				for (int i=0;i<singleSketch.array.length;i++) for (int j=0;j<singleSketch.array[0].length;j++) {
					singleSketch.array[i][j].addAZero(latestEvent); // just to remove expired
				}
			}
		
//		System.err.println("Latest event arrived at " + latestEvent);
//		System.err.println("Total events " + numberOfEventsee);
		// now average the required memory (NOT THE NETWORK, JUST MEMORY)
		for (slidingCMSketch singleSketch:scm) {
			requiredMemory+=singleSketch.getRequiredMemory(); // in kilobytes: 
		}

		if (debugMsgs)
			System.err.println("Now starting the merging...");

		// Form the hierarchy
		ArrayList<slidingCMSketch> remaining = new ArrayList<slidingCMSketch>(scm.length);
		for (slidingCMSketch singleSketch:scm) {
			remaining.add(singleSketch);
		}
		for (int i=0;i<scm.length;i++) scm[i]=null;
		ArrayList<slidingCMSketch> remainingResults = new ArrayList<slidingCMSketch>(scm.length/siblings);
		do {
			slidingCMSketch[] workingSet=new slidingCMSketch[Math.min(siblings, remaining.size())];
			for (int cnt=0;cnt<remaining.size();cnt++) {
				workingSet[cnt%siblings]=remaining.get(cnt);
				if ((cnt+1)%siblings==0||(cnt+1==remaining.size())) { // merge
					for (slidingCMSketch singleSketch:workingSet) { 
						requiredNetwork+=singleSketch.getRequiredNetwork();
					}

					slidingCMSketch merged = slidingCMSketch.mergeSlidingCMSketches(workingSet, delta, epsilon, windowSize, repeat);
					merged.prepareForQuerying();
					if (slidingWindowType==sliding_window_structures.RW)
						for (int i=0;i<merged.array.length;i++)
							for (int j=0;j<merged.array[0].length;j++)
								merged.array[i][j].addAZero(latestEvent); // just to remove expired

					remainingResults.add(merged);
					for (int i=0;i<workingSet.length;i++)workingSet[i]=null;
					workingSet=new slidingCMSketch[Math.min(siblings, remaining.size()-cnt-1)];
				}
			}
			remaining=remainingResults;
			remainingResults= new ArrayList<slidingCMSketch>(remaining.size()/siblings);
		} while (remaining.size()>1);

		slidingCMSketch merged=remaining.get(0);
		remaining=null;
		merged.cloneForQuerying(); // i do not need to call prepare, because the sliding windows are already full. Only clone is needed!
		// now execute queries 
		HashMap<Integer, Integer> totalAnswers=new HashMap<Integer, Integer>(frequencies[numberOfQueries-1].size()*2);
		double numberOfEvents=0;
		double maxRelError=0;
		double avgRelError=0;
		double avgAbsError=0;
		double maxAbsError=0;
		double maxRelErrorIP=0;
		double avgRelErrorIP=0;
		double avgTrueRelError=0,avgTrueRelErrorIP=0,TrueMaxRelError=0,TrueMaxRelErrorIP=0;
		double queries = 0;
		double totalReadEvents=0;
		// print first line of merged
		if (debugMsgs)
			System.err.println("Now starting the queries...");
		for (int queryId=0;queryId<numberOfQueries;queryId++) {
			int queryTime=queryStartTimes[queryId];
			int queryTimeT=queryTime;
			// add answers of sliding window in totalAnswers
			for (Entry<Integer,Integer> entry:frequencies[queryId].entrySet()) {
				// first is file, second is frequency
				numberOfEvents+=entry.getValue();
				Integer val = totalAnswers.get(entry.getKey());
				if (val==null) totalAnswers.put(entry.getKey(),entry.getValue()); else totalAnswers.put(entry.getKey(), entry.getValue()+val);
			}

			for (Entry<Integer,Integer> entry:totalAnswers.entrySet()) {
				// first is file, second is frequency
				Integer val = entry.getValue();
				// now execute the query to the slidingwindow cm sketch
				double estimatedVal = merged.get(entry.getKey(), queryTimeT);
				double diff = Math.abs(val-estimatedVal);
				avgAbsError+=diff;
				maxAbsError=Math.max(diff,maxAbsError);
				double relativeError=diff/(double)numberOfEvents;
				double TrueRelativeError=diff/val;
				avgTrueRelError+=TrueRelativeError;
				TrueMaxRelError=Math.max(TrueMaxRelError, avgTrueRelError);
				if (relativeError>allowedError)numberOfSeriousErrors++;
				queries++;
				maxRelError=Math.max(relativeError,maxRelError);
				avgRelError+=relativeError;
			}
			totalReadEvents=Math.max(totalReadEvents,numberOfEvents);
			
			// now compute inner join
			double innerproduct=0;
			for (Entry<Integer,Integer> entry:totalAnswers.entrySet()) {
				// first is file, second is frequency
				double val=entry.getValue();
				innerproduct+=Math.pow(val, 2);
			}
			// and estimate inner join
			double estInnerProduct = merged.getInnerProduct(queryTimeT);
			double diffIP = Math.abs(estInnerProduct-innerproduct);
			double relDiff=diffIP/(numberOfEvents*numberOfEvents);
			double trueRelDiff=diffIP/estInnerProduct;
			avgTrueRelErrorIP+=trueRelDiff;
			TrueMaxRelErrorIP = Math.max(TrueMaxRelErrorIP,trueRelDiff);
			if (numberOfEvents==0) relDiff=0;
			avgRelErrorIP+=relDiff;
			maxRelErrorIP=Math.max(maxRelErrorIP,relDiff);
		}
		avgRelError=avgRelError/(double)queries;
		double[]results=new double[14];
		results[0] = avgAbsError/(double)queries;
		results[1] = maxAbsError;
		results[2] = maxRelError;
		results[3] = avgRelError;
		results[4] = numberOfSeriousErrors/(double)queries;
		results[5] = requiredMemory; // in kilobytes
		results[6] = requiredNetwork;
		results[7] = totalReadEvents;
		results[8] = avgRelErrorIP/(double)numberOfQueries;
		results[9] = maxRelErrorIP;
		results[10]=avgTrueRelError/(double)numberOfQueries;
		results[11]=avgTrueRelErrorIP/(double)numberOfQueries;
		results[12]=TrueMaxRelError;
		results[13]=TrueMaxRelErrorIP;
		return results;
	}
}
