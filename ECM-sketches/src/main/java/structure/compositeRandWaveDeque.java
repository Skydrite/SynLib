package structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


import cern.jet.random.engine.MersenneTwister;
import dataGeneration.StreamConstructor;

public class compositeRandWaveDeque  implements slidingwindow {
	final double delta;
	final int independentInstances;
	public final randWaveDeque[] instances;
	private int lastSyncedTime;
	public void setLastSyncedTime(int t) {
		this.lastSyncedTime=t;
	}
	public int getLastSyncedTime() {
		return this.lastSyncedTime;
	}
	public compositeRandWaveDeque clone() {
		return new compositeRandWaveDeque(this);
	}
	
	public String detailedtoString() {
		return "Levels:"+instances[0].getNumberOfLevels()+  " \t Instances:" + instances.length + "\n" +instances[0];		
	}
	
	public compositeRandWaveDeque(compositeRandWaveDeque c) {
		this.windowSize=c.windowSize;
		this.epsilon=c.epsilon;
		mt = new MersenneTwister(0);//(int)System.currentTimeMillis());
		this.q=c.q;
		this.r=c.r;
		this.delta=c.delta;
		this.independentInstances=c.independentInstances;
		this.instances = new randWaveDeque[independentInstances];
		for (int i=0;i<c.instances.length;i++)
			this.instances[i] = c.instances[i].clone();
	}
	
	public String toString() {
		return this.instances[0].toString();
//		return this.getStringSummary();
	}
	
	public  int getLevelOffset(TimestampWithRank ts, int numberOfLevels) { // repeats the hashing, returning a number 
		int NPrime=(int)Math.pow(2,numberOfLevels);
		int val=(int)Math.abs(ts.rank%NPrime);
		// from 0 to (numberOfLevels-1) with exponentially decreasing probability (except from the last two levels)
		long val2 = Math.abs((q*(int)val)%NPrime+ r)%NPrime;
		if (val2==0) 
			return 0;
		else
			return numberOfLevels - (int)Math.floor(randWaveDeque.log2(val2))-1;
	}

	public void setRandWaveRanks(int rank) {
		for (int instance=0;instance<instances.length;instance++) {
			instances[instance].setRank(rank);
		}
	}
	
	public void setRandWaveLevels(ArrayDeque<TimestampWithRank> levelInterrupts[][]) {
		for (int instance=0;instance<levelInterrupts.length;instance++) {
			for (int level=0;level<levelInterrupts[instance].length;level++) {
				instances[instance].setLevel(level,levelInterrupts[instance][level]);
			}
		}
	}
	public randWaveDeque getInstanceRandWave(int cnt) {
		return instances[cnt];
	}
	public ArrayList<TimestampWithRank> getInstance(int instance) {
		HashMap<Integer, TimestampWithRank> ht = new HashMap();
		for (int level=0;level<instances[instance].getNumberOfLevels();level++) { // for all levels
			for (TimestampWithRank ts:instances[instance].getLevels()[level]) { // for all timestamps of each level
				ht.put((int)ts.getRank(), ts);
			}
		}
		return new ArrayList<TimestampWithRank>(ht.values());
	}
	
	public ArrayDeque<TimestampWithRank>[] getInstanceRaw(int instance) {
		return instances[instance].getLevels();
	}

	
	public int getRank() {
		return instances[0].getRank();
	}

	final MersenneTwister mt;
	final int q,r;
	
