package structure;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Random;

import cern.jet.random.engine.MersenneTwister;

import dataGeneration.StreamConstructor;

public class CMSketch {
	final Random rn = new Random();
	final int w, d; // w=mod, d=levels
	final int[][] array;
	public void batchUpdate(StreamHD s) {
		for (EventHD e:s.getEvents()) 
			add(e.getEvent(), e.getTime());
	}

	
	public String toString() {
		return "w:"+w + " and d:"+d;
	}
	public CMSketch(double delta, double epsilon) {
		double epsilonEach=epsilon;
		w=(int)Math.ceil(Math.E/epsilonEach);
		d=(int)Math.ceil(Math.log(1d/delta));
		array = new int[w][d];
		// initialize a and b
		MersenneTwister mt = new MersenneTwister(1234);
		alphas=new long[d];
		betas=new long[d];
		for (int i=0;i<d;i++) {
			alphas[i]=Math.abs(mt.nextInt());
			betas[i]=Math.abs(mt.nextInt());
		}
	}
	public CMSketch(double delta, double epsilon, int[][]array) {
		double epsilonEach=epsilon;
		w=(int)Math.ceil(Math.E/epsilonEach);
		d=(int)Math.ceil(Math.log(1d/delta));
		this.array = array;
		// initialize a and b
		MersenneTwister mt = new MersenneTwister(1234);
		alphas=new long[d];
		betas=new long[d];
		for (int i=0;i<d;i++) {
			alphas[i]=Math.abs(mt.nextInt());
			betas[i]=Math.abs(mt.nextInt());
		}
	}

	int [] prepareRandomValues(int number){
		int[] randomValues=new int[number];
		MersenneTwister mt = new MersenneTwister(1234);
		for (int cnt=0;cnt<number;cnt++)
			randomValues[cnt]=mt.nextInt();
		return randomValues;
	}

	long[] alphas;
	long[] betas;

//	int []hash(int type, int levels, int mod) {
//		int [] hash = new int[levels];
//		rn.setSeed(type); rn.nextInt();
//		for (int i=0;i<levels;i++) hash[i]=rn.nextInt(mod);
//		return hash;
//	}

	final int []hash(int type, int levels, int mod) {
		return hashRandom(type, levels, mod);
	}
	final int []hashFn(int type, int levels, int mod) {
		System.err.println("This function is repetitive on the mod, don't use it!");
		int [] hash = new int[levels];
		for (int i=0;i<levels;i++) hash[i]=(int)((alphas[i]*(type)+betas[i])%mod);
		return hash;
	}
	synchronized int []hashRandom(int type, int levels, int mod) {
		int [] hash = new int[levels];
		rn.setSeed(type);
		for (int i=0;i<levels;i++) hash[i]=rn.nextInt(mod);
		return hash;
	}
	
	public void add(int type, int time) {
		int[]hashes = hash(type, d, w);
		for (int depth=0;depth<d;depth++) {
			int w = hashes[depth];
			array[w][depth]++;
		}
	}

	public double get(int type) {
		int[]hashes = hash(type, d, w);
		double val = Double.MAX_VALUE;
		for (int depth=0;depth<d;depth++) {
			int w = hashes[depth];
			val = Math.min(val,array[w][depth]);
		}
		return val;
	}
	
	static int[] computeHDGroundTruth(EventHD[] events, int numberOfTypes) {
		int[] results = new int[numberOfTypes]; // all are zeros initially
		for (EventHD e:events) results[e.getEvent()]++;
		return results;
	}

