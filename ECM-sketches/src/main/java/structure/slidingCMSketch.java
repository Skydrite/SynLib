package structure;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.TreeMap;

public class slidingCMSketch {
	
	boolean []changedSinceLast=null;
	boolean maintainInnerJoin=false;
	int[] queryLengths=null;
	int lastUpdateTime =0;
	final static double verySmallNumber=1e-5;
	final int w, d; // w=mod, d=levels
	public final slidingwindow[][] array;
	public enum sliding_window_structures { NULL,EC, DW, RW} // EC is exponentialHistogramCircular, DW is detWaveOptDeque, RW is compositeRandWaveDeque
	double delta_sw;
	long numberOfEvents; // used only for DW, not for exponential histograms!
	
	double[][][]estimations;

	long numberOfInvokesToGetEstimation=0;
	public boolean getChangedSinceLast(int queryId) {
		return changedSinceLast[queryId];
	}
	public void setChangedSinceLast(int queryId, boolean t) {
		changedSinceLast[queryId]=t;
	}
	public void extractEstimations(int queryId, int queryRange) {
		slidingCMSketch cm = this;
		double[][] array = new double[cm.getW()][cm.getD()];
		for (int wcnt=0;wcnt<array.length;wcnt++)
			for (int dcnt=0;dcnt<array[0].length;dcnt++) {
				array[wcnt][dcnt] = cm.array[wcnt][dcnt].getEstimationRange(queryRange);
			}
		cm.estimations[queryId] = array;
	}
	public void prepareForQuerying() {
	}
	
	boolean preparedAlready=false;
	
	public void setMaintainInnerJoin(int[] queryLengths) {
		this.queryLengths=queryLengths;
		this.maintainInnerJoin=true;
		estimations=new double[queryLengths.length][w][d];
		maintainedInnerJoins=new double[queryLengths.length][d];
		changedSinceLast = new boolean[queryLengths.length]; for (int i=0;i<queryLengths.length;i++) changedSinceLast[i] = true;
		expirationTimesOfBucketsForAllQueries = new TreeMap[queryLengths.length];
		liveInnerJoins = new double[queryLengths.length];
		for (int i=0;i<queryLengths.length;i++) expirationTimesOfBucketsForAllQueries[i] = new TreeMap<>();
	}
	
	public String toString() {
		double total=0;
		for (int i=0;i<array.length;i++) {
			for (int j=0;j<array[i].length;j++) {
				double est = array[i][j].getEstimationRealtime(0);
				total+=est;
			}
		}
		return "Total number of true bits is " + total;
	}
	
	public void setLastSyncedTime(int time) {
		for (int i=0;i<array.length;i++) {
			for (int j=0;j<array[i].length;j++) {
				array[i][j].setLastSyncedTime(time);
			}
		}
	}

	public slidingwindow[][] getArray() {
		return array;
	}
	public long getNumberOfEvents() {
		return numberOfEvents;
	}
	
	public void batchUpdate(StreamHD s) {
		for (EventHD e:s.getEvents()) 
			add(e.getEvent(), e.getTime());
		this.lastUpdateTime = s.getCurrentTime();
	}
	public int getW() {
		return w;
	}
	public int getD() {
		return d;
	}
	final sliding_window_structures SLIDING_WINDOW_TYPE;
	public sliding_window_structures getSlidingWindowType() {
		return SLIDING_WINDOW_TYPE;
	}
	
	public long [] getAlphas() {
		return alphas;
	}
	public long [] getBetas() {
		return betas;
	}
	public void setAlphas(long[]alphas) {
		this.alphas=alphas;
	}
	public void setBetas(long[]betas) {
		this.betas=betas;
	}
	
