package structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import dataGeneration.StreamConstructor;

public class detWaveOptDeque implements slidingwindow {
	int rank;
	final ArrayDeque<TimestampWithRankAndPointers> levels[];
	final double epsilon;
	final int windowSize;
	int currentRealtime=0;
	int lastOneUpdate=0;
	final long maxEvents;
	final int numberOfLevels;
	final int eventsPerLevel;
	// each bucket ends immediately when it gets full, at the time of the LAST true bit
	// each bucket starts immediately when the previous gets full (not necessarily at a true bit)
	
	private int lastSyncedTime;
	public void setLastSyncedTime(int t) {
		this.lastSyncedTime=t;
	}
	public int getLastSyncedTime() {
		return this.lastSyncedTime;
	}
	public detWaveOptDeque clone() {	
		return new detWaveOptDeque(this);
	}


	public int getNumberOfLevels() {
		return levels.length;
	}
	public ArrayDeque<TimestampWithRankAndPointers>[] getLevels() {
		return levels;
	}
	public int getRank() {
		return rank;
	}
	
	public int[] computeGroundTruth(int[]queryTimes, Stream str) {
		Event[] events=str.getEvents();
		int numberOfQueries=queryTimes.length;
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
		return accurateAnswers;
	}
	
	ArrayList<TimestampWithRankAndPointers> fixedLevels[]=null;
	boolean prepared=false;

	@SuppressWarnings("unchecked")
	public void prepareForMerging() {
		if (prepared) 
			return;
		prepared=true;
//		ArrayDeque<TimestampWithRankAndPointers> clone[] = new ArrayDeque[this.numberOfLevels];
//
//		for (int i=0;i<numberOfLevels;i++) {
//			if (this.levels[i].size()>1 && this.levels[i].getFirst().rank==this.levels[i].getSecond().rank) {
//				System.err.println("ERROR");
//			}
//			clone[i] = this.levels[i].clone();
//		}
//
//		for (int i=0;i<numberOfLevels;i++) {
//			for (TimestampWithRankAndPointers ts : this.levels[i]) {
//				if (ts.rank==199984){
//					System.err.println("I is " + i);
//				}
//			}
//		}
		fixedLevels= new ArrayList[numberOfLevels];
		for (int i=0;i<numberOfLevels;i++) 
			fixedLevels[i]=new ArrayList<TimestampWithRankAndPointers>(this.levels[i]);
		for (int i=numberOfLevels-1;i>0;i--) 
			if (fixedLevels[i].size()>0)
				fixedLevels[i-1].addAll(fixedLevels[i]);			

		for (int i=numberOfLevels-1;i>=0;i--) {
			if (fixedLevels[i].size()==0) continue;
			Collections.sort(fixedLevels[i]);
			this.levels[i] = new ArrayDeque<TimestampWithRankAndPointers>(fixedLevels[i]);
			while (this.levels[i].size()>eventsPerLevel) 
				this.levels[i].pollFirst();
			this.fixedLevels[i]=new ArrayList<TimestampWithRankAndPointers>(this.levels[i]);
		}
//		for (int i=0;i<numberOfLevels;i++) {
//			if (this.levels[i].size()>1 && this.levels[i].getFirst().rank==this.levels[i].getSecond().rank) {
//				System.err.println("ERROR");
//			}
//		}
	}
	public void cloneForQuerying() {
		fixedLevels=new ArrayList[numberOfLevels];
		for (int i=0;i<numberOfLevels;i++) 
			fixedLevels[i]=new ArrayList<TimestampWithRankAndPointers>(this.levels[i]);
	}

	static void printLevels(ArrayDeque<TimestampWithRankAndPointers> levels[]) {
		System.err.println("LEVELS ORIGINAL \n");
		for (int i=0;i<levels.length;i++) {
			if (levels[i].size()>0) {
				System.err.print(i+": ");
				for (TimestampWithRankAndPointers ts:levels[i]) {
					System.err.print(ts + "...");
				}
			}
			else break;
			System.err.println("");
		}
	}
	