	static void testSingle(int repeats) {
		int EVENT_TYPES=100000;
		int maxNumberOfEvents=2000000;
		Random rn = new Random(System.currentTimeMillis());
		StreamConstructor sc = new StreamConstructor(rn.nextInt());
		for (double delta=0.1;delta<=0.32;delta*=2) {
			for (double epsilon=0.001;epsilon<=0.2;epsilon+=0.1) {
				double signerr=0;		
				double avgError=0;

				for (int repeat=0;repeat<repeats;repeat++) {
					rn.setSeed(repeat);

					int numberOfEvents = maxNumberOfEvents;//Math.max(100000, rn.nextInt(maxNumberOfEvents));
					Stream stream = sc.constructPoissonStream(numberOfEvents, rn.nextInt(100));
					StreamHD shd = sc.constructHighDimensionalStreamExponential(stream, EVENT_TYPES);
					
					CMSketch slidingCM = new CMSketch(delta, epsilon);
					slidingCM.batchUpdate(shd);
					EventHD[] events = shd.getEvents();
					int[] accurateAnswers = CMSketch.computeHDGroundTruth(events, EVENT_TYPES); 

					for (int i=0;i<EVENT_TYPES;i++) {
						double est = slidingCM.get(i);
						double absoluteErr = Math.abs(est-accurateAnswers[i]);
						double relErr=absoluteErr/(double)numberOfEvents;
						if (relErr>epsilon) signerr++;

						avgError+=relErr;
					}
					avgError/=(double)(repeats*EVENT_TYPES);
				}
				avgError/=(double)(repeats);
				System.err.println("Avg error is " + avgError+  " with epsilon " + epsilon );
				if (signerr/(double)EVENT_TYPES>delta) {
					System.err.println("Error probability surpassed: "+signerr/(double)EVENT_TYPES + " where delta="+delta);
				} else {
					System.err.println("Error probability ok: "+signerr/(double)EVENT_TYPES + " where delta="+delta);
				}
			}
		}
	}
	public static void main(String[]args){
	//	main1(null);
		main2(null);
	}
	public static void main2(String[] args) {}

	public static void main1(String[] args) {
		System.err.println("First standard cm");
		{
			CMSketch cm0 = new CMSketch(0.001, 0.2);
			long start1=getGCStats();
			long timeStart = System.currentTimeMillis();
			CMSketch cm1 = new CMSketch(0.001, 0.4);
			System.err.println(cm1.w + "x" + cm1.d);
			for (int cnt=0;cnt<100000000;cnt++) {cm1.add(cnt, cnt);}
			long timeStop = System.currentTimeMillis();
			long stop1=getGCStats();
			System.err.println(timeStop-timeStart-(stop1-start1));
		}
		{
			long start1=getGCStats();
			long timeStart = System.currentTimeMillis();
			CMSketch cm1 = new CMSketch(0.001, 0.2);
			System.err.println(cm1.w + "x" + cm1.d);
			for (int cnt=0;cnt<100000000;cnt++) {cm1.add(cnt, cnt);}
			long timeStop = System.currentTimeMillis();
			long stop1=getGCStats();
			System.err.println(timeStop-timeStart-(stop1-start1));
		}
		{
			long start1=getGCStats();
			long timeStart = System.currentTimeMillis();
			CMSketch cm1 = new CMSketch(0.001, 0.04);
			System.err.println(cm1.w + "x" + cm1.d);
			for (int cnt=0;cnt<100000000;cnt++) {cm1.add(cnt, cnt);}
			long timeStop = System.currentTimeMillis();
			long stop1=getGCStats();
			System.err.println(timeStop-timeStart-(stop1-start1));
		}
		{
			long start1=getGCStats();
			long timeStart = System.currentTimeMillis();
			CMSketch cm1 = new CMSketch(0.001, 0.001);
			System.err.println(cm1.w + "x" + cm1.d);
			for (int cnt=0;cnt<100000000;cnt++) {cm1.add(cnt, cnt);}
			long timeStop = System.currentTimeMillis();
			long stop1=getGCStats();
			System.err.println(timeStop-timeStart-(stop1-start1));
		}
	}
	public static long getGCStats() {
	    long garbageCollectionTime = 0;
	    for(GarbageCollectorMXBean gc :
	            ManagementFactory.getGarbageCollectorMXBeans()) {
	        long time = gc.getCollectionTime();

	        if(time >= 0) {
	            garbageCollectionTime += time;
	        }
	    }
	    long res = garbageCollectionTime;
	    return res;
	}
}
