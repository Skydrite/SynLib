package structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import dataGeneration.StreamConstructor;

class Pair {
	final int time; 
	final double estimation;
	public Pair(int time, double estimation) {
		this.time=time;
		this.estimation=estimation;
	}
	public String toString() {
		return "(T:" + time + " E:" + estimation + ")";
	}
}


public class ExponentialHistogramCircularInt implements slidingwindow {
	final int k;
	final int halfk;
	final double epsilon;
	//final int maxNumberOfBuckets;
	public final cBufferInt bucketWallclockTimes[]; // the creation time of each bucket (in timestamp/wallclock time)
	final int windowSize;
	int numberOfLevels=0;
	int currentRealtime=0;
	public int numberOfBuckets=0;
	int lastOneUpdate=0;
	final int maxNumberOfBucketsOfTheSameSize;
	final int maxNumberOfBucketsOfSize1;
	final long maxEvents;
	private int lastSyncedTime;

	public int getLastSyncedTime() {
		return this.lastSyncedTime;
	}
	public ExponentialHistogramCircularInt clone() {	
		return new ExponentialHistogramCircularInt(this);
	}
	public void setLastSyncedTime(int t) {
		this.lastSyncedTime=t;
	}

	
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
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Levels:" + numberOfLevels);
		for (int i=0;i<numberOfLevels;i++) {
			sb.append("[ L" + i + " : " + this.bucketWallclockTimes[i] + " ]");
		}
		return sb.toString();
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
	
	public int getNumberOfInterrupts() {
		int interrupts=(halfk+1)*2*(numberOfLevels-1) + k+1;
		return interrupts;
	}

	
	
	public static ExponentialHistogramCircularInt mergeEHs(ExponentialHistogramCircularInt[] ehs, double epsilon, int windowSize) {
		// now merge
		int totalEvents=0; int totalInterrupts=0;
		for (ExponentialHistogramCircularInt eh:ehs) { 
			totalEvents+=eh.getEstimationRealtime(0);
			totalInterrupts+=eh.getNumberOfInterrupts();
		}
		if (totalEvents==Integer.MAX_VALUE||totalInterrupts==Integer.MAX_VALUE) {
			System.err.println("Overflow mergeEHs0"); System.exit(-1);
		}
		int numberOfEH=ehs.length;
		ArrayList<Tuple> interrupts = new ArrayList<Tuple>(totalInterrupts);
		for (int cnt=0;cnt<numberOfEH;cnt++) {
			ExponentialHistogramCircularInt eh = ehs[cnt];
			int bucketEndTime = eh.currentRealtime;
			if (bucketEndTime==Integer.MAX_VALUE) {
				System.err.println("Overflow mergeEHs1"); System.exit(-1);
			}
				
			for (int levelid=0;levelid<eh.numberOfLevels;levelid++) {
				int bucketSize=(int)Math.pow(2,levelid);
				if (bucketSize>1) {
					Iterator<Integer> iter = eh.bucketWallclockTimes[levelid].descendingIterator();
					while (iter.hasNext()) {
						Integer startTime=iter.next();
						interrupts.add(new Tuple(bucketEndTime,  bucketSize/2)); // half on the beginning of the bucket
						interrupts.add(new Tuple(startTime,  bucketSize/2)); // half on the end of the bucket
						bucketEndTime=startTime;
					}
				} else {
					Iterator<Integer> iter = eh.bucketWallclockTimes[levelid].descendingIterator();
					while (iter.hasNext()) {
						Integer startTime=iter.next();
						interrupts.add(new Tuple(startTime,  bucketSize)); // half on the end of the bucket
						bucketEndTime = startTime;
					}
				}
			}
		}
		Collections.sort(interrupts); // will sort all events from all EHs, oldest first
		ExponentialHistogramCircularInt ehAggregated=new ExponentialHistogramCircularInt(epsilon, windowSize, totalEvents);
		// and now replay
		for (Tuple t:interrupts) {
			if (t.t==Integer.MAX_VALUE){
				System.err.println("Overflow mergeEHs2"); System.exit(-1);
			}
			ehAggregated.updateByMany(t.t, (float)t.val);
		}
		// END now merge with method 4
		return ehAggregated;
	}