	// type 2 is by operating on the buckets directly, similar to the EH.
	// i change levels when the timestamp of the higher level is BEFORE the timestamp of the current level
	// when i change level, i use the bucket values of the higher level.
	public static detWaveOptDeque mergedetWaves(detWaveOptDeque[] dws, double epsilon, int windowSize, int totalEvents) {
		// now merge
		// START now merge with method 4

		if (dws.length==1) 
			return dws[0];
		
//#1. Use ranks instead of before and after because of the case of equal times
//#2. When I have overlapping space, aferese
		
		int maxLevels=0;
		for (detWaveOptDeque eh:dws) {
			maxLevels=Math.max(maxLevels, eh.getNumberOfLevels());
		}
		
		detWaveOptDeque merged = new detWaveOptDeque(epsilon, windowSize, totalEvents, maxLevels);

		ArrayList<TimestampWithRankAndPointers> interrupts = new ArrayList<TimestampWithRankAndPointers>(10000);
		for (detWaveOptDeque dw:dws) {
			dw.prepareForMerging();
			ArrayDeque<TimestampWithRankAndPointers> levels[] = dw.getLevels();
//			printLevels(levels);
			int startTimeOfNextTS=0;
			int timestampId=0;
			for (int levelid=levels.length-1;levelid>=0;levelid--) {
				ArrayList<TimestampWithRankAndPointers>level=dw.fixedLevels[levelid];//new ArrayList(levels[levelid]);
				int valToAdd = (int) Math.pow(2, levelid); 
				if (levelid>0) {
					if (levels[levelid-1].size()==0 || levels[levelid-1].size()<dws[0].eventsPerLevel) 
						continue; // empty, nothing to add OR i will find a higher quality
					TimestampWithRankAndPointers firstAtHigherLevel = levels[levelid-1].getFirst();
					for (;timestampId<level.size();timestampId++) {
						TimestampWithRankAndPointers ts = level.get(timestampId);
   
						if (ts.rank<=firstAtHigherLevel.rank) { // then i add startTime and endTime fully
							interrupts.add(new TimestampWithRankAndPointers(startTimeOfNextTS, valToAdd/2));
							interrupts.add(new TimestampWithRankAndPointers(ts.getTime(), valToAdd/2));
//							System.err.println("Added " + valToAdd/2 + " at (" + startTimeOfNextTS + "-" + ts + ")");
							startTimeOfNextTS=ts.getTime();
							if (ts.rank==firstAtHigherLevel.rank) {
								timestampId=1; // skip the first one in the higher level
								break;
							}
						} else {
							valToAdd=ts.rank-firstAtHigherLevel.rank;
							if(valToAdd>1) {
								interrupts.add(new TimestampWithRankAndPointers(startTimeOfNextTS, valToAdd/2));
								interrupts.add(new TimestampWithRankAndPointers(firstAtHigherLevel.getTime(), valToAdd/2));
//								System.err.println("Added " + valToAdd/2 + " at (" + startTimeOfNextTS + "-" + firstAtHigherLevel + ")");
							} else {
								interrupts.add(new TimestampWithRankAndPointers(firstAtHigherLevel.getTime(), valToAdd));								
//								System.err.println("Added " + valToAdd + " at (" + firstAtHigherLevel + ")");
							}
							timestampId=1;
							startTimeOfNextTS=firstAtHigherLevel.getTime();
							break;							
//							interrupts.add(new TimestampWithRankAndPointers(startTimeOfNextTS, valToAdd/2));
//							interrupts.add(new TimestampWithRankAndPointers(ts.getTime(), valToAdd/2));
//							System.err.println("Added " + valToAdd/2 + " at (" + startTimeOfNextTS + "-" + ts.getTime() + ")");
//							timestampId=1; // skip the first one in the higher level
//							startTimeOfNextTS=ts.getTime();
//							break;							
						}
					}
				} else { // levelid=0
//					if (timestampId==0) { // i have 1 element leftover from the previous level
//						interrupts.add(new TimestampWithRankAndPointers(startTimeOfNextTS, 1));
//						System.err.println("Added " + 1 + " at (" + startTimeOfNextTS + ")");
//					}
					
					for (;timestampId<level.size();timestampId++) {
						TimestampWithRankAndPointers ts = level.get(timestampId);
						// then i add startTime and endTime fully
						interrupts.add(new TimestampWithRankAndPointers(ts.getTime(), 1));
//						System.err.println("Added " + 1 + " at (" + ts.getTime() + ")");
					}
				}
			}
//			System.err.println("Arrived here");
		}
		Collections.sort(interrupts);
		for (TimestampWithRankAndPointers ts:interrupts) {
			merged.updateByMany(ts.getTime(), (float) ts.getRank());
		}
//		for (ArrayDeque<TimestampWithRankAndPointers> ad:merged.levels) if (ad.size()>0) System.err.print("[t:" + ad.getFirst().getTime() + ",r:" + ad.getFirst().getRank() + "] "+ "\t");
//				System.err.println(" ");
		return merged;
	}
	