	public static compositeRandWaveDeque mergeRandWaves(compositeRandWaveDeque[] dws, double delta, double epsilon, int windowSize, int numberOfEvents) {
		// now merge
		// START now merge with method 4
		final int eventsPerLevel=dws[0].getInstanceRandWave(0).eventsPerLevel;
		int totalEvents=0;
		totalEvents=numberOfEvents;
		compositeRandWaveDeque merged = new compositeRandWaveDeque(delta, epsilon, windowSize, totalEvents, totalEvents);

		final int numberOfLevelsInMerged=merged.getInstanceRandWave(0).getNumberOfLevels();
		ArrayList<TimestampWithRank> levelInterrupts[][] = new ArrayList[merged.getNumberOfInstances()][numberOfLevelsInMerged];

		for (int i=0;i<levelInterrupts.length;i++) 
			for (int j=0;j<levelInterrupts[0].length;j++)
				levelInterrupts[i][j]=new ArrayList<TimestampWithRank>(eventsPerLevel);

		int totalRankComputeOnce=0;
		for (int instance=0;instance<merged.getNumberOfInstances();instance++) {
			for (compositeRandWaveDeque dw:dws) {
				randWaveDeque rw = dw.getInstanceRandWave(instance);
				if (instance==0) totalRankComputeOnce+=rw.getRank();
				ArrayDeque<TimestampWithRank> levels[] = rw.getLevels();
				for (int level=0;level<Math.min(levels.length, numberOfLevelsInMerged);level++) {
					ArrayDeque<TimestampWithRank> singleLevel = levels[level];
					if (singleLevel.isEmpty()) // not possible that i have something relevant in a lower level
						break;
					if (level<levels.length-1||levels.length==numberOfLevelsInMerged) 
						levelInterrupts[instance][level].addAll(singleLevel);
					else { // half of it goes to level, and the other half to level+1
						for (TimestampWithRank ts:singleLevel) {
							int levelOffset = merged.getLevelOffset(ts, numberOfLevelsInMerged-level);
							levelInterrupts[instance][level+levelOffset].add(ts);
						}
					}
				}
			}
		}
		// now sort all level interrupts and prune the expired
		for (int instance=0;instance<merged.getNumberOfInstances();instance++) 
		{
			for (int level=0;level<numberOfLevelsInMerged;level++) {
				Collections.sort(levelInterrupts[instance][level]);
				if (levelInterrupts[instance][level].size()>eventsPerLevel) {
					//while (levelInterrupts[instance][level].size()>eventsPerLevel)  levelInterrupts[instance][level].remove(levelInterrupts[instance][level].size()-1);
					ArrayList<TimestampWithRank> tmp= new ArrayList<TimestampWithRank>(eventsPerLevel);
					tmp.addAll(levelInterrupts[instance][level].subList(levelInterrupts[instance][level].size()-eventsPerLevel, levelInterrupts[instance][level].size()));
					levelInterrupts[instance][level]=tmp;
	//				Collections.reverse(levelInterrupts[instance][level]);
				}
				levelInterrupts[instance][level].trimToSize();
			}
		}
		ArrayDeque<TimestampWithRank> levelInterruptsDeque[][] = new ArrayDeque[merged.getNumberOfInstances()][numberOfLevelsInMerged];
		for (int i=0;i<levelInterrupts.length;i++) 
			for (int j=0;j<levelInterrupts[0].length;j++)
				levelInterruptsDeque[i][j]=new ArrayDeque<TimestampWithRank>(levelInterrupts[i][j]);
		
		merged.setRandWaveLevels(levelInterruptsDeque);
		merged.setRandWaveRanks(totalRankComputeOnce);
		// no expiration check required, since the windows are time-based and not arrival-based

		return merged;
	}