	public ExponentialHistogramCircularInt(ExponentialHistogramCircularInt exponentialHistogramCircularInt) {
		this.epsilon=exponentialHistogramCircularInt.epsilon;
		this.maxEvents=exponentialHistogramCircularInt.maxEvents;
		this.k = exponentialHistogramCircularInt.k;
		this.halfk = exponentialHistogramCircularInt.halfk;
		this.windowSize=exponentialHistogramCircularInt.windowSize;
//		this.maxNumberOfBuckets=eh.maxNumberOfBuckets;
		this.bucketWallclockTimes=new cBufferInt[50];
		for (int i=0;i<exponentialHistogramCircularInt.bucketWallclockTimes.length;i++) {
			if (exponentialHistogramCircularInt.bucketWallclockTimes[i]!=null) this.bucketWallclockTimes[i] = exponentialHistogramCircularInt.bucketWallclockTimes[i].clone();
		}
		this.numberOfLevels=exponentialHistogramCircularInt.numberOfLevels;
		maxNumberOfBucketsOfTheSameSize=exponentialHistogramCircularInt.maxNumberOfBucketsOfTheSameSize;
		maxNumberOfBucketsOfSize1=exponentialHistogramCircularInt.maxNumberOfBucketsOfSize1;
//		this.expirations = (TreeMap<Integer, LinkedList<Integer>>)eh.expirations.clone();
	}
	
	public ExponentialHistogramCircularInt(double epsilon, int windowSize,  long maxEvents) {
		this.epsilon=epsilon;
		this.maxEvents=maxEvents;
		this.k = (int)Math.ceil(1d/epsilon);
		this.halfk = (int)Math.ceil(k/2d);
		this.windowSize=windowSize;
//		int b=(int) Math.ceil((k/2d+1)*(Math.log(2d*maxEvents/k)/Math.log(2)+2))+1;
//		if (b<=0) 
//			this.maxNumberOfBuckets=(int)maxEvents+1; // this happens when maxevents is very small 
//		else 
//			this.maxNumberOfBuckets=b;
		this.bucketWallclockTimes=new cBufferInt[50];
		maxNumberOfBucketsOfTheSameSize=halfk+1;
		maxNumberOfBucketsOfSize1=k+1;
		this.bucketWallclockTimes[0]=new cBufferInt(maxNumberOfBucketsOfSize1+1);
//		for (int cnt=1;cnt<this.bucketWallclockTimes.length;cnt++){
//			this.bucketWallclockTimes[cnt]=new cBufferInt(maxNumberOfBucketsOfTheSameSize+1);
//		}
		this.numberOfLevels++;
	}

	public void deleteFirstBucket(int level) {
		bucketWallclockTimes[level].pollFirst();
		numberOfBuckets--;
		if (bucketWallclockTimes[level].isEmpty()) {
			bucketWallclockTimes[level] = null;
			numberOfLevels--;
		}
	}

	public void removeExpiredWithExpiryTime(final int expiryTime) { // current time is measured in timestamps
		if (numberOfBuckets<=1||expiryTime<0) return;
		int nextLevelToCheck = numberOfLevels-1;
		int nextItemToCheck = 1;
		while (nextLevelToCheck>=0) {
			nextItemToCheck=1;
			if (bucketWallclockTimes[nextLevelToCheck].size()<2) {
				nextLevelToCheck--;
				nextItemToCheck=0;
				if (nextLevelToCheck==-1 || bucketWallclockTimes[nextLevelToCheck].size()==0) break;
				if (bucketWallclockTimes[nextLevelToCheck].get(nextItemToCheck)<expiryTime)
					deleteFirstBucket(nextLevelToCheck+1);
				else 
					break;			
			}
			else if (bucketWallclockTimes[nextLevelToCheck].size()>=2){
				if (bucketWallclockTimes[nextLevelToCheck].get(nextItemToCheck)<expiryTime)
					deleteFirstBucket(nextLevelToCheck);
				else 
					break;			
			}
		}
	}
	
