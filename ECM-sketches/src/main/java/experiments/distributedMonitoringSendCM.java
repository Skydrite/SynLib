package experiments;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import structure.ExponentialHistogramCircularInt;
import structure.lib;
import structure.logEventInt;
import structure.slidingCMSketch;
import structure.slidingCMSketch.sliding_window_structures;
import dataGeneration.ProcessedStreamLoaderGeneric;

public class distributedMonitoringSendCM {
		
	static double[][][] addForAllNodes(double[][][][] nodes) { // [nodes][queries][w][d]monitorError
		int numberOfNodes = nodes.length;
		int numberOfQueries = nodes[0].length;
		int w = nodes[0][0].length;
		int d = nodes[0][0][0].length;
		double [][][] sum = new double[numberOfQueries][w][d];
		for (int i=0;i<numberOfNodes;i++) {
			for (int j=0;j<numberOfQueries;j++) {
					sum[j] = lib.addTwoVectors(sum[j],nodes[i][j]);
			}
		}
		return sum;
	}
	static double[][][] AverageForAllNodes(double[][][][] nodes) { // [nodes][queries][w][d]
		int numberOfNodes = nodes.length;
		int numberOfQueries = nodes[0].length;
		int w = nodes[0][0].length;
		int d = nodes[0][0][0].length;
		double [][][] sum = new double[numberOfQueries][w][d];
		for (int i=0;i<numberOfNodes;i++) {
			for (int j=0;j<numberOfQueries;j++) {
				sum[j] = lib.addTwoVectors(sum[j],nodes[i][j]);
			}
		}
		
		for (int j=0;j<numberOfQueries;j++) {
			sum[j] = lib.multVectorNoCloning(sum[j], 1d/numberOfNodes);
		}
		return sum;
	}
	
	static double[][] AverageForAllNodes(double[][][] nodes) { // [nodes][w][d]
		int numberOfNodes = nodes.length;
		int w = nodes[0].length;
		int d = nodes[0][0].length;
		double [][] sum = new double[w][d];
		for (int i=0;i<numberOfNodes;i++) {
			sum = lib.addTwoVectors(sum,nodes[i]);
		}
		sum = lib.multVectorNoCloning(sum,1d/numberOfNodes);
		return sum;
	}
	