	public static double getEpsilonSWOld(sliding_window_structures SLIDING_WINDOW_TYPE, double epsilon) {
		if (SLIDING_WINDOW_TYPE==sliding_window_structures.RW)
			return (Math.sqrt(epsilon*epsilon+10d*epsilon+9)+epsilon-3d)/4d; 
		else 
			return Math.sqrt(epsilon+1)-1;
	}
	public static double getEpsilonCMOld(sliding_window_structures SLIDING_WINDOW_TYPE, double epsilon) {
		if (SLIDING_WINDOW_TYPE==sliding_window_structures.RW)
			return (3d*epsilon-Math.sqrt(epsilon*epsilon+10d*epsilon+9d)+3d)/(epsilon+1d+Math.sqrt(epsilon*epsilon+10d*epsilon+9d));
		else 
			return Math.sqrt(epsilon+1)-1;
	}
	
	public static double getEpsilonSW(sliding_window_structures SLIDING_WINDOW_TYPE, double epsilon) {
		return epsilon;
	}
	public static double getEpsilonCM(sliding_window_structures SLIDING_WINDOW_TYPE, double epsilon) {
		return epsilon/(1d+epsilon);
	}
	public static double getEpsilonSWIP(sliding_window_structures SLIDING_WINDOW_TYPE, double epsilon) {
		return Math.sqrt(epsilon+1)-1d;
	}
	
	public static double getEpsilonCMIP(sliding_window_structures SLIDING_WINDOW_TYPE, double epsilon) {
		return epsilon/(1d+epsilon);
	}	
	public double getEpsilonSW() {
		return epsilonSW;
	}

	double epsilonSW, epsilonCM;
	public void removeExpired(int expiryTime) {
		for (int i=0;i<w;i++)
			for (int j=0;j<d;j++)
				array[i][j].removeExpired(expiryTime);
	}
	
	public void cloneForQuerying() {
		for (int i=0;i<w;i++)
			for (int j=0;j<d;j++) {
				array[i][j].cloneForQuerying();
			}
	}
	
	public slidingCMSketch cloneStructure() {
		slidingCMSketch newCMSketch = new slidingCMSketch(this);
		return newCMSketch;
	}
	
	public slidingwindow getSlidingWindowAtLocation(int i, int j) {
		return array[i][j];
	}

	public int[] getDimensions() {
		return new int[]{w,d};
	}
	
	public slidingCMSketch(slidingCMSketch sketchToClone) {
		this.w=sketchToClone.w;
		this.d=sketchToClone.d;
		this.SLIDING_WINDOW_TYPE = sketchToClone.SLIDING_WINDOW_TYPE;
		this.array = new slidingwindow[w][d];
		for (int i=0;i<w;i++)
			for (int j=0;j<d;j++)
				this.array[i][j] = sketchToClone.array[i][j].clone();
		this.lastUpdateTime = sketchToClone.lastUpdateTime;	
		mostRecentExpiration = new MyLinkedList.Tuple[w*d];
	}
	
