package structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import cern.jet.random.engine.MersenneTwister64;

public class randWaveDeque  implements slidingwindow {
	boolean generic=true;
	int c;
	int rank;
	public final ArrayDeque<TimestampWithRank> levels[];
	final double epsilon;
	final int windowSize;
	int currentRealtime=0;
	int lastOneUpdate=0;
	final long maxEvents;
	final int numberOfLevels;
	final int eventsPerLevel;
	final long NPrime;
	final int LogNPrime;
	final double [] levelProbabilities;
	final long q, r;
	static final double log2 = Math.log(2);
	private int lastSyncedTime;
	public randWaveDeque clone() {
		return new randWaveDeque(this);
	}
	
	public void setLastSyncedTime(int t) {
		this.lastSyncedTime=t;
	}
	public int getLastSyncedTime() {
		return this.lastSyncedTime;
	}

	public static final double log2(double val) {
		return Math.log(val)/log2;
	}

	public void setRank(int rank) {
		this.rank=rank;
	}
	final void setLevel(int levelid, ArrayDeque<TimestampWithRank> level) {
		this.levels[levelid]=level;
	}
	
	final int mask[] = { 1,2, 4, 8, 16, 32, 64, 128, 256,512,1024,2048,4096,8192, 16384, 32768,65536,131072,262144,524288,1048576,
			2097152,4194304,8388608, 16777216,33554432,67108864,134217728,268435456,536870912,1073741824};
	

	final Random rn=new Random();
	public final int getRandomLevel(long val) {
		val%=NPrime;
		long v = Math.abs( ( (q*val)%NPrime + r)%NPrime);
		if (v==0) v=1;
		int level = LogNPrime - (int)Math.floor(Math.log(v)/Math.log(2))-1;
		if (level<0) {
			System.err.println("Problem - negative level");
			System.exit(-1);
		}
		return level;
	}
		
//	boolean saveMemory=false;
	public randWaveDeque(randWaveDeque source) {
		this.epsilon=source.epsilon;
		this.maxEvents=source.maxEvents;
		this.NPrime=(long)Math.pow(2,Math.ceil(Math.log(2l*maxEvents)/Math.log(2)));
		this.q=source.q;
		this.r=source.r;
		numberOfLevels = source.numberOfLevels;
		levelProbabilities=source.levelProbabilities.clone(); // the sum of these probabilities will be 1!		
		eventsPerLevel=source.eventsPerLevel;
		this.levels = new ArrayDeque[source.numberOfLevels];
		for (int cnt=0;cnt<numberOfLevels;cnt++) {
			this.levels[cnt] = source.levels[cnt].clone();
		}
		this.windowSize=source.windowSize;
		this.LogNPrime = source.LogNPrime;
		this.c=source.c;
	}

	public randWaveDeque(double epsilon, int windowSize, long maxEvents, int instanceForHashing, int expectedElements, int c) {
		this.epsilon=epsilon;
		this.maxEvents=maxEvents;
		this.c=c;
		this.NPrime=(long)Math.pow(2,Math.ceil( log2(2l*maxEvents) ));
		final MersenneTwister64 mt = new MersenneTwister64(instanceForHashing);
		this.LogNPrime = (int) log2(NPrime);
		this.q=Math.abs(mt.nextInt())%NPrime;
		this.r=Math.abs(mt.nextInt())%NPrime;
		numberOfLevels = LogNPrime+1;
		levelProbabilities=new double[numberOfLevels]; // the sum of these probabilities will be 1!
		for (int cnt=0;cnt<levelProbabilities.length-1;cnt++) {
			levelProbabilities[cnt]=1/Math.pow(2, cnt+1);
		}
		levelProbabilities[levelProbabilities.length-1]=1/Math.pow(2, levelProbabilities.length-1);
		
		eventsPerLevel=(int) Math.ceil((double)c/Math.pow(epsilon, 2));
		this.levels = new ArrayDeque[this.numberOfLevels];
		for (int cnt=0;cnt<numberOfLevels;cnt++) {
			this.levels[cnt]=new ArrayDeque<TimestampWithRank>(eventsPerLevel+1);	
		}
		this.windowSize=windowSize;
	}

	public int getNumberOfLevels() {
		return levels.length;
	}
	public ArrayDeque<TimestampWithRank>[] getLevels() {
		return levels;
	}

	public int getRank() {
		return rank;
	}
	