	/*/
	static resultsContinuous monitorPointQuerySyncSingleCells(int numberOfNodes, long numberOfEvents, ProcessedStreamLoaderGeneric psl, double delta, double epsilon, int windowSize, 
			double[] thresholdFactors, structure.slidingCMSketch.sliding_window_structures structureType, Random rn, boolean debug, int repeats, int[] queryLengths, 
			double[] accuracies, String streamPath) {
		int frequentViolations=0,infrequentViolations=0;
		double[] allResults = new double[6+6*queryLengths.length];

		for (int repeat = 0; repeat < repeats; repeat++) {
			ExponentialHistogramCircularInt ehNumberOfEvents = new ExponentialHistogramCircularInt(0.001, windowSize, 0);
			boolean monitorError = false;
			double[] relativeErrors = new double[queryLengths.length];
			double[] relativeErrorDivisors = new double[queryLengths.length]; // number of queries to be executed
			double avgRelativeError = 0, avgRelativeErrorDivisor = 0;
			double[] precisions = new double[queryLengths.length];
			double[] recalls = new double[queryLengths.length];
			double[] precisionCounters = new double[queryLengths.length];
			double[] recallCounters = new double[queryLengths.length];

			int maxSyncsToPrint = 0;
			final double messageUsefulPayloadInKbytes = (32 + 32) / (8 * 1024d); // time, itemid --> kbytes
			double naiveMsgs = 0;
			double intelligentMsgs = 0;
			double intelligentTV = 0;
			int lastEventTime=-1;
			slidingCMSketch[] sketches = new slidingCMSketch[numberOfNodes]; // running sketches - time is currentTime
			for (int i = 0; i < numberOfNodes; i++)
				sketches[i] = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat);
			// lastSyncedSketches and mergedSketch are count-min sketches

			// slidingCMSketch[] lastSyncedSketches = new slidingCMSketch[numberOfNodes]; // last SENT sketches - time is lastSyncedTime PER COUNT
			// slidingCMSketch mergedSketch = null; // last merged sketch - known by all peers. Time is lastSyncedTime Per count
			slidingCMSketch globalSketchGroundTruth = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat);
			globalSketchGroundTruth.setMaintainInnerJoin(queryLengths);

			int w = globalSketchGroundTruth.getW();
			int d = globalSketchGroundTruth.getD();
			double[][][][] lastSyncedSketchesEstimations = new double[numberOfNodes][queryLengths.length][w][d];
			double[][][] lastMergedSyncedSketch = new double[queryLengths.length][w][d];
			boolean[] mergedSketchChanged = new boolean[queryLengths.length];

			for (int i = 0; i < numberOfNodes; i++) {
				sketches[i] = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat);
				sketches[i].setMaintainInnerJoin(queryLengths);

				for (int queryId = 0; queryId < queryLengths.length; queryId++)
					lastSyncedSketchesEstimations[i][queryId] = extractEstimations(sketches[i], queryLengths[queryId]);
			}
			lastMergedSyncedSketch = AverageForAllNodes(lastSyncedSketchesEstimations);
			for (int i = 0; i < mergedSketchChanged.length; i++)
				mergedSketchChanged[i] = true;

			int[] queryThresholds = new int[queryLengths.length];
			for (int queryId = 0; queryId < queryLengths.length; queryId++)
				queryThresholds[queryId] = (int) (thresholdFactors[queryId] * queryLengths[queryId]);
			HashSet<Integer> bannedItemsMethod[] = new HashSet[queryLengths.length];
			HashSet<Integer> bannedItemsGroundTruth[] = new HashSet[queryLengths.length];
			for (int queryId = 0; queryId < queryLengths.length; queryId++) {
				bannedItemsMethod[queryId] = new HashSet<>();
				bannedItemsGroundTruth[queryId] = new HashSet<>();
			}

			boolean initialRun = true;
			int i = 0;
			while (true) {
				i++;
				if (i == trainingPeriod && initialRun) {// numberOfEvents/10) { // reset all costs
					maxSyncsToPrint = 0;
					naiveMsgs = 0;
					intelligentMsgs = 0;
					intelligentTV = 0;
					System.err.println("\nStarting normal count");
					i = 0;
					initialRun = false;
					monitorError = true;
				}

				if (i % 1000000 == 0 && i != 0)
					System.err.print(".");
				if (i % 1000000 == 0) {
					double naivetv = computeTransferVolume(naiveMsgs, naiveMsgs * messageUsefulPayloadInKbytes);
					double inttv = computeTransferVolume(intelligentMsgs, intelligentTV);
					System.err.println("\n<<Round," + i + ",Nodes," + numberOfNodes + //",STREAM_PATH," + streamPath + 
							",delta," + delta + ",epsilon," + epsilon + ",windowSize," + windowSize
							+ ",naiveMsgs," + naiveMsgs + ",naiveTV," + naivetv + ",intMsgs," + intelligentMsgs + ",intTV," + inttv + ",relError,"
							+ (avgRelativeError / Math.max(1, avgRelativeErrorDivisor)) + ",ticks," + avgRelativeErrorDivisor + ",bannedItemsMethod," + printBannedItems(bannedItemsMethod)
							+ ",bannedItemsGT," + printBannedItems(bannedItemsGroundTruth));
					System.err.println("Frequent violations " + frequentViolations + " Infrequent Violations " + infrequentViolations);

				}

				logEventInt singleEvent = psl.readNextEvent(numberOfNodes);
				if (singleEvent == null)
					break;

				int currentTime = singleEvent.getTime();

				int sketchid = singleEvent.streamid;
				slidingCMSketch sketch = sketches[sketchid];

				int[] hashes = sketch.add(singleEvent.getEvent(), singleEvent.getTime());
				ehNumberOfEvents.addAOne(singleEvent.getTime());

				globalSketchGroundTruth.add(singleEvent.getEvent(), singleEvent.getTime(), hashes); // only for verification that estimation quality is achieved
				naiveMsgs++;

				HashMap<Integer, int[]> itemsRequiringSync[] = new HashMap[queryLengths.length];
				for (int queryId = 0; queryId < queryLengths.length; queryId++) {
					itemsRequiringSync[queryId] = new HashMap<>();

					// first update ground truth
					Iterator<Integer> bannedItemIterator = bannedItemsGroundTruth[queryId].iterator();

					while (bannedItemIterator.hasNext()) { // these are already banned and can be reduced
						int bannedItem = bannedItemIterator.next();
						int[] newhashes = sketch.hash(bannedItem, d, w);
						double globalSketchFreq = globalSketchGroundTruth.getFromMaintainedVector(queryId, newhashes)/numberOfNodes;
						if (globalSketchFreq < queryThresholds[queryId]) {
							bannedItemIterator.remove();
						}
					}

					// first check if the item is already banned
					if (!bannedItemsGroundTruth[queryId].contains(singleEvent.file)) {// not yet banned, and can only be increased
						double globalSketchFreq = globalSketchGroundTruth.getFromMaintainedVector(queryId, hashes)/numberOfNodes;
						if (globalSketchFreq >= queryThresholds[queryId]) {
							bannedItemsGroundTruth[queryId].add(singleEvent.file);
						}
					}

					// GEOMETRIC METHOD START
					if (lastEventTime<currentTime) {
						for (int nodeid = 0; nodeid < numberOfNodes; nodeid++) {
							if (nodeid != sketchid)
								sketches[nodeid].tick(currentTime); // otherwise it already ticked
						}
					} // otherwise it did not change

					boolean isItemPreviouslyBannedMethod = bannedItemsMethod[queryId].contains(singleEvent.file);
					for (int bannedItem : bannedItemsMethod[queryId]) {
						int[] newHashes = globalSketchGroundTruth.hash(bannedItem, d, w);
						double[] lastMergedSynced = new double[d];
						for (int dd=0;dd<d;dd++) {
							lastMergedSynced[dd] = lastMergedSyncedSketch[queryId][newHashes[dd]][dd];
						}

						boolean thresholdCrossingGMAtStep = false;
						for (int nodeid = 0; nodeid < numberOfNodes; nodeid++) {
							if (lastEventTime==currentTime && nodeid!= sketchid) continue;
							// compute upper-lower range
							if (thresholdCrossingGMAtStep)
								continue; // i wanted it to tick, but no need to do the estimation, since i anyway know i will send everything
							if (sketches[nodeid].getChangedSinceLast(queryId) || mergedSketchChanged[queryId]) {
								double[] lastLocalSynced = new double[d];
								double[] newLocalStatistics = new double[d];
								double[][]maintainedEstimations = sketches[nodeid].getMaintainedEstimations(queryId);
								for (int dd=0;dd<d;dd++) {
									lastLocalSynced[dd] = lastSyncedSketchesEstimations[nodeid][queryId][newHashes[dd]][dd];
									newLocalStatistics[dd] = maintainedEstimations[newHashes[dd]][dd];
								}
								structure.TupleGeneric<double[], Double> tg =  lib.computeBallCenterAndRadius(lastMergedSynced, lastLocalSynced, newLocalStatistics);
								double minDistance = lib.computeMinDistanceFromInadmissibleRegion(true,tg.getFirst(),queryThresholds[queryId] * (1d - accuracies[queryId]));
								if (tg.getSecond()>=minDistance) {
									thresholdCrossingGMAtStep=true;
									frequentViolations++;
									itemsRequiringSync[queryId].put(bannedItem, newHashes);
									if (maxSyncsToPrint > 0) {
										System.out.print("\nRound " + i + " Event " + singleEvent + "   At node " + nodeid + " queryLength " + queryLengths[queryId] + " radius "
												+ tg.getSecond() + " minDist " + minDistance);
										maxSyncsToPrint--;
										if (maxSyncsToPrint == 0)
											System.err.println("\nToo many lines. Stopping the output...");
									}
								}
							} else
								continue; // no change, so everything stayed as is
						}
					}

					// also check the new item for upper threshold
					{
						int nodeid = sketchid;
						double[] lastLocalSynced = new double[d];
						double[] newLocalStatistics = new double[d];
						double[] lastMergedSynced = new double[d];
						double[][]maintainedEstimations = sketches[nodeid].getMaintainedEstimations(queryId);
						for (int dd=0;dd<d;dd++) {
							lastLocalSynced[dd] = lastSyncedSketchesEstimations[nodeid][queryId][hashes[dd]][dd];
							newLocalStatistics[dd] = maintainedEstimations[hashes[dd]][dd];
							lastMergedSynced[dd] = lastMergedSyncedSketch[queryId][hashes[dd]][dd];
						}
						structure.TupleGeneric<double[], Double> tg =  lib.computeBallCenterAndRadius(lastMergedSynced, lastLocalSynced, newLocalStatistics);
						double minDistance=0;
						if (isItemPreviouslyBannedMethod) {
							minDistance = lib.computeMinDistanceFromInadmissibleRegion(true,tg.getFirst(),queryThresholds[queryId] * (1d - accuracies[queryId]));
						} else { // not banned --> factor = 1d+accuracies
							minDistance = lib.computeMinDistanceFromInadmissibleRegion(false,tg.getFirst(),queryThresholds[queryId] * (1d + accuracies[queryId]));
						}
						if (tg.getSecond()>=minDistance) {
							if (isItemPreviouslyBannedMethod) 
								frequentViolations++;
							else 
								infrequentViolations++;
							itemsRequiringSync[queryId].put(singleEvent.file, hashes);
							if (maxSyncsToPrint > 0) {
								System.out.print("\nRound " + i + " Event " + singleEvent + "   At node " + nodeid + " queryLength " + queryLengths[queryId] + " radius "
										+ tg.getSecond() + " minDist " + minDistance);
								maxSyncsToPrint--;
								if (maxSyncsToPrint == 0)
									System.err.println("\nToo many lines. Stopping the output...");
							}
						}
					}
					for (int nodeid = 0; nodeid < numberOfNodes; nodeid++)
						sketches[nodeid].setChangedSinceLast(queryId, false);
					mergedSketchChanged[queryId] = false;
				}

//				HashSet<String> requestedWindows = new HashSet<>(); // format is (timeStart, w, d)
				boolean sentAtLeastOnce = false;
				for (int queryId = 0; queryId < queryLengths.length; queryId++) {
					// now do the necessary synchronizations
					if (!itemsRequiringSync[queryId].isEmpty()) {
						HashMap<Integer, int[]> countersToSync = new HashMap<>();
						for (Entry<Integer, int[]> e : itemsRequiringSync[queryId].entrySet()) {
							int[] hashValues = e.getValue();
							for (int dcnt = 0; dcnt < d; dcnt++)
								countersToSync.put(slidingCMSketch.getUniqueCount(w, hashValues[dcnt], dcnt), new int[] { hashValues[dcnt], dcnt });
						}
						// synchronize all counters in the countersToSync map
						for (int[] counter : countersToSync.values()) {
							int wcnt = counter[0];
							int dcnt = counter[1];
							for (int nodeid = 0; nodeid < numberOfNodes; nodeid++) { // sync all nodes
								lastSyncedSketchesEstimations[nodeid][queryId][wcnt][dcnt] = sketches[nodeid].getMaintainedEstimations(queryId)[wcnt][dcnt];
								intelligentTV += 4d / 1024d; // 4 bytes for (wcnt,dcnt encoded to 1 integer)
									if (queryLengths.length==1)
										intelligentTV += (4) * 2d / 1024d; // a single counter value for each query... and send the merged back (following exactly the same order)!
									else
										intelligentTV += (4 + 4) * 2d / 1024d; // queryid + a single counter value for each query... and send the merged back (following exactly the same order)!
							}
							// and send the merged one back
							slidingCMSketch.mergeSlidingCMSketchesToCM(sketches, lastMergedSyncedSketch, wcnt, dcnt, queryId);
							lastMergedSyncedSketch[queryId][wcnt][dcnt]/=numberOfNodes;
						}

						if (!sentAtLeastOnce) {
							intelligentMsgs += (numberOfNodes * 3); // to send the query, receive the result, and send back the merged EH (all of them in a single message)
							sentAtLeastOnce = true;
						}

						// now check for items that should be added/removed at the banned ones
						for (Entry<Integer, int[]> e : itemsRequiringSync[queryId].entrySet()) {
							int url = e.getKey();
							{
								double val = slidingCMSketch.getFromGivenVector(queryId, lastMergedSyncedSketch, e.getValue());
								if (val > queryThresholds[queryId])
									bannedItemsMethod[queryId].add(url);
								else
									bannedItemsMethod[queryId].remove(url);
							}
						}
						// GM END
					}
				}

				if (monitorError) {
					// now the relative error
					for (int queryId = 0; queryId < queryLengths.length; queryId++) {
						double numberOfEventsWithinRange = ehNumberOfEvents.getEstimationRealtime(currentTime - queryLengths[queryId]);

						HashSet<Integer>[] AndXor = AndXor(bannedItemsMethod[queryId], bannedItemsGroundTruth[queryId]);
						HashSet<Integer> And = AndXor[0];
						HashSet<Integer> Xor = AndXor[1];

						for (int itemid : Xor) {
							int[] newhashes = sketch.hash(itemid, d, w);
							double realEstimation = globalSketchGroundTruth.getFromMaintainedVector(queryId, newhashes)/numberOfNodes;
							relativeErrors[queryId] += (Math.abs(realEstimation - queryThresholds[queryId]) / numberOfEventsWithinRange);
							relativeErrorDivisors[queryId]++;
							avgRelativeError += (Math.abs(realEstimation - queryThresholds[queryId]) / numberOfEventsWithinRange);
							avgRelativeErrorDivisor++;
						}
						double detectedCorrectly = And.size();
						if (bannedItemsMethod[queryId].size() > 0) {
							double precision;
							precision = detectedCorrectly / bannedItemsMethod[queryId].size();
							precisions[queryId] += precision;
							precisionCounters[queryId]++;
						}
						if (bannedItemsGroundTruth[queryId].size() > 0) {
							double recall;
							recall = detectedCorrectly / bannedItemsGroundTruth[queryId].size();
							recalls[queryId] += recall;
							recallCounters[queryId]++;
						}
					}
				}
				lastEventTime=currentTime;
			}
			double naivetv = computeTransferVolume(naiveMsgs, naiveMsgs * messageUsefulPayloadInKbytes);
			double inttv = computeTransferVolume(intelligentMsgs, intelligentTV);
			if (avgRelativeErrorDivisor == 0)
				avgRelativeErrorDivisor = 1;
			double[] repetitionResults = new double[6 + 6 * queryLengths.length];
			int pos = 0;
			repetitionResults[pos++] = naiveMsgs;
			repetitionResults[pos++] = naivetv;
			repetitionResults[pos++] = intelligentMsgs;
			repetitionResults[pos++] = inttv;
			repetitionResults[pos++] = avgRelativeError / avgRelativeErrorDivisor;
			repetitionResults[pos++] = avgRelativeErrorDivisor;

			for (int queryId = 0; queryId < queryLengths.length; queryId++) {
				if (precisionCounters[queryId] == 0)
					precisionCounters[queryId] = 1;
				if (recallCounters[queryId] == 0)
					recallCounters[queryId] = 1;
				if (relativeErrorDivisors[queryId] == 0)
					relativeErrorDivisors[queryId] = 1;

				repetitionResults[pos++] = thresholdFactors[queryId];
				repetitionResults[pos++] = accuracies[queryId];
				repetitionResults[pos++] = queryLengths[queryId];
				repetitionResults[pos++] = relativeErrors[queryId] / relativeErrorDivisors[queryId];
				repetitionResults[pos++] = precisions[queryId] / precisionCounters[queryId];
				repetitionResults[pos++] = recalls[queryId] / recallCounters[queryId];
			}
			for (int c = 0; c < repetitionResults.length; c++)
				allResults[c] += repetitionResults[c] / repeats;
		}
		resultsContinuous rc = new resultsContinuous(streamPath, tests.strSlidingWindowType(structureType), epsilon, delta, numberOfNodes, numberOfNodes, 0, numberOfEvents, allResults);
		return rc;
	}
	
	
	/*/
	