	public void removeExpiredWithExpiryTimeAndDiscreetTimeValuesDelete(final Integer expiryTime) { // current time is measured in timestamps
		if (numberOfBuckets<=1||expiryTime<0) return;
		int level=numberOfLevels-1;
		if (level==-1) return;
		while (!bucketWallclockTimes[level].isEmpty() && bucketWallclockTimes[level].getFirst()<expiryTime) {
			deleteFirstBucket(level);
			if (bucketWallclockTimes[level].isEmpty() && numberOfLevels>0) numberOfLevels--;
			if (level==-1) break;
		}

//		if (numberOfBuckets<=1||expiryTime<0) return;
//		int level=numberOfLevels-1;
//		while (!bucketWallclockTimes[level].isEmpty() && bucketWallclockTimes[level].getFirst()<expiryTime) {
//			// distinguish between the case that this is the first bucket that its starttime is expired (it needs to be kept), or not the first 
//			if (bucketWallclockTimes[level].size()>1) {
//				deleteFirstBucket(level);
//			} else if (bucketWallclockTimes[level].size()==1 && level>0 &&bucketWallclockTimes[level-1].getFirst()<expiryTime) {
//				deleteFirstBucket(level);
//				level--;
//			} else {
//				level--;
//			}
//			if (level==-1) break;
//		}
	}

	
	public void batchUpdate(Stream s) {
		for (Event e:s.getEvents()) {
			if (e.event)
				this.addAOne(e.time);
			else
				this.addAZero(e.time);
		}
	}

	public int getCurrentRealtime() {
		return currentRealtime;
	}
	public void addAZero(int time) {
		currentRealtime=time;
//		if (removeExpired) 
		removeExpiredWithExpiryTime(currentRealtime-windowSize);
		updateTriggers();
	}

	int lastOfferedExpirationTime=-1;
	public int getLastOfferedExpirationTime() {
		return lastOfferedExpirationTime;
	}
	public int getSecondBucketExpirationTimeDelete(int startTime) {
		int level=numberOfLevels-1;
		if (level==0 &&bucketWallclockTimes[level].size()==1)
			return -1;
		else if (bucketWallclockTimes[level].size()==1)
			return bucketWallclockTimes[level-1].get(1);
		else
			return bucketWallclockTimes[level].get(1);
	}
	
	public int getNextExpirationTimeDelete(int startTime) {
		int level=0;
		while (bucketWallclockTimes[level].size()>0 && bucketWallclockTimes[level].getFirst()>=startTime) {
			level++;
		}
		// at this level, i need to find the proper element
		int originalPos = bucketWallclockTimes[level].binarySearch(startTime);
		int pos=originalPos;
		if (pos<0) pos=-pos-1;
		if (pos==bucketWallclockTimes[level].size()) { // only the last one - the most recent, so I will return the expiration time of the first bucket of the above row
			if (level>0)
				return bucketWallclockTimes[level-1].getFirst();
			else
				return -1;
		} else { // not the last one
			if (originalPos<0) 
				pos--;
			while (bucketWallclockTimes[level].get(pos)==startTime) pos--; pos++;// find the first element with this time.
//			int levelBucketSize=(int)Math.pow(2, level);
			return bucketWallclockTimes[level].get(pos+1);
		}
	}

	public int getNextExpirationTime2(int startTime) {
		int level=0;
		if (startTime<0) {
			lastOfferedExpirationTime=-1;
			return lastOfferedExpirationTime;
		}
		while (bucketWallclockTimes[level].size()>0 && bucketWallclockTimes[level].getFirst()>startTime) { // > because i assume that items arrive at discrete time stamps
			level++;
		}
		// at this level, i need to find the proper element
		int originalPos = bucketWallclockTimes[level].binarySearch(startTime);
		int pos=originalPos;
		if (pos<0) pos=-pos-1;
		if (pos==bucketWallclockTimes[level].size()) { // the very last
			lastOfferedExpirationTime=bucketWallclockTimes[level-1].getFirst();
			return lastOfferedExpirationTime;
		} else {
			if (originalPos<0) 
				pos--;
//			while (bucketWallclockTimes[level].get(pos)==startTime && pos>0) pos--; pos++;// find the first element with this time.
			
			lastOfferedExpirationTime = bucketWallclockTimes[level].get(pos+1);
			return lastOfferedExpirationTime;
		}
	}
	//final boolean removeExpired=true;
	public void addAOne(int time) {
		currentRealtime=time;
		lastOneUpdate=time;
//		if (removeExpired) 
		removeExpiredWithExpiryTime(currentRealtime-windowSize);
		// BUCKET 0 is tmp by convention
		bucketWallclockTimes[0].addLast(currentRealtime);
		// and now merge if needed
		numberOfBuckets++;
		mergeIfNeeded(currentRealtime-windowSize);
		updateTriggers();
	}