	boolean optimizeForSJ;
	public static boolean oldOptimization=false;
	public static boolean printedWarningOnce=false;
	public slidingCMSketch(double deltaTotal, double epsilon, int windowSize, long numberOfEvents, sliding_window_structures SLIDING_WINDOW_TYPE, int repeat, boolean optimizeForSJ) {
		final cern.jet.random.engine.MersenneTwister64 mt;
		mt = new cern.jet.random.engine.MersenneTwister64(repeat);
		this.numberOfEvents=numberOfEvents;
		this.SLIDING_WINDOW_TYPE=SLIDING_WINDOW_TYPE;
		this.optimizeForSJ=optimizeForSJ;
		if (optimizeForSJ) {
			epsilonSW=getEpsilonSWIP(SLIDING_WINDOW_TYPE, epsilon);
			epsilonCM=getEpsilonCMIP(SLIDING_WINDOW_TYPE, epsilon);
		} else {
			epsilonSW=getEpsilonSW(SLIDING_WINDOW_TYPE, epsilon);
			epsilonCM=getEpsilonCM(SLIDING_WINDOW_TYPE, epsilon);
		}
		
		if (oldOptimization) {
			if (!printedWarningOnce) {System.err.println("WARNING -------- Old optimization\nWARNING -------- Old optimization\nWARNING -------- Old optimization\nWARNING -------- Old optimization\nWARNING -------- Old optimization\nWARNING -------- Old optimization\n"); printedWarningOnce=true;}
			epsilonSW=getEpsilonSWOld(SLIDING_WINDOW_TYPE, epsilon);
			epsilonCM=getEpsilonCMOld(SLIDING_WINDOW_TYPE, epsilon);
		}
		w=(int)Math.ceil(Math.E/epsilonCM);
		double delta=deltaTotal;
		if (SLIDING_WINDOW_TYPE==sliding_window_structures.RW) {
			delta=deltaTotal/2;
		}
		d=(int)Math.ceil(Math.log(1d/delta));
		array = new slidingwindow[w][d];
		for (int i=0;i<w;i++) {
			for (int j=0;j<d;j++) {
				switch(SLIDING_WINDOW_TYPE) {
					case EC:
						array[i][j]=new ExponentialHistogramCircularInt(epsilonSW, windowSize, numberOfEvents);
						delta_sw=0;
						break;
					case DW:
						array[i][j]=new detWaveOptDeque(epsilonSW, windowSize, numberOfEvents);
						delta_sw=0;
						break;
					case RW:
						array[i][j]=new compositeRandWaveDeque(delta,epsilonSW, windowSize, numberOfEvents, (int) numberOfEvents/w);
						delta_sw=delta;
						break;
					default:
						System.err.println("Error - type cannot be handled!");
						break;
				}
			}
		}
		alphas=new long[d];
		betas=new long[d];
		for (int i=0;i<d;i++) {
			while (alphas[i]*betas[i]==0) {
				alphas[i]=Math.abs(mt.nextInt());
			    betas[i]=Math.abs(mt.nextInt());
			}
		}
		mostRecentExpiration = new MyLinkedList.Tuple[w*d];
	}

	public slidingCMSketch(double deltaTotal, double epsilon, int windowSize, long numberOfEvents, sliding_window_structures SLIDING_WINDOW_TYPE, int[] dimensions, int repeat) {
		final cern.jet.random.engine.MersenneTwister64 mt;
		mt = new cern.jet.random.engine.MersenneTwister64(repeat);
		this.numberOfEvents=numberOfEvents;
		this.SLIDING_WINDOW_TYPE=SLIDING_WINDOW_TYPE;
		epsilonSW=getEpsilonSW(SLIDING_WINDOW_TYPE, epsilon);
		epsilonCM=getEpsilonCM(SLIDING_WINDOW_TYPE, epsilon);
		double delta=deltaTotal;
		if (SLIDING_WINDOW_TYPE==sliding_window_structures.RW) {
			delta=deltaTotal/2;
		}
		
		w=dimensions[0];
		d=dimensions[1];
		array = new slidingwindow[w][d];
		for (int i=0;i<w;i++) {
			for (int j=0;j<d;j++) {
				switch(SLIDING_WINDOW_TYPE) {
				case EC:
					array[i][j]=new ExponentialHistogramCircularInt(epsilonSW, windowSize, numberOfEvents);
					delta_sw=0;
					break;
				case DW:
					array[i][j]=new detWaveOptDeque(epsilonSW, windowSize, numberOfEvents);
					delta_sw=0;
					break;
				case RW:
					array[i][j]=new compositeRandWaveDeque(delta,epsilonSW, windowSize, numberOfEvents, (int) numberOfEvents/w);
					delta_sw=delta;
					break;
				default:
					System.err.println("Error - type cannot be handled!");
					break;
				}
			}
		}
		alphas=new long[d];
		betas=new long[d];
		for (int i=0;i<d;i++) {
			while (alphas[i]*betas[i]==0) {
				alphas[i]=Math.abs(mt.nextInt());
				betas[i]=Math.abs(mt.nextInt());
			}
		}
		mostRecentExpiration = new MyLinkedList.Tuple[w*d];
	}