	int lastEvictedRank=0;
	static int id=0;
	int myId;
	public detWaveOptDeque(detWaveOptDeque dw) {
		this.myId=id++;
		this.epsilon=dw.epsilon;
		this.maxEvents=dw.maxEvents;
		this.numberOfLevels=dw.numberOfLevels;
		this.maxLevelator = dw.maxLevelator;
		this.eventsPerLevel=dw.eventsPerLevel;
		this.eventsPerLevelHalf=dw.eventsPerLevelHalf;
		this.levels = new ArrayDeque[this.numberOfLevels];
		for (int cnt=0;cnt<numberOfLevels;cnt++) {
			this.levels[cnt]=dw.levels[cnt].clone();
		}
		this.windowSize=dw.windowSize;
	}
	
	public detWaveOptDeque(double epsilon, int windowSize, long maxEvents, int maxLevels) {
		this.epsilon=epsilon;
		this.maxEvents=maxEvents;
		int tmpNumberOfLevels = (int) Math.ceil(Math.log(2d*epsilon*(double)maxEvents)/Math.log(2));
		numberOfLevels=Math.max(tmpNumberOfLevels, maxLevels);
		maxLevelator=(int) Math.pow(2,numberOfLevels-1);
		eventsPerLevel=(int) Math.ceil(1d/epsilon+1d);
		eventsPerLevelHalf= (int) Math.ceil(eventsPerLevel/2d);
		this.levels = new ArrayDeque[this.numberOfLevels];
		for (int cnt=0;cnt<numberOfLevels;cnt++)this.levels[cnt]=new ArrayDeque<TimestampWithRankAndPointers>(this.eventsPerLevel);
		this.windowSize=windowSize;
	}

	final long maxLevelator;
	public detWaveOptDeque(double epsilon, int windowSize, long maxEvents) {
		this.epsilon=epsilon;
		this.maxEvents=maxEvents;
		numberOfLevels = (int) Math.ceil(Math.log(2d*epsilon*(double)maxEvents)/Math.log(2));
		maxLevelator=(long) Math.pow(2,numberOfLevels-1);
		eventsPerLevel=(int) Math.ceil(1d/epsilon+1d);
		eventsPerLevelHalf= (int) Math.ceil(eventsPerLevel/2d);
		this.levels = new ArrayDeque[this.numberOfLevels];
		for (int cnt=0;cnt<numberOfLevels;cnt++)this.levels[cnt]=new ArrayDeque<TimestampWithRankAndPointers>(this.eventsPerLevel);
		this.windowSize=windowSize;
	}
	
	public double getEstimationRealtime(int query) {
//		if (query==0)
//			System.err.println("Test");
		if (fixedLevels==null)
			prepareForMerging();
		ArrayList<TimestampWithRankAndPointers>[] levels = fixedLevels;
		if (levels[0].size()==0) return 0;
		TimestampWithRankAndPointers startQueryTime = new TimestampWithRankAndPointers(query, 0);

		// find maxbef and minaft
		for (int l=0;l<numberOfLevels;l++) {
			if (levels[l].size()>0 && levels[l].get(0).isBefore(startQueryTime)) { // else go one level deeper
				ArrayList<TimestampWithRankAndPointers> level = levels[l];
				int start = Collections.binarySearch(level, startQueryTime); // returns (-(insertion point) - 1)
				if (start<0) 
					start = -start-1;
				while(start>0 && level.get(start-1).time==query) start--; 

				if (start==level.size()) 
					return 0; // this didn't happen yet
				if (level.get(start).getTime()==query) 
					return rank + 1d  - level.get(start).rank;
				else if (l==0) { // unit
					int minaft=start;
					return rank + 1d  - (double)(level.get(minaft).rank);
				}
				else {
					int minaft=start;
					int maxbef=start-1;
					double maxbefrank = level.get(maxbef).rank;
					double minaftrank=level.get(minaft).rank;
					double drank=rank;
					return drank + 1d  - (maxbefrank+minaftrank)/2d;
				}
			}
		}
		// if i arrived here, it means that the query time is in the first few time units (OR outside the query range, for which i don't care)
		double minRank=levels[0].get(0).rank;
		for (ArrayList<TimestampWithRankAndPointers> level:levels) {
			if (level.size()>0)
				minRank=Math.min(minRank,level.get(0).rank);
		}
		return rank-(minRank+lastEvictedRank)/2d;

//		return ((double)rank) + 1d  - (minRank)/2d; // out of my query range
	}
	TimestampWithRankAndPointers first,last;