	public static String printBannedItems(HashSet<Integer> bannedItems[]) {
		String s = "[";
		for (HashSet<Integer> h:bannedItems) s+=h.size()+" ";
		return s + "]";
	}

	boolean debug=false;
	static int trainingPeriod=0;
	
	static String queryDescription="";
	// 4 uniform null 0.05 0.05 0.025 50 434 
//	static collection_type collection;
	public final static double ratio=1;

	public static void main(String[]args) {
		String s = "";		
		for (String ss:args) s = s + ss + " ";
		s=s.trim();
		System.err.println("java -cp ContinuousEH.jar -Xmx4g experiments.distributedMonitoringSendCM " + s);
		
		sliding_window_structures structureType = sliding_window_structures.EC;
		int numberOfNodes = Integer.parseInt(args[0]);
		String path = args[1];
		double delta = Double.parseDouble(args[2]);
		double epsilon = Double.parseDouble(args[3]);
		int repeats = Integer.parseInt(args[4]);
//		if (path.contains("snmp")) {
//			collection = collection_type.snmp;
////			trainingPeriod=10000000;
//		}
//		else {
//			collection = collection_type.wc98;
////			trainingPeriod=10000000;
//		}
		trainingPeriod=10000000;

		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);

		
		// and now the queries to monitor
		int startPos=5;
		int numberOfQueries = (args.length-startPos)/3;
		
		double[] accuracies = new double[numberOfQueries];
		double[] thresholdFactors = new double[numberOfQueries];
		int[] queryLengths = new int[numberOfQueries];
		
		for (int i=0;i<numberOfQueries;i++) {
			accuracies[i] = Double.parseDouble(args[startPos+i*3]);
			thresholdFactors[i] = Double.parseDouble(args[startPos+i*3+1]);
			queryLengths[i] = Integer.parseInt(args[startPos+i*3+2]);
			queryDescription+="[Q"+i + ":Acc=" + accuracies[i] + ",Thres="+ thresholdFactors[i] +",qLen=" + queryLengths[i] + "]";
		}
		int windowSize = 0; 
		for (int i:queryLengths) windowSize=Math.max(windowSize,i);
		long maxEvents=(long)(windowSize*1000d*ratio); // one event per msec on average
		java.util.Random rn = new java.util.Random(repeats);

		System.err.println("Sliding window length is " + windowSize +  " seconds and maxEvents in SW is " + maxEvents);
		System.err.println("Repeats: " + repeats + " and memory results printed in Kbytes");
		System.err.println(queryDescription);
		

//		resultsContinuous rc = distributedMonitoringSendCM.monitorPointQuerySyncSingleCells(numberOfNodes, maxEvents, psl, delta, epsilon, windowSize, thresholdFactors, structureType, rn, false,  repeats, queryLengths, accuracies, path);
//		System.err.println(rc.printResults(true));
		