	public static int[] computeGroundTruth(int[]queryTimes, Event[] events) {
		int numberOfQueries=queryTimes.length; 
		// assume that it is sorted!

		int[] accurateAnswers = new int[numberOfQueries]; // accurateAnswers is usually like 1,2,3,4,5... but in case of 
		  // multiple events at the same timestamp, this may not be the case. So we recompute it
		
		int queryId=0;
		int i=events.length-1;
		while (i>=0) {
			Event e = events[i];
			if (e.comesAtOrAfter(queryTimes[queryId])) {
				accurateAnswers[queryId]++;
			} else { // try next query
				accurateAnswers[queryId+1]=accurateAnswers[queryId];
				queryId++;
				i++; // redo this event
			}
			i--;			
		}
		queryId++;
		while (queryId<=accurateAnswers.length-1 && accurateAnswers[queryId]==0) 
			accurateAnswers[queryId]=accurateAnswers[queryId++-1];
		
		return accurateAnswers;
	}
	


	public double getEstimationRealtime(int query) {
		TimestampWithRank startQueryTime = new TimestampWithRank(query, 0);

		// find maxbef and minaft
		for (int l=0;l<numberOfLevels;l++) {
			if (levels[l].size()>0 && levels[l].getFirst().isBefore(startQueryTime)) { // else go one level deeper
				ArrayList<TimestampWithRank> level = new ArrayList(levels[l]);
				int start = Collections.binarySearch(level, startQueryTime); // returns (-(insertion point) - 1)
				if (start<0) 
					start = -start-1;
				else {// found
					while (level.get(start-1).time==startQueryTime.time) 
						start--; 
				}
				if (start==level.size()) {
					if (l==0)
						return 0; // this didn't happen yet
					else
						return levels[l-1].size()*Math.pow(2, l-1);
				}
				else 
					return (level.size()-start)*Math.pow(2, l);
			}
		}
		// if i arrived here, it means that the query time is in the first few time units (OR outside the query range, for which i don't care)
		// find the first level that is not yet full
		for (int l=0;l<numberOfLevels;l++) {
			if (levels[l].size()<eventsPerLevel)
				return (levels[l].size()+1)*Math.pow(2, l);
		}
		// the following should never happen, unless i am querying for an expired position!!!!
		return rank; //(levels[numberOfLevels-1].size()+1)*Math.pow(2, numberOfLevels-2);
	}
	
//	public double getEstimationRealtimeAccurate(int query) {
//		TimestampWithRank startQueryTime = new TimestampWithRank(query, 0);
//
//		// find maxbef and minaft
//		for (int l=0;l<numberOfLevels;l++) {
//			if (levels[l].size()>0 && levels[l].getFirst().isBefore(startQueryTime)) { // else go one level deeper
//				ArrayList<TimestampWithRank> level = new ArrayList(levels[l]);
//				int start = Collections.binarySearch(level, startQueryTime); // returns (-(insertion point) - 1)
//				if (start<0) 
//					start = -start-1;
//				if (start==level.size()) 
//					return 0; // this didn't happen yet
//				if (level.get(start).getTime()==query) 
//					return rank + 1d  - level.get(start).rank;
//				else if (l==0) { // unit
//					int minaft=start;
//					return rank + 1d  - (2d*level.get(minaft).rank)/2d;
//				}
//				else {
//					int minaft=start;
//					int maxbef=start-1;
//					return rank + 1d  - (level.get(maxbef).rank+level.get(minaft).rank)/2d;
//				}
//			}
//		}
//		// if i arrived here, it means that the query time is in the first few time units (OR outside the query range, for which i don't care)
//		double minRank=levels[0].getFirst().rank;
//		for (ArrayDeque<TimestampWithRank> level:levels) {
//			if (level.size()>0)
//				minRank=Math.min(minRank,level.getFirst().rank);
//		}
//		return rank + 1d  - (minRank)/2d; // out of my query range
//	}
		
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
		currentRealtime = time;
		int expTime = currentRealtime - windowSize;
		if (time > windowSize) {
			for (int level = 0; level < numberOfLevels; level++) {
				// insert it at the end of level 'level'
				ArrayDeque<TimestampWithRank> lev = levels[level];
				// check for expired elements
				if (lev.size() > 0 && lev.getLast().isBefore(expTime))
					lev.clear();
				else
					while (lev.size() > 1 && lev.getSecond().isBefore(expTime)) {
						lev.pollFirst();
					}

			}
		}
	}
	
	public void addAOne(int time) {
		rank++;
		TimestampWithRank ts = new TimestampWithRank(time, rank);
		currentRealtime=time;
		lastOneUpdate=time;

		// find the proper levels to update
		int levelsToUpdate = getRandomLevel(time);	 // from 0 to maxLevels	
		// find which level should accept it
		for (int level=0;level<=levelsToUpdate;level++) {
			// insert it at the end of level 'level'
			//ArrayDeque<TimestampWithRank> lev=levels[level];
//			if (time>windowSize) { // check for expired elements
//				if (lev.size()>0 && lev.getLast().isBefore(expTime))
//					lev.clear();
//				else				
//					while (lev.size()>1 && lev.getSecond().isBefore(expTime)) {
//						lev.pollFirst();
//					}
//			}
			if (levels[level].size()==this.eventsPerLevel) {
				levels[level].pollFirst();
			}
			levels[level].addLast(ts);
		}
	}
	
	public void addAOneOld(int time) {
		rank++;
		TimestampWithRank ts = new TimestampWithRank(time, rank);
		currentRealtime=time;
		lastOneUpdate=time;
		if (time>windowSize) // check for expired elements
			removeExpiredWithExpiryTime(currentRealtime-windowSize);
		// find the proper levels to update
		int levelsToUpdate = getRandomLevel(rank);	 // from 0 to maxLevels	
		// find which level should accept it
		for (int level=0;level<=levelsToUpdate;level++) {
			// insert it at the end of level 'level'
			if (levels[level].size()==this.eventsPerLevel) levels[level].pollFirst();
			levels[level].addLast(ts);
		}
	}

	// required memory in Kbytes!
	public double getRequiredMemory() {
		int realtimeBits = 32;
		int numberOfPos = (eventsPerLevel+1)*numberOfLevels;
		int sizeOfEachPos = realtimeBits; // no counter bits required!
		double mem = numberOfPos*sizeOfEachPos;
		return mem/8d/1024d;
	}
	// required memory in Kbytes!
	public double getRequiredNetwork() {
		int realtimeBits = 32;
		int numberOfPos = 0;
		for (ArrayDeque ad:this.levels) numberOfPos+=ad.size();
		int sizeOfEachPos = realtimeBits; // no counter bits required!
		// two counters per bucket: startTime and realTime
		double mem = numberOfPos*sizeOfEachPos;
		return mem/8d/1024d;
	}

	public double getMaxRequiredMemory() {
		return getRequiredMemory();
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
		for (int l=0;l<5;l++) {
			ArrayList<TimestampWithRank> lev = new ArrayList<TimestampWithRank>(levels[l]);
			for (int i=0;i<=Math.min(eventsPerLevel,levels[l].size()-1);i++) {
				strLevels.append("(" + lev.get(i).getTime() +","+ lev.get(i).getRank()+")" + "\t");
			}
			strLevels.append("\n");
		}
		return strLevels.toString();
	}

	@Override
	public void cloneForQuerying() {
		// TODO Auto-generated method stub
		
	}
	public void removeExpired(int currentTime) {
		for (ArrayDeque<TimestampWithRank>lev:this.levels){
			if (lev.size()>0 && lev.getLast().isBefore(currentTime-windowSize))
				lev.clear();
			else {
				while (lev.size()>1 && lev.getSecond().isBefore(currentTime-windowSize)) {
					lev.pollFirst();
				}
			}
		}
	}
	public void removeExpiredWithExpiryTime(final int expiryTimeT) { // current time is measured in timestamps
//		TimestampWithRank expiryTime = new TimestampWithRank(expiryTimeT, 0);
		for (int l=numberOfLevels-1;l>=0;l--) {
			ArrayDeque<TimestampWithRank> level = levels[l];
			TimestampWithRank sec = level.getSecond();
			if (level.size()<2  || sec.isAfter(expiryTimeT)) 
				break;
			while (sec!=null && sec.isBefore(expiryTimeT))  {
				level.pollFirst();
				sec=level.getFirst();
			}
		}
	}
	public void removeExpiredWithExpiryTime2(final int expiryTimeT) { // current time is measured in timestamps
//		TimestampWithRank expiryTime = new TimestampWithRank(expiryTimeT, 0);
		for (int l=numberOfLevels-1;l>=0;l--) {
			ArrayDeque<TimestampWithRank> level = levels[l];
			if (level.size()<2) break;
			TimestampWithRank sec = level.getSecond();
			if (sec.isAfter(expiryTimeT)) 
				break;
			while (sec!=null && sec.isBefore(expiryTimeT))  {
				level.pollFirst();
				sec=level.getFirst();
			}
		}
	}

	public double getEstimationRange(int len) {
		int query = this.lastSyncedTime-len;
		return getEstimationRealtime(query);
	}

	public double getEpsilon() {
		return epsilon;
	}

	public int getLastUpdateTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Pair getEstimationRealtimeWithExpiryTime(int startTime, int queryLength) {
		// TODO Auto-generated method stub
		return null;
	}
}