	private void mergeIfNeeded(int expiryTime) {
		int level=0;
		while (true) {
			int maxNumberOfBucketsForThisCase;
			if (level==0) 
				maxNumberOfBucketsForThisCase=k+1;
			else
				maxNumberOfBucketsForThisCase=halfk+1;
			int sizeOfLevel=bucketWallclockTimes[level].size();
			if (sizeOfLevel>maxNumberOfBucketsForThisCase) { // i need to merge
				int startTime=bucketWallclockTimes[level].pollFirst();bucketWallclockTimes[level].pollFirst();
				if (bucketWallclockTimes[level+1]==null || bucketWallclockTimes[level+1].size()==0)  {
					if (level+1==0) // level is 0 
						this.bucketWallclockTimes[0]=new cBufferInt(maxNumberOfBucketsOfSize1+1);
					else	
						bucketWallclockTimes[level+1]=new cBufferInt(maxNumberOfBucketsOfTheSameSize+1);
					numberOfLevels++;
				}
				bucketWallclockTimes[level+1].addLast(startTime);
				numberOfBuckets--; // i merged two in one
				level++;
			} else
				break;
		}
	}
	public void updateTriggers() {
		
	}
	public void updateByMany(int realtimeNow, float val) {
		while (val>0) {addAOne(realtimeNow);val--;}
		if (val!=0) System.err.println("Wrong remaining:" + val);
	}

	private void mergeIfNeededBeforeAddingNew() {
		int level=0;
		int buffer=-1;
		while (true) {
			int maxNumberOfBucketsForThisCase;
			if (level==0) 
				maxNumberOfBucketsForThisCase=k+1;
			else
				maxNumberOfBucketsForThisCase=halfk+1;
			int sizeOfLevel=bucketWallclockTimes[level].size();
			if (sizeOfLevel==maxNumberOfBucketsForThisCase) { // i need to merge
				int startTime=bucketWallclockTimes[level].pollFirst();bucketWallclockTimes[level].pollFirst();
				if (bucketWallclockTimes[level+1].size()==0) 
					numberOfLevels++;
				buffer=startTime;
				if (buffer!=-1)
//				bucketWallclockTimes[level+1].addLast(startTime);
				numberOfBuckets--; // i merged two in one and i hold one
				level++;
			} else {
				if (buffer!=-1) bucketWallclockTimes[level].addLast(buffer);
				break;
			}
		}
	}
		
	public double getEstimationRealtime(int startTime) {
		double est=0;
		int level=0;
		int pow = 1;
		while (bucketWallclockTimes[level] != null && bucketWallclockTimes[level].size()>0 && bucketWallclockTimes[level].getFirst()>=startTime) {
//			est+=Math.pow(2, level)*bucketWallclockTimes[level].size();
			est+=pow*bucketWallclockTimes[level].size();
			level++;
            pow*=2;
		}
		// at this level, i need to find the proper element
		int originalPos =-1;
		if (bucketWallclockTimes[level]!=null) 
			originalPos = bucketWallclockTimes[level].binarySearch(startTime);
		int pos=originalPos;
		if (pos<0) pos=-pos-1;
		if (bucketWallclockTimes[level]==null || pos==bucketWallclockTimes[level].size()) {
//			int levelBucketSize=(int)Math.pow(2, level);
			int levelBucketSize=pow;
			if (bucketWallclockTimes[level]!=null && bucketWallclockTimes[level].size()>0) est+=levelBucketSize/2;
			return est;
		} else {
			if (originalPos<0) 
				pos--;
			while (bucketWallclockTimes[level].get(pos)==startTime) pos--; pos++;// find the first element with this time.
//			int levelBucketSize=(int)Math.pow(2, level);
			int levelBucketSize=pow;
			est+=(bucketWallclockTimes[level].size()-pos)*levelBucketSize;
			if (levelBucketSize!=1) est+=levelBucketSize/2;
			return est;
		}
	}
	