		lib.saveNetwork = true;
		System.err.println("lib.saveNetwork is " + lib.saveNetwork);
		ProcessedStreamLoaderGeneric psl2 = new ProcessedStreamLoaderGeneric(path);
		long startTime = System.currentTimeMillis();
		resultsContinuousSJ rcSJ = distributedMonitoringSendCM.monitorSelfJoinQuery(numberOfNodes, maxEvents, psl, psl2, delta, epsilon, windowSize, structureType, rn, false, repeats, queryLengths[0], accuracies[0], path);
		long duration = System.currentTimeMillis()-startTime;
		System.err.println("Duration is " + duration/1000);
		System.err.println(rcSJ.printConfiguration(true));
		System.err.println(rcSJ.printResults(true));
		// and now full results
		System.err.println("\n>>" + rcSJ.printConfigurationLabels() + "," + rcSJ.printResultsLabels());
		System.err.println("\n>>" + rcSJ.printConfiguration(false) + "," + rcSJ.printResults(false));
	}
	
	final static void printDebugOld(PrintStream ps, boolean debug, String msg) {
		if (debug)
			ps.print(msg);
	}
	
	final static void printDebugOld(boolean debug, String msg) {
		if (debug)
			System.out.print(msg);
	}
		
	
	static double computeL2NormOfRow(int row, double[][]countMin) {
		double d = 0;
		for (int i=0;i<countMin.length;i++)
			d+=(countMin[i][row]*countMin[i][row]);
		return Math.sqrt(d);
	}
	static double computeL2NormOfVector(double[]v) {
		double d = 0;
		for (int i=0;i<v.length;i++)
			d+=(v[i]*v[i]);
		return Math.sqrt(d);
	}

	static double[] computeDi(double[][] currentPoint, double[][] lastSyncedCountMin) {
		int w = currentPoint.length; int d = currentPoint[0].length;
		double[] dis = new double[d];
		
		for (int row=0;row<d;row++) {
			for (int i=0;i<w;i++) {
				double diff = currentPoint[i][row]-lastSyncedCountMin[i][row];
				dis[row]+=lib.square(diff);
			}
			dis[row] = Math.sqrt(dis[row]);
		}
		return dis;
	}
	
	static double estimateSelfJoin(double[][] cmSketch, int w, int d) {
		double v = 0;
		double vMin = Double.MAX_VALUE;
		for (int row=0;row<d;row++) {
			for (int i=0;i<w;i++) {
				v+=lib.square(cmSketch[i][row]);
			}
			vMin = Math.min(vMin,v);
		}
		return vMin;
	}
	static boolean cleverSync=true;
	
	
	static resultsContinuousSJ monitorSelfJoinQuery(int numberOfNodes, long numberOfEvents, ProcessedStreamLoaderGeneric psl, ProcessedStreamLoaderGeneric psl2, double delta, double epsilon, int windowSize, 
			structure.slidingCMSketch.sliding_window_structures structureType, Random rn, boolean debug, int repeats, int queryLength, 
			double accuracy, String streamPath) {
		double[] allResults = new double[13];
		float freqErrorCheck=0.001f;
		int freqErrorCheckReverse = (int)Math.round(1f/freqErrorCheck);
		for (int repeat = 0; repeat < repeats; repeat++) {
			System.err.println("Starting repetition  " + repeat);
			psl.reset();
			psl2.reset();
			double groundTruthSJ=0;
			double numberOfEventsAccurate=0;
			HashMap<Integer, Integer> freqsGroundTruth = new HashMap<>(1000000);
			logEventInt singleEventToErase = psl2.readNextEvent(numberOfNodes);

			int syncsDi=0, syncsFull=0;
			ExponentialHistogramCircularInt ehNumberOfEvents = new ExponentialHistogramCircularInt(0.0001, windowSize, 0);
			boolean monitorError = false;
			double maxRelativeError = -Double.MAX_VALUE;
			double maxRelativeErrorAccurate = -Double.MAX_VALUE;
			double avgrelativeErrorAccurate = 0;
			double ErrorDivisor = 0;
			double avgRelativeError = 0; 
			double absoluteErrorAccurate = 0;

			final double messageUsefulPayloadInKbytes = (32 + 32) / (8 * 1024d); // time, itemid --> kbytes
			final double singleCounterSizeInKbytes = 32 / (8 * 1024d); // 32 bits for the value
			final double singleBitInKbytes = 1 / (8 * 1024d); 
			double naiveMsgs = 0;
			double intelligentMsgs = 0;
			double intelligentTV = 0;
			double intelligentMsgsUpload = 0;
			double intelligentTVUpload = 0;
			slidingCMSketch[] sketches = new slidingCMSketch[numberOfNodes]; // running sketches - time is currentTime
//			for (int i = 0; i < numberOfNodes; i++)
//				sketches[i] = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat, true);
			// lastSyncedSketches and mergedSketch are count-min sketches

			slidingCMSketch globalSketchGroundTruth = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat, true);
			globalSketchGroundTruth.setMaintainInnerJoin(new int[]{queryLength});

			int w = globalSketchGroundTruth.getW();
			int d = globalSketchGroundTruth.getD();
			// [nodeid][queryid][w][d]
			double[][][] lastSyncedSketchesEstimations = new double[numberOfNodes][w][d];
			// [nodeid][queryid][0:up, 1:down][d]
			double[][][] firstLevelDelta = new double[numberOfNodes][2][d]; // 0 for TBI, 1 for TBF queries
			double[][] lastMergedSketch = new double[w][d];
			
			boolean mergedSketchChanged = true;

			for (int i = 0; i < numberOfNodes; i++) {
				sketches[i] = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat, true);
				sketches[i].setMaintainInnerJoin(new int[]{queryLength}); // this is only to maintain the sliding window counter estimations at separate count-min sketches, for performance reasons

				lastSyncedSketchesEstimations[i] = new double[w][d];// init to 0
			}

			double lastFSynced = 0; // this is f(v_0) = f(0) = 0

			boolean initialRun = true;
			int i = 0;
			
			int lastEventTime=-1;
			double[] vL2 = new double[d];
			double[] vL2Normalized = new double[d];
			int dRetrieved=0,cmRetrieved=0;
			double lastEstSJ=0;
			int lastReadTime=0;
			while (true) {
				i++;
				naiveMsgs++;
				if ( initialRun && i >= trainingPeriod*1.1 && lastReadTime>queryLength*1.1) {// numberOfEvents/10) { // reset all costs
					rn.setSeed(repeat);
					naiveMsgs = 0;
					intelligentMsgs = 0; intelligentTV = 0;
					intelligentMsgsUpload = 0; intelligentTVUpload = 0;
					System.err.println("\nStarting normal count");
					i = 0;
					dRetrieved=0;cmRetrieved=0;
					initialRun = false;
					monitorError = true;
					syncsDi =0; syncsFull =0;
				}

				
				logEventInt singleEvent = psl.readNextEvent(numberOfNodes);
				if (singleEvent==null) break;
				lastReadTime=singleEvent.seconds;

				if (i % 1000000 == 0 && i!=0 ) {
					double naivetv = computeTransferVolume(naiveMsgs, naiveMsgs * messageUsefulPayloadInKbytes);
					double inttv = computeTransferVolume(intelligentMsgs, intelligentTV);
					double inttvUpload = computeTransferVolume(intelligentMsgsUpload, intelligentTVUpload);
					System.err.print("\n<<Round," + i + ",Nodes," + numberOfNodes + //",STREAM_PATH," + streamPath + 
							",delta," + delta + ",epsilon," + epsilon +",theta," + accuracy + ",windowSize," + windowSize
							+ ",naiveMsgs," + naiveMsgs + ",naiveTV," + (int)naivetv + ",intMsgs," + (int)intelligentMsgs + ",intTV," + (int) inttv + ",relErrorAccurate,"
							+ (avgrelativeErrorAccurate / Math.max(1, ErrorDivisor)) + ",maxRelErrorAccurate," + maxRelativeErrorAccurate + ",relError,"
							+ (avgRelativeError / Math.max(1, ErrorDivisor)) + ",maxRelError," + maxRelativeError 
							+ ",AbsErrorAcc," + (absoluteErrorAccurate / Math.max(1, ErrorDivisor))	+ ",FreqErrorCheck," + freqErrorCheck + 
							",intMsgsUpload," + (int)intelligentMsgsUpload + ",intTvUpload,"+(int)inttvUpload);
					System.err.println("\nSyncs " + syncsDi + "," + syncsFull  + " LastTime " + singleEvent.seconds + " DRetrieved "+ dRetrieved + "  CMRetrieved " + cmRetrieved+ " GT " + groundTruthSJ + " lastEstSJ " + lastEstSJ + " numberOfEvents " + numberOfEventsAccurate);
				}
				
				int currentTime = singleEvent.getTime();

				// update with the new arrival
				{
					int ev = singleEvent.getEvent();
					Integer oldFreq = freqsGroundTruth.get(ev);
					if (oldFreq==null) {
						oldFreq=0;
						freqsGroundTruth.put(ev,1);
					}
					else 
						freqsGroundTruth.put(ev,oldFreq+1);

					groundTruthSJ = groundTruthSJ+oldFreq+oldFreq+1; // old=new-1 : This equals to +new^2-old^2
					numberOfEventsAccurate++;
				}

				// then delete expired
				while (singleEventToErase.seconds<currentTime-queryLength) {
					int ev = singleEventToErase.getEvent();
					int oldFreq = freqsGroundTruth.get(ev);
					int newFreq = oldFreq-1;
					if (newFreq==0) freqsGroundTruth.remove(ev); else freqsGroundTruth.put(ev,newFreq);
					singleEventToErase = psl2.readNextEvent(numberOfNodes);
					groundTruthSJ = groundTruthSJ-(oldFreq+newFreq);  // old=new+1 : This equals to +new^2-old^2
					numberOfEventsAccurate--;
				}
				

				int sketchid = singleEvent.streamid;
				slidingCMSketch sketch = sketches[sketchid];

				int[] hashes = sketch.add(singleEvent.getEvent(), singleEvent.getTime());
				ehNumberOfEvents.addAOne(singleEvent.getTime());

				globalSketchGroundTruth.add(singleEvent.getEvent(), singleEvent.getTime(), hashes); // only for verification that estimation quality is achieved

				if (lastEventTime<currentTime) {
					for (int nodeid=0;nodeid<numberOfNodes;nodeid++)
						if (nodeid!=sketchid)
							sketches[nodeid].tick(currentTime);
					lastEventTime=currentTime;
				}
				
				double[][][] diNodes = new double[numberOfNodes][2][]; // 0 for up, 1 for down, 3rd dim is d

				// now execute the GM
				// check
				// TBI: To become infrequent, TBF: To become frequent
				double theta = accuracy;
				double stayHigherThanThis = Math.sqrt(lastFSynced/(1d+theta))/numberOfNodes; // needs to stay higher than this - TBI
				double stayLowerThanThis  = Math.sqrt(lastFSynced/(1d-theta))/numberOfNodes; // needs to stay lower  than this - TBF
				HashSet<Integer> nodesCausingViolation=new HashSet<>();
				boolean[] needToSync = new boolean[2]; // 0 for up, 1 for down
				
				for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
					if (!(sketches[nodeid].getChangedSinceLast(0) || mergedSketchChanged )) continue;
					if (diNodes[nodeid][0]==null) { // node, up/down, d
						diNodes[nodeid] = lib.sqrt(lib.computePosNegInnerProductPerRow(lib.subtractTwoVectors2d(sketches[nodeid].getMaintainedEstimations(0),lastSyncedSketchesEstimations[nodeid])));
					}
					double[][]deltaOfDi = firstLevelDelta[nodeid];
					needToSync = checkForViolationSelfJoin(vL2Normalized, diNodes[nodeid], deltaOfDi, stayHigherThanThis, stayLowerThanThis);
					if (needToSync[0] || needToSync[1]) nodesCausingViolation.add(nodeid);
					// messages and tv will be counted later in the loop 
					sketches[nodeid].setChangedSinceLast(0, false);
				}
				mergedSketchChanged=false;
				
				if (!nodesCausingViolation.isEmpty()) { // need to sync
					syncsDi++;
					HashSet<Integer>retrievedUpToNow = new HashSet<>();
					double[] partialSumD0 = new double[d];
					double[] partialSumD1 = new double[d];
					for (int nodeid:nodesCausingViolation) {
						dRetrieved++;
						retrievedUpToNow.add(nodeid); // TODO: download d_i vector and delta vectors
						// i need to send two counters:d0 and d1 (to monitor upper and lower thresholds)
						intelligentMsgs++; intelligentTV+=2*d*singleCounterSizeInKbytes; // type (D_i vs CM) can be understood by the size. delta is known by the coordinator. --> send only SUM(delta,di) 
						intelligentMsgsUpload++; intelligentTVUpload+=2*d*singleCounterSizeInKbytes; // type (D_i vs CM) can be understood by the size. delta is known by the coordinator. --> send only SUM(delta,di) 
						partialSumD0 = lib.addTwoVectors(partialSumD0, diNodes[nodeid][0]);
						partialSumD0 = lib.addTwoVectors(partialSumD0, firstLevelDelta[nodeid][0]);
						partialSumD1 = lib.addTwoVectors(partialSumD1, diNodes[nodeid][1]);
						partialSumD1 = lib.addTwoVectors(partialSumD1, firstLevelDelta[nodeid][1]);
//                        for (int dd=0;dd<d;dd++)
//                        	if ((diNodes[nodeid][0]!=null && diNodes[nodeid][1]!=null && diNodes[nodeid][0][dd]!=0 && diNodes[nodeid][1][dd]!=0) ||
//                        		(firstLevelDelta[nodeid][0]!=null && firstLevelDelta[nodeid][1]!=null && firstLevelDelta[nodeid][0][dd]!=0 && firstLevelDelta[nodeid][1][dd]!=0))
//                        		System.err.println("Oi apparon");
					}
					// now check averagew
					double[] partialAvgD0 = lib.divVector(partialSumD0, nodesCausingViolation.size());
					double[] partialAvgD1 = lib.divVector(partialSumD1, nodesCausingViolation.size());
					if (nodesCausingViolation.size()>1) // try again
						needToSync = checkForViolationSelfJoin(vL2Normalized, new double[][]{partialAvgD0,partialAvgD1}, new double[2][d], stayHigherThanThis, stayLowerThanThis);
					ArrayList<Integer> al = lib.getShuffle(numberOfNodes);
					Iterator<Integer> iterAl = al.iterator();
					
					while (iterAl.hasNext() && (retrievedUpToNow.size()<=1 || needToSync[0] || needToSync[1])) 	{
						// now check average
						int nextNode = iterAl.next();
						if (retrievedUpToNow.contains(nextNode)) continue;
						dRetrieved+=2;
						retrievedUpToNow.add(nextNode); // TODO: download d_i vector and delta vectors
						if (diNodes[nextNode][0]==null) {
							diNodes[nextNode] = lib.sqrt(lib.computePosNegInnerProductPerRow(lib.subtractTwoVectors2d(sketches[nextNode].getMaintainedEstimations(0),lastSyncedSketchesEstimations[nextNode])));
						}
						intelligentMsgs+=2; intelligentTV+=2*d*singleCounterSizeInKbytes+singleBitInKbytes; // two messages (request,response). Request is 1 bit for the type. Response does not need type. Delta is known by the coordinator. --> send only SUM(delta,di)
						intelligentMsgsUpload++; intelligentTVUpload+=2*d*singleCounterSizeInKbytes; // upload is only one message - request is ignored
						partialSumD0 = lib.addTwoVectors(partialSumD0, diNodes[nextNode][0]);
						partialSumD0 = lib.addTwoVectors(partialSumD0, firstLevelDelta[nextNode][0]);
						partialSumD1 = lib.addTwoVectors(partialSumD1, diNodes[nextNode][1]);
						partialSumD1 = lib.addTwoVectors(partialSumD1, firstLevelDelta[nextNode][1]);
						partialAvgD0 = lib.divVector(partialSumD0, retrievedUpToNow.size());
						partialAvgD1 = lib.divVector(partialSumD1, retrievedUpToNow.size());
						needToSync = checkForViolationSelfJoin(vL2Normalized, new double[][]{partialAvgD0,partialAvgD1}, new double[2][d], stayHigherThanThis, stayLowerThanThis);
					}
					if (!needToSync[0]&& !needToSync[1]) { // violation handled with 1st-level delta
//						double[][] diSums=new double[2][d];
						for (int nodeid:retrievedUpToNow) {
							firstLevelDelta[nodeid][0] = lib.subtractTwoVectors(partialAvgD0, diNodes[nodeid][0]);
							firstLevelDelta[nodeid][1] = lib.subtractTwoVectors(partialAvgD1, diNodes[nodeid][1]);
							intelligentMsgs++; intelligentTV+=2*d*singleCounterSizeInKbytes; // send the new avg back so that peers fix their deltas
							// do not need to count anything w.r.t. intelligentMsgsUpload, intelligentTVUpload! The direction for this is from coordinator to peers
							dRetrieved++;
//							diSums = lib.addTwoVectors(diSums, diNodes[nodeid]);
//							diSums = lib.addTwoVectors(diSums, firstLevelDelta[nodeid]);
//							needToSync = checkForViolationSelfJoin(vL2Normalized, diNodes[nodeid], firstLevelDelta[nodeid], stayHigherThanThis, stayLowerThanThis);
//							if (needToSync[0]||needToSync[1])
//								System.err.println("Problem");

						}
//						lib.multVectorNoCloning(diSums, 1d/retrievedUpToNow.size());
//						if (lib.compareSignificantDifference(diSums, new double[][]{partialAvgD0, partialAvgD1}))
//							System.err.println("Problemo");
						nodesCausingViolation.clear();
					}
				}

				if (!nodesCausingViolation.isEmpty()) { // then i really need to sync 
					syncsFull++;
					cmRetrieved+=2*numberOfNodes;
					// partialAvgD0, partialAvgD1 are already collected and computed
					for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
						intelligentMsgs+=2; // request for the cm, and collect it
						intelligentMsgsUpload++; // ignore request
						intelligentTV+=w*d*singleCounterSizeInKbytes+singleBitInKbytes;  // the request type and the cm 
						intelligentTV+=w*d*singleCounterSizeInKbytes;  // the average 
						intelligentTVUpload+=w*d*singleCounterSizeInKbytes;  // ignore request and the average results which will be broadcasted from coordinator
						
						// get cm-sketch
//						double intTVOptimal = lib.computeOptimalTV(lastSyncedSketchesEstimations[nodeid], sketches[nodeid].getMaintainedEstimations(0));
//						intelligentTV+= 2d/8/1024 + intTVOptimal/1024d; //sync request, count-min values
						// since synchronization happens, reset the lastSyncedSketchesEstimations
						lastSyncedSketchesEstimations[nodeid] = lib.deepClone(sketches[nodeid].getMaintainedEstimations(0));
						// delta becomes 0
						firstLevelDelta[nodeid] = new double[2][d];
					}
					// avg
					double[][] newAvgs =  AverageForAllNodes(lastSyncedSketchesEstimations);