	public double getRequiredMemory() {
		double mem=0;
		for (int i=0;i<array.length;i++){
			for (int j=0;j<array[i].length;j++) {
				mem+=array[i][j].getRequiredMemory();
			}
		}
		return mem;
	}
	public double getRequiredNetwork() {
		double mem=0;
		for (int i=0;i<array.length;i++){
			for (int j=0;j<array[i].length;j++) {
				mem+=array[i][j].getRequiredNetwork();
			}
		}
		return mem;
	}
	public double getRequiredNetworkWithExpire(int expiryTime) {
		double mem=0;
		for (int i=0;i<array.length;i++){
			for (int j=0;j<array[i].length;j++) {
				array[i][j].removeExpired(expiryTime);
				mem+=array[i][j].getRequiredNetwork();
			}
		}
		return mem;
	}
	long[] alphas;
	long[] betas;

	public static slidingCMSketch mergeSlidingCMSketches(slidingCMSketch[] dws, double delta, double epsilon, int windowSize, int repeat) {
		if (dws.length==1) return dws[0];
		long maxEvents = 0;
		sliding_window_structures SLIDING_WINDOW_TYPE=dws[0].getSlidingWindowType();

		maxEvents=dws[0].getNumberOfEvents(); // maximum number of events in the SW stays the same in this scenario!
		
		slidingCMSketch scm = new slidingCMSketch(delta, epsilon, windowSize, maxEvents, SLIDING_WINDOW_TYPE, repeat, dws[0].optimizeForSJ);
		for (int i=0;i<scm.w;i++) {
			for (int j=0;j<scm.d;j++) {
				// list of all exponential histograms
				switch(SLIDING_WINDOW_TYPE) {
				case EC: {
						ExponentialHistogramCircularInt[] al = new ExponentialHistogramCircularInt[dws.length];int c=0;
						for (slidingCMSketch single:dws) al[c++]=(ExponentialHistogramCircularInt) single.getSlidingWindowAtLocation(i, j);
						scm.array[i][j]=ExponentialHistogramCircularInt.mergeEHs(al, dws[0].getEpsilonSW(), windowSize);
						break;
				}
				case DW: { 
					detWaveOptDeque[] al = new detWaveOptDeque[dws.length];int c=0;
					for (slidingCMSketch single:dws) al[c++]=(detWaveOptDeque) single.getSlidingWindowAtLocation(i, j);
					scm.array[i][j]=detWaveOptDeque.mergedetWaves(al, dws[0].getEpsilonSW(), windowSize, (int) maxEvents);
					break;
				}
				case RW: {
					compositeRandWaveDeque[] al = new compositeRandWaveDeque[dws.length];int c=0;
					for (slidingCMSketch single:dws) al[c++]=(compositeRandWaveDeque) single.getSlidingWindowAtLocation(i, j);
					scm.array[i][j]=compositeRandWaveDeque.mergeRandWaves(al, delta/2d, dws[0].getEpsilonSW(), windowSize, (int)maxEvents);
					break;
				}
				default:
					System.err.println("Error - type cannot be handled!");
					break;
				}
			}
		}
		return scm; 
	}
	
	// merge only one wcnt/dcnt
	public static void mergeSlidingCMSketches(slidingCMSketch[] dws, slidingCMSketch sketchToStoreMerged, int wcnt, int dcnt, double delta, double epsilon, int windowSize, int repeat) {
		if (dws.length == 1) return;
		sliding_window_structures SLIDING_WINDOW_TYPE = dws[0].getSlidingWindowType();
		long maxEvents=dws[0].getNumberOfEvents(); // maximum number of events in the SW stays the same in this scenario!

		switch (SLIDING_WINDOW_TYPE) {
			case EC: {
				ExponentialHistogramCircularInt[] al = new ExponentialHistogramCircularInt[dws.length];
				int c = 0;
				for (slidingCMSketch single : dws)
					al[c++] = (ExponentialHistogramCircularInt) single.getSlidingWindowAtLocation(wcnt, dcnt);
				sketchToStoreMerged.array[wcnt][dcnt] = ExponentialHistogramCircularInt.mergeEHs(al, dws[0].getEpsilonSW(), windowSize);
			}
			case DW: { 
				detWaveOptDeque[] al = new detWaveOptDeque[dws.length];int c=0;
				for (slidingCMSketch single:dws) al[c++]=(detWaveOptDeque) single.getSlidingWindowAtLocation(wcnt, dcnt);
				sketchToStoreMerged.array[wcnt][dcnt]=detWaveOptDeque.mergedetWaves(al, dws[0].getEpsilonSW(), windowSize, (int) maxEvents);
				break;
			}
			case RW: {
				compositeRandWaveDeque[] al = new compositeRandWaveDeque[dws.length];int c=0;
				for (slidingCMSketch single:dws) al[c++]=(compositeRandWaveDeque) single.getSlidingWindowAtLocation(wcnt, dcnt);
				sketchToStoreMerged.array[wcnt][dcnt]=compositeRandWaveDeque.mergeRandWaves(al, delta/2d, dws[0].getEpsilonSW(), windowSize, (int)maxEvents);
				break;
			}
			default:
				System.err.println("Error - type cannot be handled!");
				break;
		}
	}
	