	public void removeExpiredWithExpiryTime(final int expiryTimeT) {
		if (expiryTimeT<0) 
			return;
//		if (first.time<expiryTimeT) {// all expired
//			this.first=null;
//			this.last=null;
//			for (int cnt=0;cnt<numberOfLevels;cnt++)this.levels[cnt]=new ArrayDeque<TimestampWithRankAndPointers>(this.eventsPerLevel);
//		}
		while (last.next.time<expiryTimeT) {
			TimestampWithRankAndPointers ts = last;
			int level = ts.level;
			lastEvictedRank=Math.max(ts.rank,lastEvictedRank);
			if (levels[level].getFirst()==ts) // it can be that due to the max-size, the item is removed 
				levels[level].pollFirst();
//			else
//				System.err.println("Why");
			last=last.next;
			if (last==null) 
				break; 
			last.previous=null;
		}
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
		removeExpiredWithExpiryTime(currentRealtime-windowSize);
	}
	
	final int eventsPerLevelHalf;
	final int getOptimizedEventsPerLevel(int level) {
		if (level==numberOfLevels-1)
			return eventsPerLevel;
		else
			return eventsPerLevelHalf;
	}
	static final levelator levelator = new levelator(28);
	int cnt=0;
	public void addAOne(int time) {
		rank++;
//		System.err.println(rank + " > " + myId);
//		if (rank==117534)
//			System.err.println("GTEst");
		TimestampWithRankAndPointers ts = new TimestampWithRankAndPointers(time, rank);
		if (first!=null) {
			first.next=ts;
			ts.previous=first;
			first=ts;
		} else { // first element ever
			last=ts;
			first=ts;
			last.next=ts; // self-reference, which will clear up in the next arrival!
		}
		currentRealtime=time;
		lastOneUpdate=time;
		removeExpiredWithExpiryTime(currentRealtime-windowSize);
		// find which level should accept it
		int level=levelator.getCachedLevel(rank, maxLevelator);
		ts.setLevel(level);
		levels[level].addLast(ts);
		if (levels[level].size()>getOptimizedEventsPerLevel(level)) {
			TimestampWithRankAndPointers tsRemoved=levels[level].pollFirst();
			TimestampWithRankAndPointers previous=tsRemoved.previous;
			TimestampWithRankAndPointers next=tsRemoved.next;
			if (last==tsRemoved) last=tsRemoved.next;
			if (first==tsRemoved) first=tsRemoved.previous; // this should never happen, only when all items expire --> resetting
			if (previous!=null) 
				previous.next=next;
			if (next!=null) next.previous=previous;
		}
	}

	// required memory in Kbytes!
	public double getRequiredMemory() {
		int realtimeBits = 32;
		int pointerSize=32;
		int numberOfPos = (int)( Math.ceil(0.5d * (1d/epsilon + 1d))*(numberOfLevels-1d)  + Math.ceil((1d/epsilon + 1d)) );
		int actualPositions = 0;
		for (ArrayDeque ad:levels) {
			actualPositions+=ad.size();
		}
		int counterBits = 0;//(int) Math.ceil(Math.log(2d*maxEvents)/Math.log(2));
		int sizeOfEachPos = realtimeBits+counterBits;
		// two counters per bucket: startTime and realTime
	//	double oldmem=numberOfPos*(sizeOfEachPos+2*pointerSize);
		double mem = numberOfPos*sizeOfEachPos + actualPositions*2*pointerSize;
		return mem/8d/1024d;
	}
	
	public int improveSpaceOnlyCountElements() {
		int maxOptQueueSize=(int)( Math.ceil(0.5d * (1d/epsilon + 1d)));
		int elements=0;
		for (int i=0;i<this.levels.length-1;i++) {
			int levelElements=0;
			int modValNext=(int)Math.pow(2, i+1);
			for (TimestampWithRankAndPointers ts:this.levels[i]) {
				if (ts.rank%modValNext>0) // keep
					levelElements++;				
			}
			levelElements=Math.min(levelElements, maxOptQueueSize);
			elements+=levelElements;
		}
		elements+=Math.min(this.levels[this.levels.length-1].size(),2*maxOptQueueSize); // i need to keep the last queue completely
		return elements;
	}
	