	int[] findNextBucket(int level, int position) { // return {level, position}
		if (position>=bucketWallclockTimes[level].size()-1) {
			level--;
			position=0; // the oldest in the previous row
		} else {
			position++;
		}
		if (level==-1)
			return new int[]{-1,-1};
		else 
			return new int[]{level,position};
	}
	
	int[] findNextBucketWithDifferentTime(int level, int position, int previousExpiryTime) {
		while (true) {
			int[] nextBucket = findNextBucket(level, position);
			int nextLevel = nextBucket[0];
			int nextPosition = nextBucket[1];
			if (nextLevel==-1) return nextBucket;
			if (bucketWallclockTimes[nextLevel].get(nextPosition)!=previousExpiryTime) return nextBucket;
		}
	}	
	int findNextBucketWithDifferentTimeReturnTime(int level, int position, int previousExpiryTime) {
		while (true) {
			int[] nextBucket = findNextBucket(level, position);
			int nextLevel = nextBucket[0];
			int nextPosition = nextBucket[1];
			if (nextLevel==-1) return -1;
			if (bucketWallclockTimes[nextLevel].get(nextPosition)!=previousExpiryTime) 
				return bucketWallclockTimes[nextLevel].get(nextPosition);
			else  {
				position=nextPosition;
				level=nextLevel;
			}
		}
	}

	public Pair getEstimationRealtimeWithExpiryTime(int startTime, int queryLength) {
		double est=0;
		int level=0;
		int pow = 1;
		if (bucketWallclockTimes[0].size()==0) return new Pair(-1, 0); // actually nothing will ever expire from am empty EH!
		while (bucketWallclockTimes[level]!=null && bucketWallclockTimes[level].size()>0 && bucketWallclockTimes[level].getFirst()>=startTime) {
			est+=pow*bucketWallclockTimes[level].size(); // these are the fully-covered rows!
			level++;
            pow*=2;
		}
		int pos;
		if (level<numberOfLevels) { // non-empty level, add part of it!
			int expiryTimeOfTheEstimate;
			int originalPos = bucketWallclockTimes[level].binarySearch(startTime);
			if (originalPos==0) System.err.println("1This should probably not happen, otherwise I would be at the next level");
			if (originalPos>0) {// i found something. Note that original pos will never be 0, otherwise i would go to the next level
				while (bucketWallclockTimes[level].get(originalPos)==startTime) originalPos--; originalPos++; // find the very first element with this value
			}
			
			pos=originalPos;
			if (originalPos<0) { // actual element not found, but i found something SMALLER
				pos=-pos - 1 -1; // the second -1 is because i want to start from the element smaller than startTime
				if (pos<0) {
					System.err.println("2This should probably not happen, otherwise I would be at the next level!");
					pos=0;
				}
				est+=pow*(bucketWallclockTimes[level].size()-pos-1);
			} else 
				est+=pow*(bucketWallclockTimes[level].size()-pos);

			if (level!=0) est+=(pow/2d); // add half of the last bucket
			expiryTimeOfTheEstimate=findNextBucketWithDifferentTimeReturnTime(level, pos, bucketWallclockTimes[level].get(pos));
			if ((expiryTimeOfTheEstimate==-1 && est>0) || originalPos>0) 
				expiryTimeOfTheEstimate=bucketWallclockTimes[level].get(pos);
			if ((expiryTimeOfTheEstimate==-1 && est==0))
				return new Pair(expiryTimeOfTheEstimate, est);
			return new Pair(expiryTimeOfTheEstimate+queryLength, est);
		} else { // empty level, ignore it  - not enough items have arrived to fill the SW yet
			level--;
			pos=0;
			int expiryTimeOfTheEstimate = bucketWallclockTimes[level].get(0) + queryLength;
			return new Pair(expiryTimeOfTheEstimate, est);
		}
	}

//	public IntDoubleTuple getEstimationRealtimeWithExpiration(int startTime) {
//		this.prepareForQuerying();
//		double est=0;
//		int level=0;
//		int pow = 1;
//		while (bucketWallclockTimes[level].size()>0 && bucketWallclockTimes[level].getFirst()>=startTime) {
////			est+=Math.pow(2, level)*bucketWallclockTimes[level].size();
//			est+=pow*bucketWallclockTimes[level].size();
//			level++;
//            pow*=2;
//		}
//		// at this level, i need to find the proper element
//		int originalPos = bucketWallclockTimes[level].binarySearch(startTime);
//		int pos=originalPos;
//		if (pos<0) pos=-pos-1;
//		if (pos==bucketWallclockTimes[level].size()) {
////			int levelBucketSize=(int)Math.pow(2, level);
//			int levelBucketSize=pow;
//			if (bucketWallclockTimes[level].size()>0) est+=levelBucketSize/2;
//			return new IntDoubleTuple(-1,est);
//		} else {
//			if (originalPos<0) 
//				pos--;
//			while (bucketWallclockTimes[level].get(pos)==startTime) pos--; pos++;// find the first element with this time.
////			int levelBucketSize=(int)Math.pow(2, level);
//			int levelBucketSize=pow;
//			est+=(bucketWallclockTimes[level].size()-pos)*levelBucketSize;
//			if (levelBucketSize!=1) est+=levelBucketSize/2;
//			return new IntDoubleTuple(-1,est);
//		}
//	}