	public static void mergeSlidingCMSketchesToCM(slidingCMSketch[] dws, slidingCMSketch sketchToStoreMerged, int wcnt, int dcnt, int queryId) {
		sliding_window_structures SLIDING_WINDOW_TYPE = dws[0].getSlidingWindowType();
		switch (SLIDING_WINDOW_TYPE) {
			case EC:
			case DW:
			case RW:
				sketchToStoreMerged.array[wcnt][dcnt] = null;
				sketchToStoreMerged.estimations[queryId][wcnt][dcnt] = 0;
				for (slidingCMSketch scm:dws) 
					sketchToStoreMerged.estimations[queryId][wcnt][dcnt]+=scm.estimations[queryId][wcnt][dcnt];
				break;
			default:
				System.err.println("Error - type cannot be handled!");
				break;
		}
	}
	
	public static void mergeSlidingCMSketchesToCM(slidingCMSketch[] dws, double[][][] sketchToStoreMerged, int wcnt, int dcnt, int queryId) {
		sliding_window_structures SLIDING_WINDOW_TYPE = dws[0].getSlidingWindowType();
		switch (SLIDING_WINDOW_TYPE) {
			case EC:
			case DW:
			case RW:
				sketchToStoreMerged[queryId][wcnt][dcnt] = 0;
				for (slidingCMSketch scm:dws) 
					sketchToStoreMerged[queryId][wcnt][dcnt]+=scm.estimations[queryId][wcnt][dcnt];
				break;
			default:
				System.err.println("Error - type cannot be handled!");
				break;
		}
	}
	
	
	
	final int allones = Integer.MAX_VALUE;
	
	public final int []hash(int type, int levels, int mod) {
		final int [] hash = new int[levels];
		for (int i=0;i<levels;i++) {
			long tmp = alphas[i]*(type)+betas[i];
			hash[i]=(int)((((tmp>>31) + tmp)&allones)%mod);
		}
		return hash;
	}

	int rank=0;

	TreeMap<Integer,MyLinkedList<int[]>> expirationTimesOfBucketsForAllQueries[] = null; // key is expiration time, val is coordinate
	final MyLinkedList.Tuple<int[]>[] mostRecentExpiration; 
	
	public void addToLinkedList(TreeMap<Integer,MyLinkedList<int[]>> expirationTimesOfBuckets, int nextExpTime, int dcnt, int wcnt) {
		MyLinkedList<int[]> ll = expirationTimesOfBuckets.get(nextExpTime);
		if (ll==null) {
			ll=new MyLinkedList<>();
			expirationTimesOfBuckets.put(nextExpTime, ll);
		}
		ll.add(new int[]{dcnt,wcnt});

		int uniqueCount = getUniqueCount(w, wcnt, dcnt);
		mostRecentExpiration[uniqueCount]=new MyLinkedList.Tuple<>(ll, ll.getLastNode());
	}
	