	public double getRequiredNetwork() {
		int elements=improveSpaceOnlyCountElements();
		int realtimeBits = 32;
		int sizeOfEachPointer=32;
		int sizeOfEachPos = realtimeBits + sizeOfEachPointer*2;
		// two counters per bucket: startTime and realTime
		double mem = elements*sizeOfEachPos;
		return mem/8d/1024d;
	}
	
	double keepInMem=0;
	public void updateByMany(int realtimeNow, float val) {
		double toInsert=keepInMem+val;
		while (toInsert>=1) {
			addAOne(realtimeNow);
			toInsert--;
			if (toInsert<1) keepInMem=toInsert;
		}
		if (keepInMem<1e-5) keepInMem=0;
	}
	
	public String toString() {
		StringBuilder strLevels = new StringBuilder("\nRank="+rank+"\n");
//		for (int l=0;l<numberOfLevels;l++) {
//			strLevels.append("\nLevel " + l + ":" + levels[l].size() + " HAS ");
//			Iterator<TimestampWithRankAndPointers> iter = levels[l].iterator();
//			while (iter.hasNext())
//				strLevels.append(" " + iter.next() +",");
//		}
		return strLevels.toString();
	}
	public String toString2() {
		StringBuilder strLevels = new StringBuilder("\nRank="+rank+"\n");
		for (int l=0;l<numberOfLevels;l++) {
			ArrayList<TimestampWithRankAndPointers> level = new ArrayList<TimestampWithRankAndPointers>(levels[l]);
			for (int i=0;i<=Math.min(eventsPerLevel,levels[l].size()-1);i++) {
				strLevels.append("(" + level.get(i).getTime() +","+ level.get(i).getRank()+")" + "\t");
			}
			strLevels.append("\n");
		}
		return strLevels.toString();
	}
	@Override
	public void removeExpired(int currentTime) {
		// TODO Auto-generated method stub
	}
	public double getEstimationRange(int len) {
		int query = this.lastSyncedTime-len;
		return getEstimationRealtime(query);
	}
	public double getEpsilon() {
		return epsilon;
	}
	public int getLastUpdateTime() {
		return 0;
	}
	@Override
	public Pair getEstimationRealtimeWithExpiryTime(int startTime, int queryLength) {
		return null;
	}

}

class levelator {
	short getCachedLevel(int rank, long numberOfLevels) {
		short level=0;
		for (level=0;level<numberOfLevels;level++) {
			if ((rank & andVector[level])==andVector[level]) break;
		}
		return level;
	}
	
	int[] andVector = new int[32];

	
	public levelator(int maxLevels) {
		for (int i=0;i<32;i++) {
			andVector[i] = (int)Math.pow(2,i);
		}
	}
}
class levelatorOld {
	short[]levels;
	
	short getLevelFirstAlgorithm(int rank, int numberOfLevels) {
		short level=0;
		for (level=0;level<numberOfLevels;level++) {
			int mod = (int) Math.pow(2, level+1);
			if (rank%mod!=0 || level+1==numberOfLevels) break;
		}
		return level;
	}

	short getLevelSecondAlgorithm(int rank, int numberOfLevels) {
		short level=0;
		for (level=0;level<numberOfLevels;level++) {
			if ((rank & andVector[level])==andVector[level]) break;
		}
		return level;
	}
	
	int[] andVector = new int[32];

	void prepareTable(int maxLevels) {
		int maxToKeep=(int)Math.pow(2,maxLevels-1);
		levels=new short[maxToKeep];
		for (int rank=1;rank<maxToKeep;rank++) {
			short level=getLevelFirstAlgorithm(rank,maxLevels);
			levels[rank-1]=level;
//			int sec = getLevelSecondAlgorithm(rank, maxLevels);
//			if (sec!=levels[rank-1])
//				System.err.println("probl");
		}
	}
	
	short getCachedLevel(int rank, int max) {
		int pos=(int)((rank-1)%max);
		return levels[pos];
	}
	
	public levelatorOld(int maxLevels) {
		for (int i=0;i<32;i++) {
			andVector[i] = (int)Math.pow(2,i);
		}

		this.prepareTable(maxLevels);
	}
}