//					double tvToSendBack = lib.computeOptimalTV(lastMergedSketch, newAvgs);
//					intelligentTV+=(tvToSendBack*numberOfNodes/1024d); // new values back to nodes
//					intelligentMsgs+=3*numberOfNodes; // Sync request, count-min, newSyncedValues
					lastMergedSketch = newAvgs;
					// also compute diff between last and pre-last
					vL2 = new double[d]; for (int c=0;c<d;c++) vL2[c]=computeL2NormOfRow(c,lastMergedSketch);
					vL2Normalized = lib.multVector(vL2, 1d/numberOfNodes);
					
					lastFSynced = Double.MAX_VALUE;
					for (double dd:vL2) lastFSynced = Math.min(dd, lastFSynced);
					lastFSynced = lib.square(lastFSynced);	
					mergedSketchChanged=true;
				}
				lastEstSJ=lastFSynced;
				if (monitorError && i%freqErrorCheckReverse==0) {
					ErrorDivisor++;
					// now the relative error
					int startTimeWithCurrentTime = currentTime - queryLength;
					double sqNumberOfNodes = lib.square(numberOfNodes);
					
					// first compute error comparing to a centralized ECM
					double numberOfEventsWithinRange = ehNumberOfEvents.getEstimationRealtime(startTimeWithCurrentTime);
					double groundTruthECM = globalSketchGroundTruth.getInnerProduct(startTimeWithCurrentTime)/sqNumberOfNodes;
					double relError = Math.abs(lastFSynced - groundTruthECM)/(lib.square(numberOfEventsWithinRange/numberOfNodes));
					avgRelativeError += relError; // this is for ALL queries
					maxRelativeError = Math.max(maxRelativeError, relError);
					
					// then comparing to actual ground truth
					double groundTruthActual = groundTruthSJ/sqNumberOfNodes;
					double relErrorAccurate = Math.abs(lastFSynced-groundTruthActual)/lib.square(numberOfEventsAccurate/numberOfNodes);
					avgrelativeErrorAccurate += relErrorAccurate;
					maxRelativeErrorAccurate = Math.max(maxRelativeErrorAccurate, relErrorAccurate);
					absoluteErrorAccurate += Math.abs(lastFSynced-groundTruthActual);
				}
				lastEventTime=currentTime;
			}
			double naivetv = computeTransferVolume(naiveMsgs, naiveMsgs * messageUsefulPayloadInKbytes);
			double inttv = computeTransferVolume(intelligentMsgs, intelligentTV);
			double inttvupload = computeTransferVolume(intelligentMsgsUpload, intelligentTVUpload);
			if (ErrorDivisor == 0)
				ErrorDivisor = 1;
			double[] repetitionResults = new double[13];
			int pos = 0;