	public void addToLinkedListAndRemoveOlder(TreeMap<Integer,MyLinkedList<int[]>> expirationTimesOfBuckets, int nextExpTime, int dcnt, int wcnt) {
		MyLinkedList<int[]> ll = expirationTimesOfBuckets.get(nextExpTime);
		if (ll==null) {
			ll=new MyLinkedList<>();
			expirationTimesOfBuckets.put(nextExpTime, ll);
		}
		ll.add(new int[]{dcnt,wcnt});

		int uniqueCount = getUniqueCount(w, wcnt, dcnt);
		if (mostRecentExpiration[uniqueCount]!=null) { // remove old expiration
			MyLinkedList.Tuple<int[]> t = mostRecentExpiration[uniqueCount];
			t.getList().removeNode(t.getNode()); // expiration time changed now, so old one should be removed
		} // add new one, and overwrite old if exists
		mostRecentExpiration[uniqueCount]=new MyLinkedList.Tuple<>(ll, ll.getLastNode());
	}
	
	double[][] maintainedInnerJoins=null;
	public static int getUniqueCount(int w, int wcnt, int dcnt) {
		return (w*dcnt)+wcnt;
	}

	public double getInnerProductFromMaintainedVectors(int queryId) {
		double val = Double.MAX_VALUE;
		for (int depth=0;depth<d;depth++) {
			double est=0;			
			for (int width=0;width<w;width++) {
				double tmp=estimations[queryId][width][depth];
				est+=square(tmp);
			}
			val=Math.min(val, est);
		}
		return val;
	}

	public double getFromMaintainedVector(int queryId, int[]hashes) {
		double minVal = Double.MAX_VALUE;
		for (int dcnt=0;dcnt<hashes.length;dcnt++)
			minVal=Math.min(minVal, estimations[queryId][hashes[dcnt]][dcnt]);
		return minVal;
	}
	public static double getFromGivenVector(int queryId, double[][][] vector, int[]hashes) {
		double minVal = Double.MAX_VALUE;
		for (int dcnt=0;dcnt<hashes.length;dcnt++)
			minVal=Math.min(minVal, vector[queryId][hashes[dcnt]][dcnt]);
		return minVal;
	}