	public double getEstimationRealtimeSlower(int startTime) {
		double est=0;
		int level=0;
		while (bucketWallclockTimes[level].size()>0 && bucketWallclockTimes[level].getFirst()>=startTime) {
			est+=Math.pow(2, level)*bucketWallclockTimes[level].size();
			level++;
		}
		// at this level, i need to find the proper element
		int originalPos = bucketWallclockTimes[level].binarySearch(startTime);
		int pos=originalPos;
		if (pos<0) pos=-pos-1;
		if (pos==bucketWallclockTimes[level].size()) {
			int levelBucketSize=(int)Math.pow(2, level);
			if (bucketWallclockTimes[level].size()>0) est+=levelBucketSize/2;
			return est;
		} else {
			if (originalPos<0) 
				pos--;
			while (bucketWallclockTimes[level].get(pos)==startTime) pos--; pos++;// find the first element with this time.
			int levelBucketSize=(int)Math.pow(2, level);
			est+=(bucketWallclockTimes[level].size()-pos)*levelBucketSize;
			if (levelBucketSize!=1) est+=levelBucketSize/2;
			return est;
		}
	}

	// required memory in Kbytes!
	public double getRequiredMemory() {
		int counterBits = 0;
		int realtimeBits = 32; // to store time in whatever format
		int levelBits = numberOfLevels*32;
		int bucketsUpperBound = k+1+(halfk+1)*(numberOfLevels-1);
		double mem = bucketsUpperBound*(counterBits+realtimeBits)+levelBits;
		return mem/8d/1024d;
	}
	// required memory in Kbytes!
	public double getRequiredNetwork() {
		int counterBits = 0; // no need to store 1,2,4,... because this can be computed by the level bits
		int levelBits = numberOfLevels*32;// to store the level delimiters, the counter-bits can then be computed for each counter
		int realtimeBits = 32; // to store time in whatever format
		int numberOfBuckets=0;
		for (int levelid=0;levelid<numberOfLevels;levelid++)
			numberOfBuckets+=bucketWallclockTimes[levelid].size();
		double mem = numberOfBuckets*(counterBits+realtimeBits)+levelBits;
		return mem/8d/1024d; // kbytes
	}
	
	public double getMaxRequiredMemory() {
//		return getRequiredMemory()/(double)numberOfBuckets*(double)maxNumberOfBuckets;
		return getRequiredMemory();
	}
	
	public void cloneForQuerying() {
	}

	@Override
	public void removeExpired(int currentTime) {
		// TODO Auto-generated method stub
		
	}
	public double getEstimationRange(int len) {
		int query = this.lastSyncedTime-len;
		return getEstimationRealtime(query);
	}
	@Override
	public int getLastUpdateTime() {
		if (numberOfLevels>0)
			return bucketWallclockTimes[0].getLast();
		else 
			return 0;
	}
	@Override
	public double getEpsilon() {
		return epsilon;
	}
}
