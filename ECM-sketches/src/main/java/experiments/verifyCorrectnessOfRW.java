package experiments;

import dataGeneration.ProcessedStreamLoaderGeneric;
import structure.ExponentialHistogramCircularInt;
import structure.compositeRandWaveDeque;
import structure.logEventInt;
import structure.randWaveDeque;
import structure.slidingCMSketch;
import structure.slidingCMSketch.sliding_window_structures;

public class verifyCorrectnessOfRW {
	public static int repeats = -1;
	final static boolean debugMsgs = false;

	public enum collection_type {
		wc98, snmp
	}

	static boolean singleLevel = false;
	static int streamDuration;
	static collection_type collection;
	public final static double ratio = 1;

	static String path = null;
	static double defaultDelta = 0.15;

	public static void main(String [] args) {
		main2(args);
	}
	public static void main1(String[] args) {
		String s = "";

		for (String ss : args)
			s = s + ss + " ";
		s = s.trim();
		configuration c = configuration.fromString(s);
		path = c.path;

		double avgError = 0;
		int cnt = 0;
		int[] qStarttimes  = new int[(int)Math.floor(Math.log(c.windowSizeInt)/Math.log(2))-4];
		int[] qAnswers  = new int[(int)Math.floor(Math.log(c.windowSizeInt)/Math.log(2))-4];
		for (int i=0;i<qStarttimes.length;i++) {
			qStarttimes[qStarttimes.length-i-1] = c.stop-(int)Math.pow(2,i+4);
		}

		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);
		int i=0;
		while (true) {
			logEventInt levent = psl.readNextEvent(1);
			if (levent == null) {
				break;
			}
			if (i+1<qStarttimes.length && qStarttimes[i+1]<=levent.seconds) i++;
			if (qStarttimes[i]<=levent.seconds) qAnswers[i]++;
		}
		for (i=qStarttimes.length-1;i>0;i--) {
			qAnswers[i-1] += qAnswers[i];
		}
		
		for (int cc : new int[] { 1, 5, 10, 20, 25, 30,36 }) {
			compositeRandWaveDeque.cOut = cc;
			compositeRandWaveDeque crand = new compositeRandWaveDeque(c.delta, c.epsilon, c.windowSizeInt, c.maxEvents,(int)c.maxEvents);
			psl = new ProcessedStreamLoaderGeneric(path);
			while (true) {
				logEventInt levent = psl.readNextEvent(1);
				if (levent == null) {
					break;
				}
//				if (levent.seconds>=10367950) 
//					System.err.println(levent);
				crand.addAOne(levent.seconds);
			}
			
			for (i=qStarttimes.length-1;i>=0;i--) {
				int startTime=qStarttimes[i];
				double est = qAnswers[i];
				if (est>0) {
					double est2 = crand.getEstimationRealtime(startTime);
					double thiserror=Math.abs(est2 - est) / est;
					avgError += thiserror;
					cnt++;
				}
			}
			System.err.println("For cOut=" + cc + " Avg error is " + avgError / cnt);
		}

	}
	public static void main2(String[] args) {

		// swtype: 0-EC, 1-DW, 2-RW
		// first find the time difference

		String s = "";

		for (String ss : args)
			s = s + ss + " ";
		s = s.trim();
		configuration c = configuration.fromString(s);
		path = c.path;

		double avgError = 0;
		int cnt = 0;
		int repeat = 0;
		slidingCMSketch scmEC = new slidingCMSketch(c.delta / 2, c.epsilon,
				c.windowSizeInt, c.maxEvents, sliding_window_structures.EC,
				repeat, false);
		for (int i = 0; i < scmEC.getW(); i++) {
			for (int j = 0; j < scmEC.getD(); j++) {
				scmEC.array[i][j] = new ExponentialHistogramCircularInt(0.001,c.windowSizeInt, c.maxEvents);
			}
		}

		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(
				path);
		int latestEvent = 0;
		while (true) {
			logEventInt levent = psl.readNextEvent(1);
			if (levent == null) {
				break;
			}
			scmEC.add(levent.file, levent.seconds);
			latestEvent = levent.seconds;
		}

		for (int cc : new int[] { 1, 5, 10, 20, 25, 30 }) {
			compositeRandWaveDeque.cOut = cc;
			slidingCMSketch scmRW = new slidingCMSketch(c.delta, c.epsilon, c.windowSizeInt, c.maxEvents, sliding_window_structures.RW, repeat, false);
			psl = new ProcessedStreamLoaderGeneric(path);
			while (true) {
				logEventInt levent = psl.readNextEvent(1);
				if (levent == null) {
					break;
				}
				scmRW.add(levent.file, levent.seconds);
			}
			for (int qlen = 1000; qlen < c.windowSizeInt; qlen *= 2)
				for (int i = 0; i < scmRW.getW(); i++)
					for (int j = 0; j < scmRW.getD(); j++) {
						double est = scmEC.array[i][j]
								.getEstimationRealtime(latestEvent - qlen);
						if (est>0) {
							avgError += Math.abs(scmRW.array[i][j].getEstimationRealtime(latestEvent - qlen) - est) / est;
							cnt++;
						}
					}
			System.err.println("For cOut=" + cc + " Avg error is " + avgError
					/ cnt);
		}

	}

}