	public double getFromMaintainedVector(int queryId, int itemid) {
		int[]hashes = hash(itemid, d, w);
		double minVal = Double.MAX_VALUE;
		for (int d=0;d<hashes.length;d++)
			minVal=Math.min(minVal, estimations[queryId][hashes[d]][d]);
		return minVal;
	}
	
	
	public void maintainInnerJoin(int time, int[] hashes) {
		double[][] maintainedInnerJoinsAtEntrance = lib.deepClone(maintainedInnerJoins);
		boolean singleUpdate=false;
		for (int queryId=0;queryId<queryLengths.length;queryId++) {
			TreeMap<Integer,MyLinkedList<int[]>> expirationTimesOfBuckets = expirationTimesOfBucketsForAllQueries[queryId];
			int queryLength=queryLengths[queryId];
			// update any of the updated counters
			int startTime=time-queryLength;
			HashSet<Integer> pairOfBuckets = new HashSet<>();
			if (hashes!=null) {
				singleUpdate=true;
				for (int dcnt=0;dcnt<d;dcnt++) {
					int wcnt = hashes[dcnt];
					int uniqueCount = getUniqueCount(w, wcnt, dcnt);
					if (!pairOfBuckets.add(uniqueCount)) continue; // no need to check because i already checked it
					numberOfInvokesToGetEstimation++;
					Pair p = array[wcnt][dcnt].getEstimationRealtimeWithExpiryTime(startTime,queryLength);
					double val = p.estimation;
					int nextExpTime = p.time;
					if (nextExpTime<0) 
						nextExpTime=time;
					addToLinkedListAndRemoveOlder(expirationTimesOfBuckets, nextExpTime, dcnt, wcnt);
					double diffInnerJoin = lib.square(val)-lib.square(estimations[queryId][wcnt][dcnt]);
					maintainedInnerJoins[queryId][dcnt]+=diffInnerJoin;
					estimations[queryId][wcnt][dcnt]=val; // for the next iteration
				}
			}
			
			// and now find out which buckets have expired
			while (expirationTimesOfBuckets.size()!=0 && expirationTimesOfBuckets.firstEntry().getKey()<time) {
				singleUpdate=true;
				Entry<Integer,MyLinkedList<int[]>> expired = expirationTimesOfBuckets.pollFirstEntry();
				MyLinkedList<int[]> ll = expired.getValue();
				for (int[] d:ll) {
					int dcnt = d[0]; int wcnt =d[1];
					int uniqueCount = getUniqueCount(w, wcnt, dcnt);
					if (!pairOfBuckets.add(uniqueCount)) continue; // no need to check because i already checked it
					numberOfInvokesToGetEstimation++;
					Pair p = array[wcnt][dcnt].getEstimationRealtimeWithExpiryTime(startTime,queryLength);
					array[wcnt][dcnt].removeExpiredWithExpiryTime(startTime);
					double val = p.estimation;
					int nextExpTime = p.time;
					if (nextExpTime>0) // otherwise it is 0, it never expires
						addToLinkedList(expirationTimesOfBuckets, nextExpTime, dcnt, wcnt);

					double diffInnerJoin= lib.square(val)-lib.square(estimations[queryId][wcnt][dcnt]);
					maintainedInnerJoins[queryId][dcnt]+=diffInnerJoin;
					estimations[queryId][wcnt][dcnt]=val; // for the next iteration
				}
			}
			if (singleUpdate) {
				liveInnerJoins[queryId]=Double.MAX_VALUE;
				for(int dcnt=0;dcnt<d;dcnt++) liveInnerJoins[queryId]=Math.min(liveInnerJoins[queryId], maintainedInnerJoins[queryId][dcnt]);

				// now compare to see if something changed
				boolean changed=false;
				for(int dcnt=0;dcnt<d && !changed;dcnt++) changed = (Math.abs(maintainedInnerJoinsAtEntrance[queryId][dcnt]-maintainedInnerJoins[queryId][dcnt])>0.0001);
				if (changed) setChangedSinceLast(queryId,true);
			}
		}
	}
	
	
	double[] liveInnerJoins;
	public final void tick(int time) {
		this.maintainInnerJoin(time,null);
	}
	
	public final int[] add(int type, int time) {
		rank++;
		int[]hashes = hash(type, d, w);
//		System.err.println(type + ":" + hashes[0] + " " + hashes[1] + " " + hashes[2]);
		for (int depth=0;depth<d;depth++) {
			array[hashes[depth]][depth].addAOne(time);
		}
		this.lastUpdateTime = time;
		if (this.maintainInnerJoin) 
			this.maintainInnerJoin(time,hashes);
		return hashes;
	}

	public final void add(int type, int time, int[]hashes) {
		rank++;
		for (int depth=0;depth<d;depth++) {
			array[hashes[depth]][depth].addAOne(time);
		}
		this.lastUpdateTime = time;
		if (this.maintainInnerJoin) 
			this.maintainInnerJoin(time,hashes);
	}

	public double get(int type, int time) {
		int[]hashes = hash(type, d, w);
		double val = Double.MAX_VALUE;
		for (int depth=0;depth<d;depth++) {
			int w = hashes[depth];
			numberOfInvokesToGetEstimation++;
			double est=array[w][depth].getEstimationRealtime(time);
			val = Math.min(val, est);
		}
		return val;
	}
	public double get(int type, int time, int [] hashes) {
		double val = Double.MAX_VALUE;
		for (int depth=0;depth<d;depth++) {
			int w = hashes[depth];
			numberOfInvokesToGetEstimation++;
			double est=array[w][depth].getEstimationRealtime(time);
			val = Math.min(val, est);
		}
		return val;
	}
	public static double square(double v) {
		return v*v;
	}
	public double getInnerProduct(int time) {
		return getInnerProduct(time, null);
	}
	public double getMaintainedInnerProduct(int queryId) {
		return liveInnerJoins[queryId];
	}
	public double getMaintainedInnerJoinAtRow(int queryId, int rowid) {
		return maintainedInnerJoins[queryId][rowid];
	}
	public double[] getMaintainedInnerJoinsForRows(int queryId) {
		return maintainedInnerJoins[queryId];
	}
	public double[][] getMaintainedEstimations(int queryId) {
		return estimations[queryId];
	}
	public double getInnerProduct(int time, double[]perRow) {
		double val = Double.MAX_VALUE;
		for (int depth=0;depth<d;depth++) {
			double est=0;			
			for (int width=0;width<w;width++) {
				numberOfInvokesToGetEstimation++;
				double tmp=array[width][depth].getEstimationRealtime(time);
				est+=square(tmp);
			}
			val=Math.min(val, est);
			if (perRow!=null)
				perRow[depth]=est;
		}
		return val;
	}
	
