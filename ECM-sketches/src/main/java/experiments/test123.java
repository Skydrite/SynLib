package experiments;

import java.util.Random;

import structure.slidingCMSketch;
import structure.slidingCMSketch.sliding_window_structures;

public class test123 {
	public static void main(String[]args){
		Random rn=new Random();
		int distinct=10;
		int rep=100000000;
		try {Thread.sleep(1000);} catch (Exception e){};
		int[] ints = new int[rep];
		slidingCMSketch scm = new slidingCMSketch(0.1, 0.15, 10000,10000,sliding_window_structures.EC, 0, false);
		for (int i=0;i<rep;i++) {
			ints[i]=rn.nextInt(distinct);
		}
		System.err.println("STarting");
		long timeStart = System.currentTimeMillis();
		for (int i=0;i<rep;i++) {
			scm.add(ints[i],i);
		}
		long timeStop=System.currentTimeMillis();
		double dur = (timeStop-timeStart)/1000d;
		System.err.println(scm.get(3, rep-100));
		System.err.println("Time taken is " + dur + " and ratio is " + (rep/dur) );
	}
}