	public String getStringSummary2() {
		return "Independent instances: " + this.independentInstances + " DeltaRW: " + this.delta + " EpsilonRW: " + this.epsilon;
	}
	public String getStringSummary() {
		StringBuilder sb = new StringBuilder();
		randWaveDeque instance = instances[0];
//		for (randWaveDeque instance:instances) 
		{
			for (int level=0;level<instance.getNumberOfLevels();level++) {
				if (instance.getLevels()[level].size()>0)
					sb.append(level + "(" + instance.getLevels()[level].size() + "-" +instance.getLevels()[level].getFirst().toString() + ") ");
				else break;
			}
			sb.append( " LLL ");
			
		}
		return sb.toString();
	}
	double epsilon;
	final int windowSize;
	public static int cOut=6;
	static {
		System.err.println("cOut is " + cOut);
	}
	public compositeRandWaveDeque(double delta, double epsilon, int windowSize, long maxEvents, int expectedElements) {
		this.windowSize=windowSize;
		this.epsilon=epsilon;
		mt = new MersenneTwister(0);//(int)System.currentTimeMillis());
		this.q=Math.abs(mt.nextInt());
		this.r=Math.abs(mt.nextInt());
		this.delta=delta;
		// correct 
		int indInst = (int) Math.ceil(cOut*Math.log(1d/delta)/Math.log(2));
//		int indInst = (int) Math.ceil(Math.log(1d/delta)/Math.log(3));
//		System.err.println("Setting independent instances to 1 for debugging");
//		indInst=1;
		if (indInst%2==0) // then median is no good!
			this.independentInstances=indInst+1;
		else
			this.independentInstances=indInst;

		this.instances = new randWaveDeque[independentInstances];
		for (int i=0;i<this.independentInstances;i++) {
			this.instances[i]=new randWaveDeque(epsilon, windowSize, maxEvents, i, expectedElements, cOut);
//			try {Thread.sleep(2);} catch(Exception ignored){};
		}
	}
	
	static boolean debug=false;
	public static void main(String[] args) {
		long time1 = System.currentTimeMillis();
		Random rn = new Random(1235);
		int queryLength = 10000;
		int totalStreams=256;
		double delta=0.3;
		System.err.println("SW Length is " + queryLength);
		for (int cc: new int[]{1,6,10,36}) {
			for (int streams = 2; streams <= totalStreams; streams*=2) {
				// for (float epsilon = 0.05f; epsilon < 0.53; epsilon += 0.05)
				float epsilon = 0.3f;
				{
					cOut = cc;
					compositeRandWaveDeque union = new compositeRandWaveDeque(delta, epsilon, queryLength, queryLength*totalStreams, queryLength);
					compositeRandWaveDeque[] cr = new compositeRandWaveDeque[streams];
					boolean[] eventsi[] = new boolean[totalStreams][queryLength];
					for (int i = 0; i < cr.length; i++) {
						cr[i] = new compositeRandWaveDeque(delta, epsilon,queryLength, queryLength, queryLength);
					}

					double totalerr = 0;
					double maxerr = 0;
					double Utotalerr = 0;
					double Umaxerr = 0;
					int numberOfQueries = 0;
					for (int i = 0; i < queryLength; i++) {
						if (i==9840) debug=true;
						for (int streamid = 0; streamid < totalStreams; streamid++) {
							if (rn.nextBoolean()) {
								eventsi[streamid][i] = true;
								cr[streamid%streams].addAOne(i);
								union.addAOne(i);
							}
						}
					}
//					System.err.println("Union\n" + union);
					compositeRandWaveDeque comp = mergeRandWaves(cr, delta,epsilon, queryLength*totalStreams, queryLength);
//					System.err.println("Compo\n" + comp);
					// queries
					int i = queryLength - 1;
					for (int qlen = 100; qlen <= queryLength/2; qlen += 100) {
						double ans = 0;
						for (int c = i; c >= i - qlen; c--) {
							if (c < 0)
								break;
							for (int sid = 0; sid < totalStreams; sid++)
								if (eventsi[sid][c])
									ans++;
						}
						double v = comp.getEstimationRealtime(i - qlen);
						double un = union.getEstimationRealtime(i - qlen);
						totalerr += Math.abs(v - ans) / ans;
						Utotalerr+=Math.abs(un-ans)/ans;
						maxerr = Math.max(maxerr, Math.abs(v - ans) / ans);
						Umaxerr = Math.max(Umaxerr,  Math.abs(un-ans)/ans);
						numberOfQueries++;
					}

					System.err.println("Streams " + streams + " Epsilon is " + epsilon + " c is  " + cc + 
							"\t  Errtotal " + totalerr / numberOfQueries + " max " + maxerr +
							"\t  UErrtotal " + Utotalerr / numberOfQueries + " Umax " + Umaxerr
							);
				}
			}
		}
		long time2 = System.currentTimeMillis() - time1;
		System.err.println("Total time: " + (time2) / 1000);
	}
	public static void main2(String[] args) {
		long time1 = System.currentTimeMillis();
		Random rn = new Random(123);
		int queryLength = 1000;
		System.err.println("SW Length is " + queryLength);
		for (float epsilon = 0.05f; epsilon < 0.53; epsilon += 0.1) {
			for (int cc: new int[]{1,6}) {
				cOut=cc;
				// ExponentialHistogramDeque ex = new
				// ExponentialHistogramDeque(epsilon, queryLength, 1000);
				compositeRandWaveDeque ex = new compositeRandWaveDeque(0.07, epsilon, queryLength, 100000, 100000);
				boolean[] events = new boolean[1000000];
				double totalerr = 0;
				double maxerr = 0;
				int numberOfQueries = 0;
				for (int i = 0; i < events.length; i++) {
					if (rn.nextBoolean()) {
						events[i] = true;
						ex.addAOne(i);
					}
					double ans = 0;
					for (int c = i; c >= i - queryLength; c--) {
						if (c < 0)
							break;
						if (events[c])
							ans++;
					}
					if (i<queryLength) continue;
					double v = ex.getEstimationRealtime(i - queryLength);
					totalerr += Math.abs(v - ans) / ans;
					maxerr = Math.max(maxerr, Math.abs(v - ans) / ans);
					numberOfQueries++;
				}
				System.err.println("Epsilon is " + epsilon + " c is  " + cc + "\t  Errtotal " + totalerr / numberOfQueries +  " max " + maxerr);
			}
		}
		long time2 = System.currentTimeMillis() - time1;
		System.err.println("Total time: " + (time2) / 1000);
	}
	

