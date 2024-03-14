package experiments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import structure.logEventInt;
import structure.slidingCMSketch;
import structure.slidingCMSketch.sliding_window_structures;
import structure.slidingwindow;
import dataGeneration.ProcessedStreamLoaderGeneric;
import experiments.tests.collection_type;


public class testsWithOldOptimization {
	final static boolean debugMsgs=false;	
	static boolean singleLevel=false;
	static int streamDuration;
	static collection_type collection;
	public final static double ratio=1;

	
	static int repeats;
	static String path=null;
	static double defaultDelta=0.15;
	
	public static void main(String[]args) {
		//swtype: 0-EC, 1-DW, 2-RW
		// first find the time difference
		path = args[0];
		repeats = Integer.parseInt(args[1]);

		if (path.contains("snmp"))
			collection = collection_type.snmp;
		else if (path.contains("wc"))
			collection = collection_type.wc98;
		else if (path.contains("ips"))
			collection = collection_type.ips;
		
		ProcessedStreamLoaderGeneric psl = new ProcessedStreamLoaderGeneric(path);
		int start,stop;
		start = psl.getStart();
		stop = psl.getStop();

		streamDuration=2000000;//864000; // 10 days
		long maxEvents=(long)(streamDuration*1000); // one event per msec on average

		if (collection==collection_type.ips) { 
			streamDuration=200000000;
			maxEvents=(long)(streamDuration*10);
		}
		ArrayList<configuration> al = new ArrayList<>();
		{
			// single
			int numberOfStreams=1;
			configuration c=null;
			{
				c = new configuration(numberOfStreams, sliding_window_structures.EC, false, maxEvents, streamDuration, 1, 1, 0.15, 0.20, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfStreams, sliding_window_structures.EC, false, maxEvents, streamDuration, 1, 1, 0.15, 0.15, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfStreams, sliding_window_structures.EC, false, maxEvents, streamDuration, 1, 1, 0.15, 0.10, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfStreams, sliding_window_structures.DW, false, maxEvents, streamDuration, 1, 1, 0.15, 0.20, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfStreams, sliding_window_structures.DW, false, maxEvents, streamDuration, 1, 1, 0.15, 0.15, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfStreams, sliding_window_structures.DW, false, maxEvents, streamDuration, 1, 1, 0.15, 0.10, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfStreams, sliding_window_structures.RW, false, maxEvents, streamDuration, 1, 1, 0.15, 0.20, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfStreams, sliding_window_structures.RW, false, maxEvents, streamDuration, 1, 1, 0.15, 0.15, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfStreams, sliding_window_structures.RW, false, maxEvents, streamDuration, 1, 1, 0.15, 0.10, path, start, stop, collection);
				al.add(c);
			}
			// 16
			boolean useOriginalAssignmentToStreams = false;
			int siblings=2,height=-1,numberOfNodes=16;
			height = computeHeight(siblings, numberOfNodes);
			if (false)
				{
				c = new configuration(numberOfNodes, sliding_window_structures.EC, useOriginalAssignmentToStreams, maxEvents, streamDuration, siblings, height, 0.15, 0.15, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfNodes, sliding_window_structures.DW, useOriginalAssignmentToStreams, maxEvents, streamDuration, siblings, height, 0.15, 0.15, path, start, stop, collection);
				al.add(c);
				c = new configuration(numberOfNodes, sliding_window_structures.RW, useOriginalAssignmentToStreams, maxEvents, streamDuration, siblings, height, 0.15, 0.15, path, start, stop, collection);
				al.add(c);
			}
		}
		

		for (configuration c:al) {			
			// and now execute
			slidingCMSketch.oldOptimization=true;
			String[] cParams = c.toString().split(" ");
			ArrayList<String> params = new ArrayList();
			for (String s:cParams) params.add(s); params.add("1");
			testsOneShotRun.main(params.toArray(new String[params.size()])); // last 1 is number of repetitions
		}
	}
		

	static int computeHeight(int siblings, int numberOfrandWaves) {
		return (int) Math.ceil(Math.log(numberOfrandWaves)/Math.log(siblings));
	}

}
