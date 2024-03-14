package structure;

import cern.jet.random.engine.MersenneTwister;

public class tmp {
	static MersenneTwister mt = new MersenneTwister(123);

	public static void main(String[] args) {
		
		levelProbabilities=new double[13]; // the sum of these probabilities will be 1!
		for (int cnt=0;cnt<levelProbabilities.length-1;cnt++) {
			levelProbabilities[cnt]=1/Math.pow(2, cnt+1);
		}
		levelProbabilities[levelProbabilities.length-1]=1/Math.pow(2, levelProbabilities.length-1);

		int[] distribution=new int[13];
		for (int cnt=1;cnt<100000000;cnt++) {
			if (mt.nextDouble()>0.3) {
				int level = getRandomLevel1(Math.abs(cnt));
				distribution[level]++;
			}
		}
		for (int cnt=0;cnt<13;cnt++) 
			System.err.println(distribution[cnt]);
	}
	

	static final int q=121, r=3241, NPrime=8192;
	static final int Log2NPrime = (int) Math.ceil(Math.log(NPrime)/Math.log(2));
	static final double log2 = Math.log(2);

	public static final double log2(double val) {
		return Math.log(val)/log2;
	}
	static double [] levelProbabilities;

	public static int getRandomLevel3(int dummy) {
		double val = mt.nextDouble();
		int level=0;
		while (val>0) { val-=levelProbabilities[level];level++; }
		level--;
		return level;
	}

	
	public static final int getRandomLevel2(long val) {
		long  val2 = Math.abs((q*val)%NPrime + r)%NPrime;
		String s = Long.toBinaryString(val2);
		int msbPos = s.length();
		return Log2NPrime-msbPos;
	}
	
	
	public static final int getRandomLevel1(long val) {
		val%=NPrime;
		long val2 = Math.abs((q*val)%NPrime + r)%NPrime;
		if (val2==0) 
			return 0;
		else
			return Log2NPrime - (int)Math.floor(log2(val2))-1;
//		if (val2==0) return Log2NPrime;
//		int res = (int)Math.floor(Math.log(val2)/Math.log(2))+1;
//		String s = Integer.toBinaryString(val2);
//		int msbPos = s.length();
//		if (res!=msbPos) 
//			System.err.println("test");
//		return Log2NPrime-msbPos;
	}
	
}