	public int getNumberOfInstances(){
		return independentInstances;
	}
	
	int lastTime=0;
	public void addAOne(int t) {
		for (int cnt=0;cnt<this.independentInstances;cnt++) {
			instances[cnt].addAOne(t);
		}
		lastTime=t;
	}
	public void addAZero(int t) {
		for (int cnt=0;cnt<this.independentInstances;cnt++) {
			instances[cnt].addAZero(t);
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

	public double getEstimationRealtime(int query) {
		ArrayList<Double>results = new ArrayList<Double>(this.independentInstances);
		for (int cnt=0;cnt<this.independentInstances;cnt++) 
			results.add(instances[cnt].getEstimationRealtime(query));
		Collections.sort(results);
		if (independentInstances%2==0)
			return results.get(results.size()/2-1);
		else
			return results.get(results.size()/2);
	}
	static void testSingle() {
		for (double i=0.05;i<0.31;i+=0.05) {
			compositeRandWaveDeque c1 = new compositeRandWaveDeque(i, 0.05, 1000, 1000000, 1000000);
			System.err.println(" For i =" + i + " d is " + c1.independentInstances);
		}

	}

	public int getCurrentRealtime() {
		return this.instances[0].currentRealtime;
	}


	// required memory in Kbytes!
	public double getRequiredMemory() {
		double total = 0;
		for (randWaveDeque rw:this.instances) {
			total+=rw.getRequiredMemory();
		}
		return total;
	}	
	
	// required network in Kbytes!
	public double getRequiredNetwork() {
		double total = 0;
		for (randWaveDeque rw:this.instances) {
			total+=rw.getRequiredNetwork();
		}
		return total;
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

	public void cloneForQuerying() {
	}

	public void removeExpired(int currentTime) {
		for (randWaveDeque r:this.instances) {
			r.removeExpired(currentTime);
		}
	}
	public double getEstimationRange(int range) {
		int startRangeTime = lastSyncedTime-range;		// range will give me the length of the query
		return this.getEstimationRealtime(startRangeTime);
	}
	@Override
	public double getEpsilon() {
		return epsilon;
	}
	@Override
	public int getLastUpdateTime() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public Pair getEstimationRealtimeWithExpiryTime(int startTime, int queryLength) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void removeExpiredWithExpiryTime(int startTime) {
		// TODO Auto-generated method stub
		
	}
}