/* 0*/		repetitionResults[pos++] = accuracy;  
/* 1*/		repetitionResults[pos++] = queryLength;
/* 2*/		repetitionResults[pos++] = naiveMsgs;
/* 3*/		repetitionResults[pos++] = naivetv;
/* 4*/		repetitionResults[pos++] = intelligentMsgs;
/* 5*/		repetitionResults[pos++] = inttv;
/* 6*/		repetitionResults[pos++] = avgRelativeError / ErrorDivisor; // avg relative error
/* 7*/		repetitionResults[pos++] = maxRelativeError;                           // max relative error
/* 8*/		repetitionResults[pos++] = avgrelativeErrorAccurate / ErrorDivisor; // avg relative error Accurate
/* 9*/		repetitionResults[pos++] = maxRelativeErrorAccurate;                           // max relative error Accuarte
/*10*/		repetitionResults[pos++] = absoluteErrorAccurate / ErrorDivisor;                           // absolute error Accuarte
/*11*/		repetitionResults[pos++] = intelligentMsgsUpload;                           // only msgs from peers to coordiator
/*12*/		repetitionResults[pos++] = inttvupload;                           // only msgs from peers to coordiator

			// and query specific now
			
			for (int c = 0; c < repetitionResults.length; c++) {
				if (c!=7 && c!=9)
					allResults[c] += repetitionResults[c] / repeats;
				else
					allResults[c] = Math.max(allResults[c], repetitionResults[c]);
			}
		}

		resultsContinuousSJ rc = new resultsContinuousSJ(streamPath, tests.strSlidingWindowType(structureType), queryLength, epsilon, delta, numberOfNodes, numberOfNodes, 0, numberOfEvents, allResults);
		return rc;
	}
	
	/*/
	private static boolean[] checkForViolation(int numberOfNodes, slidingCMSketch[] sketches, double[][][] lastSyncedSketchesEstimations, double[][][] firstLevelDelta, boolean mergedSketchChanged,
			double[] vL2Normalized, double[][][] diNodes, double[][][] diNodesWithDelta, double stayHigherThanThis, double stayLowerThanThis,
			HashSet<Integer> nodesCausingViolation) {
		boolean[] needToSync=new boolean[2];
		int d = vL2Normalized.length;
		for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
			if (!(sketches[nodeid].getChangedSinceLast(0) || mergedSketchChanged )) continue;
			
			double[][] diQtbiQtbf = lib.sqrt(lib.computePosNegInnerProductPerRow(lib.subtractTwoVectors2d(sketches[nodeid].getMaintainedEstimations(0),lastSyncedSketchesEstimations[nodeid])));

			diNodes[nodeid] = diQtbiQtbf;  // 2 x d-dimensional - one per row, 0 for TBI, 1 for TBF
			diNodesWithDelta[nodeid][0] = lib.addTwoVectors(firstLevelDelta[nodeid][0],diNodes[nodeid][0]); // decreased --> TBI - bound from below
			diNodesWithDelta[nodeid][1] = lib.addTwoVectors(firstLevelDelta[nodeid][1],diNodes[nodeid][1]); // increased --> TBF - bound from above

			// check violation from below - Q_tbi or Q_l
			double minVal = Double.MAX_VALUE;
			for (int c=0;c<d;c++) {
				double val = vL2Normalized[c]-diNodesWithDelta[nodeid][0][c];
				minVal=Math.min(minVal,val);
			}
			if (minVal<stayHigherThanThis) {
				needToSync[0]=true;
				nodesCausingViolation.add(nodeid);
			}
			// check violation from above - Qtbf - convex NON-admissible region-->trick	
			double[] halfDiNodesWithDelta = lib.multVector(diNodesWithDelta[nodeid][1], 0.5);
			double[] center = lib.addTwoVectors(vL2Normalized, halfDiNodesWithDelta);
			double radius = lib.computeL2(halfDiNodesWithDelta);
			double minRadius = lib.computeMinDistanceFromInadmissibleRegion(false, center, stayLowerThanThis);
			if (radius>minRadius) {// violation
				needToSync[1]=true;
				nodesCausingViolation.add(nodeid);
			}
		}
		return needToSync;
	}	
	/*/
	

	private static double[] findNearestPointOfInadmissibleRegion(double stayLowerThanThis, double[] vL2Normalized) {
		double[] nearestPoint = new double[vL2Normalized.length];
		for (int i=0;i<vL2Normalized.length;i++) {
			if (vL2Normalized[i]>=stayLowerThanThis) 
				nearestPoint[i] = vL2Normalized[i]; 
			else 
				nearestPoint[i] = stayLowerThanThis;
		}
		return nearestPoint;
	}
	
	public static double[][] findOrthogonalHyperplane(double[] vL2Normalized, double[] nearestPoint) {
		double[] point = new double[vL2Normalized.length];
		double[] coefficients = new double[vL2Normalized.length];
		for (int i =0;i<vL2Normalized.length;i++) {
			coefficients[i] = nearestPoint[i] - vL2Normalized[i];
			point[i] = nearestPoint[i];
		}
		// check that i am in the positive of the hyperplane
//		double v =0;
//		for (int i=0;i<vL2Normalized.length;i++) {
//			v += coefficients[i]*(vL2Normalized[i]-nearestPoint[i]);
//		}
//		if (v>0)
//			System.err.println("Nevernevernever");
		return new double[][]{coefficients,point};		
	}
	public static boolean checkIfStillNegative(double[]point,double[][]hyperPlane) {
		double[] coefficients = hyperPlane[0];
		double[] p = hyperPlane[1];
		double v = 0;

		for (int i=0;i<point.length;i++) {
			v += coefficients[i]*(point[i]-p[i]);
		}
		return v<0;
	}
	
	private static boolean[] checkForViolationSelfJoin(double[] vL2Normalized, double[][] di, double[][] delta, double stayHigherThanThis, double stayLowerThanThis) {
//		boolean[] test = checkForViolationSelfJoinWithSphere(vL2Normalized, di, delta, stayHigherThanThis, stayLowerThanThis);
		boolean[] test =checkForViolationSelfJoinWithSafeZone(vL2Normalized, di, delta, stayHigherThanThis, stayLowerThanThis);
		return test;
	}
	private static boolean[] checkForViolationSelfJoinWithSafeZone(double[] vL2Normalized, double[][] di, double[][] delta, double stayHigherThanThis, double stayLowerThanThis) {
		boolean[] needToSync=new boolean[2];
		int d = di.length;
		// check violation from below - Q_tbi or Q_l
		double minVal = Double.MAX_VALUE;
		double[][] diNodesWithDelta=new double[2][d];
		diNodesWithDelta[0]=lib.addTwoVectors(di[0], delta[0]);
		diNodesWithDelta[1]=lib.addTwoVectors(di[1], delta[1]);
		
		for (int c=0;c<d;c++) {
			double val = vL2Normalized[c]-diNodesWithDelta[0][c];
			minVal=Math.min(minVal,val);
		}
		if (minVal<stayHigherThanThis) {
			needToSync[0]=true;
		}

		// check violation from above - Qtbf - convex NON-admissible region--> safe zone
		double[] nearestPointOfInadmissibleRegion = findNearestPointOfInadmissibleRegion(stayLowerThanThis, vL2Normalized);
		double[][] orthogonalHyperplane = findOrthogonalHyperplane(vL2Normalized,nearestPointOfInadmissibleRegion);
		boolean stillNegative = checkIfStillNegative(lib.addTwoVectors(vL2Normalized, diNodesWithDelta[1]), orthogonalHyperplane);
		needToSync[1]=!stillNegative;
		return needToSync;
	}
	
	private static boolean[] checkForViolationSelfJoinWithSphere(double[] vL2Normalized, double[][] di, double[][] delta, double stayHigherThanThis, double stayLowerThanThis) {
		boolean[] needToSync=new boolean[2];
		int d = di.length;
		// check violation from below - Q_tbi or Q_l
		double minVal = Double.MAX_VALUE;
		double[][] diNodesWithDelta=new double[2][d];
		diNodesWithDelta[0]=lib.addTwoVectors(di[0], delta[0]);
		diNodesWithDelta[1]=lib.addTwoVectors(di[1], delta[1]);
		
		for (int c=0;c<d;c++) {
			double val = vL2Normalized[c]-diNodesWithDelta[0][c];
			minVal=Math.min(minVal,val);
		}
		if (minVal<stayHigherThanThis) {
			needToSync[0]=true;
		}

		// check violation from above - Qtbf - convex NON-admissible region-->trick	
		double[] halfDiNodesWithDelta = lib.multVector(diNodesWithDelta[1], 0.5);
		double[] center = lib.addTwoVectors(vL2Normalized, halfDiNodesWithDelta);
		double radius = lib.computeL2(halfDiNodesWithDelta);
		double minRadius = lib.computeMinDistanceFromInadmissibleRegion(false, center, stayLowerThanThis);
		if (radius>minRadius) {// violation
			needToSync[1]=true;
		}
		/*/
		Tropos 2 - mallon lathos
			double[] center = lib.multVector(lib.addTwoVectors(vL2Normalized, diNodesWithDelta[1]),0.5);
			double[] radiusVector = lib.multVector(lib.subtractTwoVectors(vL2Normalized, diNodesWithDelta[1]),0.5);
			double radius=computeL2NormOfVector(radiusVector);
			double minRadius = lib.computeMinDistanceFromInadmissibleRegion(false, center, stayLowerThanThis);
	
		if (radius>minRadius) {// violation
			needToSync[1]=true;
		}
		/*/

		return needToSync;
	}

	/*/
	static resultsContinuousSJ monitorSelfJoinQueryOld(int numberOfNodes, long numberOfEvents, ProcessedStreamLoaderGeneric psl, double delta, double epsilon, int windowSize, 
				structure.slidingCMSketch.sliding_window_structures structureType, Random rn, boolean debug, int repeats, int[] queryLengths, 
				double[] accuracies, String streamPath) {
		System.err.println("cleverSync is " + cleverSync);
		double[] allResults = new double[7+4*queryLengths.length];

		for (int repeat = 0; repeat < repeats; repeat++) {
			int syncsDi=0, syncsFull=0;
			ExponentialHistogramCircularInt ehNumberOfEvents = new ExponentialHistogramCircularInt(0.0001, windowSize, 0);
			boolean monitorError = false;
			double[] maxRelativeErrors = new double[queryLengths.length];
			double maxRelativeError = -Double.MAX_VALUE;
			double[] relativeErrors = new double[queryLengths.length];
			double[] relativeErrorDivisors = new double[queryLengths.length]; // number of queries to be executed
			double avgRelativeError = 0, avgRelativeErrorDivisor = 0;

			final double messageUsefulPayloadInKbytes = (32 + 32) / (8 * 1024d); // time, itemid --> kbytes
			double naiveMsgs = 0;
			double intelligentMsgs = 0;
			double intelligentTV = 0;
			slidingCMSketch[] sketches = new slidingCMSketch[numberOfNodes]; // running sketches - time is currentTime
			for (int i = 0; i < numberOfNodes; i++)
				sketches[i] = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat);
			// lastSyncedSketches and mergedSketch are count-min sketches

			slidingCMSketch globalSketchGroundTruth = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat);
			globalSketchGroundTruth.setMaintainInnerJoin(queryLengths);

			int w = globalSketchGroundTruth.getW();
			int d = globalSketchGroundTruth.getD();
			// [nodeid][queryid][w][d]
			double[][][][] lastSyncedSketchesEstimations = new double[numberOfNodes][queryLengths.length][w][d];
			// [nodeid][queryid][0:up, 1:down][d]
			double[][][][] firstLevelDelta = new double[numberOfNodes][queryLengths.length][2][d]; // 0 for up, 1 for down
			double[][][] lastMergedSketch = new double[queryLengths.length][w][d];
			
			boolean[] mergedSketchChanged = new boolean[queryLengths.length];
			for (int i = 0; i < mergedSketchChanged.length; i++)
				mergedSketchChanged[i] = true;

			for (int i = 0; i < numberOfNodes; i++) {
				sketches[i] = new slidingCMSketch(delta, epsilon, windowSize, numberOfEvents, structureType, repeat);
				sketches[i].setMaintainInnerJoin(queryLengths); // this is only to maintain the sliding window counter estimations at separate count-min sketches, for performance reasons

				for (int queryId = 0; queryId < queryLengths.length; queryId++)
					lastSyncedSketchesEstimations[i][queryId] = new double[w][d];//extractEstimations(sketches[i], queryLengths[queryId]);
			}

			double[] lastFSynced= new double[queryLengths.length]; // this is f(v_0)
			for (int queryId = 0; queryId < queryLengths.length; queryId++)
				lastFSynced[queryId] = 0;

			boolean initialRun = true;
			int i = 0;			
			
			int lastEventTime=-1;
			double[] vL2 = new double[d];
			double[] vL2Normalized = new double[d];

			while (true) {
				i++;
				if (i == trainingPeriod && initialRun) {// numberOfEvents/10) { // reset all costs
					naiveMsgs = 0;
					intelligentMsgs = 0;
					intelligentTV = 0;
					System.err.println("\nStarting normal count");
					i = 0;
					initialRun = false;
					monitorError = true;
				}

				logEventInt singleEvent = psl.readNextEvent(numberOfNodes);
				if (i % 1000000 == 0 && i!=0 && !initialRun) {
					double naivetv = computeTransferVolume(naiveMsgs, naiveMsgs * messageUsefulPayloadInKbytes);
					double inttv = computeTransferVolume(intelligentMsgs, intelligentTV);
					System.err.println("\n<<Round," + i + ",Nodes," + numberOfNodes + //",STREAM_PATH," + streamPath + 
							",delta," + delta + ",epsilon," + epsilon + ",windowSize," + windowSize
							+ ",naiveMsgs," + naiveMsgs + ",naiveTV," + naivetv + ",intMsgs," + intelligentMsgs + ",intTV," + inttv + ",relError,"
							+ (avgRelativeError / Math.max(1, avgRelativeErrorDivisor)) + ",maxRelError," + maxRelativeError + ",ticks," + avgRelativeErrorDivisor);
					System.err.println("Syncs " + syncsDi + "," + syncsFull  + " LastTime " + singleEvent.seconds);

				}

				if (singleEvent == null)
					break;
				int currentTime = singleEvent.getTime();

				int sketchid = singleEvent.streamid;
				slidingCMSketch sketch = sketches[sketchid];

				int[] hashes = sketch.add(singleEvent.getEvent(), singleEvent.getTime());
				ehNumberOfEvents.addAOne(singleEvent.getTime());

				globalSketchGroundTruth.add(singleEvent.getEvent(), singleEvent.getTime(), hashes); // only for verification that estimation quality is achieved
				naiveMsgs++;

				if (lastEventTime<currentTime) {
					for (int nodeid=0;nodeid<numberOfNodes;nodeid++)
						if (nodeid!=sketchid)
							sketches[nodeid].tick(currentTime);
					lastEventTime=currentTime;
				}
				double[][][][] diNodes = new double[queryLengths.length][numberOfNodes][2][]; // 0 for up, 1 for down
				double[][][][] diNodesWithDelta = new double[queryLengths.length][numberOfNodes][2][]; // 0 for up, 1 for down

				boolean[][] needToSync = new boolean[queryLengths.length][2];
				// now execute the GM
				for (int queryId = 0; queryId < queryLengths.length; queryId++) {
					// check
					// TBI: To become infrequent, TBF: To become frequent
					double theta = accuracies[queryId];
					double stayHigherThanThis = Math.sqrt(lastFSynced[queryId]/(1d+theta))/numberOfNodes; // needs to stay higher than this - TBI
					double stayLowerThanThis  = Math.sqrt(lastFSynced[queryId]/(1d-theta))/numberOfNodes; // needs to stay lower  than this - TBF
					HashSet<Integer> nodesCausingViolation=new HashSet<>();
					for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
						if (!(sketches[nodeid].getChangedSinceLast(queryId) || mergedSketchChanged[queryId] )) continue;
						
						double[][] diQtbiQtbf = lib.sqrt(lib.computePosNegInnerProductPerRow(lib.subtractTwoVectors2d(sketches[nodeid].getMaintainedEstimations(queryId),lastSyncedSketchesEstimations[nodeid][queryId])));

						diNodes[queryId][nodeid] = diQtbiQtbf;
						diNodesWithDelta[queryId][nodeid][0]=lib.addTwoVectors(firstLevelDelta[nodeid][queryId][0],diNodes[queryId][nodeid][0]); // decreased --> TBI - bound from below
						diNodesWithDelta[queryId][nodeid][1]=lib.addTwoVectors(firstLevelDelta[nodeid][queryId][1],diNodes[queryId][nodeid][1]); // increased --> TBF - bound from above

						// first check Qtbf - convex NON-admissible region-->trick	
						double[] halfDiNodesWithDelta = lib.multVector(diNodesWithDelta[queryId][nodeid][1], 0.5);
						double[] center = lib.addTwoVectors(vL2Normalized, halfDiNodesWithDelta);
						double radius = lib.computeL2(halfDiNodesWithDelta);
						double minRadius = lib.computeMinDistanceFromInadmissibleRegion(false, center, stayLowerThanThis);
						if (radius>minRadius) {// violation
							needToSync[queryId][1]=true;
							nodesCausingViolation.add(nodeid);
						}
						// also check violation from below - Q_tbi or Q_l
						double minVal = Double.MAX_VALUE;
						for (int c=0;c<d;c++) {
							double val = vL2Normalized[c]-diNodesWithDelta[queryId][nodeid][0][c];
							minVal=Math.min(minVal,val);
						}
						if (minVal<stayHigherThanThis) {
							needToSync[queryId][0]=true;
							nodesCausingViolation.add(nodeid);
						}
					}
					
					HashSet<Integer> retrievedUpToNow = new HashSet<>();
					if (needToSync[queryId][0] || needToSync[queryId][1]) { // start from the peers causing the violation, and expand
						double[] partialAvgD = new double[d];
						for (int i:nodesCausingViolation) {
							retrievedUpToNow.add(i);
							partialAvgD = lib.addTwoVectors(partialAvgD, lib.addtwove);
						}
						if (retrievedUpToNow.size()>1) {
							// now check average

						}
						lib.getShuffle(numberOfNodes);

						lib.setStartAndShuffleRest(nodesCausingViolation,numberOfNodes);
					}

					boolean syncFirst=needToSync[queryId][0]; // threshold violation: to become infrequent
					boolean syncSec  =needToSync[queryId][1]; // threshold violation: to become frequent
					
					
					if (needToSync[queryId][0] || needToSync[queryId][1]) {
						// first sync the d_i
						mergedSketchChanged[queryId]=true;
						double[] dNewQtbi = new double[d],dNewQtbf=new double[d];
						syncsDi++;
					
						if (syncFirst) { // Q_tbi only
							needToSync[queryId][0] = false;
							for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
								if (diNodes[queryId][nodeid][0]==null) {
									double[][] diQtbiQtbf = lib.sqrt(lib.computePosNegInnerProductPerRow(lib.subtractTwoVectors2d(sketches[nodeid].getMaintainedEstimations(queryId),lastSyncedSketchesEstimations[nodeid][queryId])));
									diNodes[queryId][nodeid] = diQtbiQtbf;
									diNodesWithDelta[queryId][nodeid][0]=lib.addTwoVectors(firstLevelDelta[nodeid][queryId][0],diNodes[queryId][nodeid][0]);
									diNodesWithDelta[queryId][nodeid][1]=lib.addTwoVectors(firstLevelDelta[nodeid][queryId][1],diNodes[queryId][nodeid][1]);
								}
							}
							dNewQtbi = new double[d];
							for (int nodeid=0;nodeid<numberOfNodes;nodeid++) 
								dNewQtbi=lib.addTwoVectors(diNodesWithDelta[queryId][nodeid][0], dNewQtbi);
							dNewQtbi = lib.divVector(dNewQtbi, numberOfNodes); 
							// due to the way diNodesWithDelta are computed, it cannot be that dNewQl has a negative component. It either has a positive one or a 0

							
							double[] dNew = dNewQtbi;
							intelligentTV += 2/8d/1024 + numberOfNodes*d*32/8d/1024d; //sync request (2bit), d_i values
							intelligentMsgs+=(2*numberOfNodes);// request, response
							// check if dNew causes a violation
							double[] center = lib.addTwoVectors(vL2Normalized, lib.multVector(dNew, 0.5));
							double radius = lib.computeL2(lib.multVector(dNew, 0.5));
							double minRadius = lib.computeMinDistanceFromInadmissibleRegion(false, center, upperBoundThreshold);
							if (radius>minRadius) {// STILL violation
								needToSync[queryId][0]=true;
							} // Fixing violation of Ql cannot cause a new violation for Qu because dNewQl cannot have a negative component
						}
						
						if (syncSec && !needToSync[queryId][0]) { // Q_u only : violation from below, and up to now i don't need to fully resync
							needToSync[queryId][1] = false;
							for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
								if (diNodes[queryId][nodeid][1]==null) {
									double[][] diQlQu = lib.sqrt(lib.computePosNegInnerProductPerRow(lib.subtractTwoVectors2d(sketches[nodeid].getMaintainedEstimations(queryId),lastSyncedSketchesEstimations[nodeid][queryId])));
									diNodes[queryId][nodeid] = diQlQu;
									diNodesWithDelta[queryId][nodeid][0]=lib.addTwoVectors(firstLevelDelta[nodeid][queryId][0],diNodes[queryId][nodeid][0]);
									diNodesWithDelta[queryId][nodeid][1]=lib.addTwoVectors(firstLevelDelta[nodeid][queryId][1],diNodes[queryId][nodeid][1]);
								}
							}
							dNewQu = new double[d];
							for (int nodeid=0;nodeid<numberOfNodes;nodeid++) dNewQu=lib.addTwoVectors(diNodesWithDelta[queryId][nodeid][1], dNewQu);
							dNewQu = lib.divVector(dNewQu, numberOfNodes); 
							// due to the way diNodesWithDelta are computed, it cannot be that dNewQl has a negative component. It either has a positive one or a 0

							double[] dNew = dNewQu;
							intelligentTV += 2/8d/1024 + numberOfNodes*d*32/8d/1024d; //sync request (2bit), d_i values
							if (!syncFirst) // otherwise i can save msgs by aggregating both d_upper and d_lower
								intelligentMsgs+=(2*numberOfNodes);// request, response
							// check if dNew still causes a violation
							double maxVal=-Double.MAX_VALUE;
							for (int c=0;c<d;c++) {
								double val = numberOfNodes*dNew[c] - vL2[c];
								maxVal=Math.max(maxVal, val);
							}
							maxVal=-maxVal;
							if (maxVal<stayHigherThanThis) {
								needToSync[queryId][1]=true;
							}
						}
						
						if (!needToSync[queryId][0] && !needToSync[queryId][1]) { //balance succeeded
							for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
								firstLevelDelta[nodeid][queryId][0] = lib.subtractTwoVectors(dNewQu,diNodes[queryId][nodeid][0]);
								firstLevelDelta[nodeid][queryId][1] = lib.subtractTwoVectors(dNewQl,diNodes[queryId][nodeid][1]);
							}
							if (syncFirst) intelligentTV += numberOfNodes*d*32/8d/1024d; //dnew values
							if (syncSec) intelligentTV += numberOfNodes*d*32/8d/1024d; //dnew values
							intelligentMsgs+=numberOfNodes;

						} else { // sync on d did not help, full sync needed
							 // i still need to get everything
							syncsFull++;
//								System.err.println("2ndLevelSync at round "+ i);
							for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
								firstLevelDelta[nodeid][queryId] = new double[2][d]; // reset the deltas
								double intTVOptimal = lib.computeOptimalTV(lastSyncedSketchesEstimations[nodeid][queryId], sketches[nodeid].getMaintainedEstimations(queryId));
								
								intelligentTV+= 2d/8/1024 + intTVOptimal/1024d; //sync request, count-min values
								// since synchronization happens, reset the lastSyncedSketchesEstimations
								lastSyncedSketchesEstimations[nodeid][queryId] = lib.deepClone(sketches[nodeid].getMaintainedEstimations(queryId));
							}
							double[][][] newAvgs =  AverageForAllNodes(lastSyncedSketchesEstimations);
							double tvToSendBack = lib.computeOptimalTV(lastMergedSketch[queryId], newAvgs[queryId]);
							intelligentTV+=(tvToSendBack*numberOfNodes/1024d); // new values back to nodes
							intelligentMsgs+=3*numberOfNodes; // Sync request, count-min, newSyncedValues
							lastMergedSketch = newAvgs;
							// also compute diff between last and pre-last
							vL2 = new double[d]; for (int c=0;c<d;c++) vL2[c]=computeL2NormOfRow(c,lastMergedSketch[queryId]);
							vL2Normalized = lib.multVector(vL2, 1d/numberOfNodes);
							
							lastFSynced[queryId] = Double.MAX_VALUE;
							for (double dd:vL2) lastFSynced[queryId] = Math.min(dd, lastFSynced[queryId]);
							lastFSynced[queryId] = lib.square(lastFSynced[queryId]);
							for (int nodeid=0;nodeid<numberOfNodes;nodeid++) {
								firstLevelDelta[nodeid][queryId][0] = new double[d];
								firstLevelDelta[nodeid][queryId][1] = new double[d];
							}
						} 
					
					}
				}

				if (monitorError) {
					// now the relative error
					for (int queryId = 0; queryId < queryLengths.length; queryId++) {
						int startTimeWithCurrentTime = currentTime - queryLengths[queryId];
						double numberOfEventsWithinRange = ehNumberOfEvents.getEstimationRealtime(startTimeWithCurrentTime);
						double groundTruth = globalSketchGroundTruth.getInnerProduct(startTimeWithCurrentTime)/lib.square(numberOfNodes);
						double error = Math.abs(lastFSynced[queryId] - groundTruth);
						double relError = error/(numberOfEventsWithinRange*numberOfEventsWithinRange);
						relativeErrors[queryId] += relError;
						relativeErrorDivisors[queryId]++;
						maxRelativeErrors[queryId] = Math.max(maxRelativeErrors[queryId], relError);
						maxRelativeError = Math.max(maxRelativeError, relError); // this is for ALL queries
						avgRelativeError += relError; // this is for ALL queries
						avgRelativeErrorDivisor++;
					}
				}
				lastEventTime=currentTime;
			}
			double naivetv = computeTransferVolume(naiveMsgs, naiveMsgs * messageUsefulPayloadInKbytes);
			double inttv = computeTransferVolume(intelligentMsgs, intelligentTV);
			if (avgRelativeErrorDivisor == 0)
				avgRelativeErrorDivisor = 1;
			double[] repetitionResults = new double[7 + 4 * queryLengths.length];
			int pos = 0;
			repetitionResults[pos++] = naiveMsgs;
			repetitionResults[pos++] = naivetv;
			repetitionResults[pos++] = intelligentMsgs;
			repetitionResults[pos++] = inttv;
			repetitionResults[pos++] = avgRelativeError / avgRelativeErrorDivisor;
			repetitionResults[pos++] = avgRelativeErrorDivisor;
			repetitionResults[pos++] = maxRelativeError;

			for (int queryId = 0; queryId < queryLengths.length; queryId++) {
				if (relativeErrorDivisors[queryId] == 0)
					relativeErrorDivisors[queryId] = 1;
				repetitionResults[pos++] = accuracies[queryId];
				repetitionResults[pos++] = queryLengths[queryId];
				repetitionResults[pos++] = relativeErrors[queryId] / relativeErrorDivisors[queryId];
				repetitionResults[pos++] = maxRelativeErrors[queryId];
			}
			for (int c = 0; c < repetitionResults.length; c++) {
				if (c!=6 && (c<6 || (c-6)%4!=0))
					allResults[c] += repetitionResults[c] / repeats;
				else
					allResults[c] = Math.max(allResults[c], repetitionResults[c]);
			}
		}
		resultsContinuousSJ rc = new resultsContinuousSJ(streamPath, tests.strSlidingWindowType(structureType), epsilon, delta, numberOfNodes, numberOfNodes, 0, numberOfEvents, allResults);
		return rc;
	}
	
	/*/
	public static double[] averageDis(double[][][] dis, int queryId) {
		// [numberOfNodes][queryLengths.length][d];
		int d = dis[0][0].length;
		double[] avgD = new double[d];
		for (int i=0;i<d;i++)
			for (int nodeid=0;nodeid<dis.length;nodeid++)
				avgD[i]+=dis[nodeid][queryId][i];
		for (int i=0;i<d;i++)
			avgD[i]/=dis.length;
		return avgD;
	}
	
	public static HashSet<Integer> xorSlow(HashSet<Integer>first, HashSet<Integer>second) {
		HashSet<Integer> small, large;
		if (first.size()<second.size())  {small = first; large=second;}
		else {small=second;large=first;}
		
		HashSet<Integer> intersection = new HashSet<>(large);
		intersection.retainAll(small); // now xor contains the intersection
		
		HashSet<Integer> union = new HashSet<>(large);
		union.addAll(small);
		
		union.removeAll(intersection);
		return union;
	}
        
	public static HashSet<Integer> xor(HashSet<Integer>first, HashSet<Integer>second) {
            HashSet<Integer> small, large;
            if (first.size()<second.size()) {
                small=first;large=second;
            } else {
                small=second;large=first;
            }
            HashSet<Integer> xor = new HashSet<>();
            for (int i:small) {
                if (large.contains(i))
                    large.remove(i);
                else
                    xor.add(i);
            }
            xor.addAll(large);
            return xor;
	}
	
	public static HashSet<Integer>[] AndXor(HashSet<Integer>first, HashSet<Integer>second) {
        HashSet<Integer> small, large;
        if (first.size()<second.size()) {
            small=first;large=second;
        } else {
            small=second;large=first;
        }
        HashSet<Integer> and = new HashSet<>();
        HashSet<Integer> xor = new HashSet<>();
        for (int i:small) {
            if (large.contains(i)) {
                large.remove(i);
                and.add(i);
            }
            else {
                xor.add(i);
            }
        }
        xor.addAll(large);
        return new HashSet[] {and,xor};
}

	

	static double[][] extractEstimations(slidingCMSketch cm, int queryRange) {
		double[][] array = new double[cm.getW()][cm.getD()];
		for (int wcnt=0;wcnt<array.length;wcnt++)
			for (int dcnt=0;dcnt<array[0].length;dcnt++) {
				array[wcnt][dcnt] = cm.array[wcnt][dcnt].getEstimationRange(queryRange);
			}
		return array;
	}
	
//	static void updateEstimations(slidingCMSketch cm, int w, int d, int queryRange,int queryId) {
//		cm.estimations[queryId][w][d] = cm.array[w][d].getEstimationRange(queryRange);
//	}
	
	

	// all input-output is in kbytes
	static double computeTransferVolume(double numberOfMessages, double usefulPayload) {
		// 20-byte header + usefulPayload bytes data
		return numberOfMessages*40d/1024d + usefulPayload;
	}
	
	public distributedMonitoringSendCM() {}

	
}