	public double getInnerProductAverage(int time, int numberOfNodes) {
		return getInnerProductAverage(time, numberOfNodes, null);
	}
	public double getInnerProductAverage(int time, int numberOfNodes, double[]perRow) {
		double val = Double.MAX_VALUE;
		for (int depth=0;depth<d;depth++) {
			double est=0;			
			for (int width=0;width<w;width++) {
				numberOfInvokesToGetEstimation++;
				double tmp=array[width][depth].getEstimationRealtime(time);
				tmp/=numberOfNodes;
				est+=square(tmp);
			}
			val=Math.min(val, est);
			if (perRow!=null) perRow[depth]=est;
		}
		return val;
	}
	
	public double getInnerProductMultiplyByNodes(int time, int numberOfNodes) {
		double val = Double.MAX_VALUE;
		for (int depth=0;depth<d;depth++) {
			double est=0;			
			for (int width=0;width<w;width++) {
				numberOfInvokesToGetEstimation++;
				double tmp=array[width][depth].getEstimationRealtime(time);
				est+=square(tmp);
			}
			val=Math.min(val, est);
		}
		return val*numberOfNodes*numberOfNodes;
	}
	public double getInnerProductDivideByNumberOfNodes(int time, int nodes) {
		double n = nodes;
		double val = Double.MAX_VALUE;
		for (int depth=0;depth<d;depth++) {
			double est=0;			
			for (int width=0;width<w;width++) {
				numberOfInvokesToGetEstimation++;
				double tmp=array[width][depth].getEstimationRealtime(time)/n;
				est+=Math.pow(tmp,2);
			}
			val=Math.min(val, est);
		}
		return val;
	}
	
	public static double getInnerProduct(double[][] array) {
		double val = Double.MAX_VALUE;
		int w = array.length;
		int d = array[0].length;
		for (int depth=0;depth<d;depth++) {
			double est=0;			
			for (int width=0;width<w;width++) {
				double tmp=array[width][depth];
				est+=Math.pow(tmp,2);
			}
			val=Math.min(val, est);
		}
		return val;
	}
	
	static int[][] computeHDGroundTruth(int[] queryTimes, EventHD[] events, int numberOfTypes) {
		int numberOfQueries=queryTimes.length; 
		int[][] results = new int[numberOfQueries][numberOfTypes]; // all are zeros initially
		int queryId=0;
		int i=events.length-1;
		while (i>=0) {
			EventHD e = events[i];
			if (e.comesAtOrAfter(queryTimes[queryId])) {
				if (queryId>0) // then carry on the previous answers!
					for (int t=0;t<numberOfTypes;t++) results[queryId][t]=results[queryId-1][t];
				results[queryId][e.getEvent()]++;
			} else { // try next query
				for (int t=0;t<numberOfTypes;t++) results[queryId+1][t]=results[queryId][t];
				queryId++;
				i++; // redo this event
			}
			i--;			
		}
		queryId++;
		while (queryId<=results.length-1)  {
			for (int t=0;t<numberOfTypes;t++)
				results[queryId][t]=results[queryId-1][t];
			queryId++;
		}
		return results;
	}
}
