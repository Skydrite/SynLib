package experiments;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import structure.slidingCMSketch.sliding_window_structures;
import experiments.tests.collection_type;


public class sortAverages {
	static configuration printCommand(boolean optimizeForSJ, String data, String struct, float epsilon, int numberOfStreams) {
		long streamDuration=2000000;//864000; // 10 days
		long maxEvents=(long)(streamDuration*1000d); // one event per msec on average

		if (data.contains("ips")) { 
			streamDuration=200000000;
			maxEvents=(long)(streamDuration*10d);
		}
		sliding_window_structures slidingWindowType = null;
		if (struct.contains("EC")) slidingWindowType=sliding_window_structures.EC;
		else if (struct.contains("DW")) slidingWindowType=sliding_window_structures.DW;
		else if (struct.contains("RW")) slidingWindowType=sliding_window_structures.RW;
		int windowSizeInt=(int)streamDuration;
		int siblings=1,height=0;
		if (numberOfStreams>1) {
			siblings=2;
			height = tests.computeHeight(siblings, numberOfStreams);
		}
		String path=data;
		int start=0;
		int stop=371995477;
		collection_type collectionType=collection_type.ips;
		if (data.contains("wc")) {
			stop=7518578;
			collectionType=collection_type.wc98;
		}
		configuration c = new configuration(numberOfStreams, slidingWindowType, false, maxEvents, windowSizeInt, siblings, height, 0.15, epsilon, path, start, stop, collectionType);
		return c;
	}
	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		ArrayList<config> allResults = new ArrayList<>();
		ArrayList<configMonitoring> allResultsCM = new ArrayList<>();
		while (br.ready()) {
			String line = br.readLine();
			if (line.startsWith("<<")) {
				args = line.split(",");
				boolean optimizeForSJ = Boolean.parseBoolean(args[0].substring(3));
				String data = args[1];
				String struct = args[2];
				float epsilon = ((int)100*Float.parseFloat(args[3]))/100f;
				float delta = ((int)100*Float.parseFloat(args[4]))/100f;
				int numberOfStreams = Integer.parseInt(args[5]);
				int siblings = Integer.parseInt(args[6]);
				int height = Integer.parseInt(args[7]);
				config c = new config(optimizeForSJ, data, struct, epsilon, delta, numberOfStreams, siblings, height, args);
				allResults.add(c);
			} else {
				args = line.split(",");
				
				String data = args[0].substring(2);
				String struct = args[1];
				float epsilon = ((int)100*Float.parseFloat(args[3]))/100f;
				float delta = ((int)100*Float.parseFloat(args[4]))/100f;
				int numberOfStreams = Integer.parseInt(args[5]);
				int siblings = Integer.parseInt(args[6]);
				int height = Integer.parseInt(args[7]);
				float monitoringAccuracy = ((int)100*Float.parseFloat(args[9]))/100f;
				configMonitoring c = new configMonitoring(data, struct, epsilon, delta, numberOfStreams, siblings, height, monitoringAccuracy, args);
				allResultsCM.add(c);
			}
		}
		br.close();
		
		HashSet<String> remaining = new HashSet<>();
		HashSet<String> remainingMon = new HashSet<>();
		int cnt=0;
		// now sort and print
		// loop on two datasets {ips.binary.gz, wc-full.binary.gz}
		for (String dataset:new String[]{"wc-full.binary.gz","ips.binary.gz"}) {
			// loop on point/sj queries {first field false/true}
			for (boolean optimizeForSJ: new boolean[]{false,true}) {
				// vary structures
				for (String struct:new String[]{"ExpHist-EC","DetWave-DW","RandWave-RW"}) {
					if (struct.equals("RandWave-RW") && optimizeForSJ) continue; // not applicable
					
					// first print on varying error for 1 node
					System.err.println("#" + cnt++ + " - vary error single node");
					for (float error:new float[]{0.05f,0.1f,0.15f,0.2f,0.25f,0.3f}) {
						boolean found=false;
						for (config c:allResults) {
							if (c.compareEqual(optimizeForSJ, dataset, struct, error, 1)){
								System.err.println(printLine(c.fields));
								found=true;
								break;
							}
						}
						if (!found) remaining.add(printCommand(optimizeForSJ, dataset,struct,error, 1).toString());
					}
					System.err.println("\n");
					
					// then print on varying error for -1 node
					System.err.println("#" + cnt++ + " - vary error fixed network");
					for (float error:new float[]{0.05f,0.1f,0.15f,0.2f,0.25f,0.3f}) {
						boolean found=false;
						for (config c:allResults) {
							if (c.compareEqual(optimizeForSJ, dataset, struct, error, 16)) {
								System.err.println(printLine(c.fields));
								found=true;
								break;
							}
						}
						if (!found) remaining.add(printCommand(optimizeForSJ, dataset,struct,error,16).toString());
					}
					
					System.err.println("\n");
					// then print on varying network size for fixed error
					System.err.println("#" + cnt++ + " - vary network size fixed error");
					for (int netSize:new int[]{2,4,8,16,32,64,128,256}) {
						boolean found=false;
						for (config c:allResults) {
							if (c.compareEqual(optimizeForSJ, dataset, struct, 0.15f, netSize)){
								System.err.println(printLine(c.fields));
								found=true;
								break;
							}
						}
						if (!found) remaining.add(printCommand(optimizeForSJ, dataset,struct,0.15f, netSize).toString());
					}
					System.err.println("\n");
				}				
			}
		}
		
		
		// and now the continuous monitoring
		for (String dataset : new String[] { "wc-full.binary.gz", "ips.binary.gz" }) {
			// loop on point/sj queries {first field false/true}
			{
				// vary structures
				for (String struct : new String[] { "ExpHist-EC"}) { // NA: , "DetWave-DW", "RandWave-RW" 
					// then print on varying network size for fixed error
					System.err.println("#" + cnt++ + " - MONITOR vary network size fixed error");
					for (float monerror : new float[] { 0.15f}) {
						for (int netSize : new int[] { 2, 4, 8, 16, 32, 64, 128, 256 }) {
							boolean found=false;
							for (configMonitoring c : allResultsCM) {
								if (c.compareEqual(dataset, struct, 0.15f, netSize, monerror)) {
									System.err.println(printLine(c.fields));
									found=true;
									break;
								}
							}
							if (!found) remainingMon.add(printCommand(true, dataset,struct,monerror, netSize).toString());
						}
					}
					System.err.println("\n");
				}				
				
				// vary structures
				for (String struct : new String[] { "ExpHist-EC" }) { // NA: , "DetWave-DW", "RandWave-RW"
					// then print on varying network size for fixed error
					System.err.println("#" + cnt++ + " - MONITOR vary error fixed network");
					for (float monerror : new float[] { 0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f }) {
						for (int netSize : new int[] { 16}) {
							boolean found=false;
							for (configMonitoring c : allResultsCM) {
								if (c.compareEqual(dataset, struct, 0.15f, netSize, monerror)) {
									System.err.println(printLine(c.fields));
									found=true;
									break;
								}
							}
							if (!found) remainingMon.add(printCommand(true, dataset,struct,monerror, netSize).toString());
						}
					}
					System.err.println("\n");
				}
			}
		}
		
		// and now print commands
		for (String s : remaining) {
			configuration c = configuration.fromString(s);

			int mem=0;
			if (c.slidingWindowType==sliding_window_structures.EC) {
				if (c.numberOfStreams<128)
					mem=2;
				else 
					mem=3;
			}
			if (c.slidingWindowType==sliding_window_structures.DW) {
				if (c.numberOfStreams<64)
					mem=2;
				else if (c.numberOfStreams<256)
					mem=3;
				else 
					mem=4;
			}
			if (c.slidingWindowType==sliding_window_structures.RW) {
				if (c.numberOfStreams<32)
					mem=4;
				else if (c.numberOfStreams<=64)
					mem=8;
				else 
					mem=10;
				if (c.delta<0.16 || c.epsilon<0.16)
					mem+=2;
				if (c.delta<0.11 || c.epsilon<0.11)
					mem+=2;
				if (c.delta<0.06 || c.epsilon<0.06)
					mem+=2;
			}
			
			System.out.println("java -cp ContinuousEH.jar -Xmx" + mem + "g experiments.testsOneShotRun " + c.toString() + " &> S" + c.toShortString());
		
		}
		
		for (String s : remainingMon) {
			configuration c = configuration.fromString(s);

			int mem=0;
			if (c.slidingWindowType==sliding_window_structures.EC) {
				if (c.numberOfStreams<128)
					mem=2;
				else 
					mem=3;
			}
			if (c.slidingWindowType==sliding_window_structures.DW) {
				if (c.numberOfStreams<64)
					mem=2;
				else if (c.numberOfStreams<256)
					mem=3;
				else 
					mem=4;
			}
			if (c.slidingWindowType==sliding_window_structures.RW) {
				if (c.numberOfStreams<32)
					mem=4;
				else if (c.numberOfStreams<=64)
					mem=8;
				else 
					mem=10;
				if (c.delta<0.16)
					mem+=2;
				if (c.delta<0.11)
					mem+=2;
				if (c.delta<0.06)
					mem+=2;
			}
			String dd = c.path.replaceAll(".binary.gz", ".txt");
			int repeats=1;
			String cmd = ("\njava -cp ContinuousEH.jar -Xmx" + mem + "g experiments.distributedMonitoringSendCM ");
			cmd = cmd + c.numberOfStreams + " "; // 0 
			cmd = cmd + c.path + " "; // 1
			cmd = cmd + c.delta + " "; // 2
			cmd = cmd + 0.15 + " "; // 3
			cmd = cmd + repeats + " "; // 4
			cmd = cmd + c.epsilon + " "; // 5
			cmd = cmd + 0 + " "; // 6 unused - for point queries only
			if (c.path.contains("ips"))
				cmd = cmd + "200000000 "; // 2000 seconds - unit is 0.001 milliseconds
			else
				cmd = cmd + "2000000 "; // 2 million seconds

			cmd = cmd + (" &> N" + c.numberOfStreams + ".E" + (int) (0.15 * 100) + ".A" + (int) (c.epsilon * 100) + "." + dd);
		
		}
	}
	
	static String printLine(String[]line) {
		String ss="";
		for (String s:line)
			ss+=s+"\t";
		return ss;
	}
}

class config {
	boolean optimizeForSJ;
	String data;
	String struct;
	float epsilon;
	float delta;
	int numberOfStreams;
	int siblings;
	int height;
	String[] fields;
	public config(boolean optimizeForSJ, String data, String struct, float epsilon, float delta, int numberOfStreams, int siblings, int height,String[] fields) {
		this.optimizeForSJ=optimizeForSJ;
		this.data=data;
		this.struct=struct;
		this.epsilon=epsilon;
		this.delta=delta;
		this.numberOfStreams=numberOfStreams;
		this.siblings=siblings;
		this.height=height;
		this.fields=fields;
	}
	public boolean compareEqual(boolean optimizeForSJ, String data, String struct, float epsilon, int numberOfStreams) {
		return (this.optimizeForSJ==optimizeForSJ && 
				this.data.equals(data) &&
				this.struct.equals(struct) &&
				this.epsilon==epsilon &&
				this.numberOfStreams==numberOfStreams);
	}
}
class configMonitoring {
	String data;
	String struct;
	float epsilon;
	float delta;
	int numberOfStreams;
	int siblings;
	int height;
	float acc;
	String[] fields;
	public configMonitoring(String data, String struct, float epsilon, float delta, int numberOfStreams, int siblings, int height, float acc, String[] fields) {
		this.data=data;
		this.struct=struct;
		this.epsilon=epsilon;
		this.delta=delta;
		this.numberOfStreams=numberOfStreams;
		this.siblings=siblings;
		this.height=height;
		this.acc=acc;
		this.fields=fields;
	}
	public boolean compareEqual(String data, String struct, float epsilon, int numberOfStreams, float acc) {
		return ( 
				this.data.equals(data) &&
				this.struct.equals(struct) &&
				this.epsilon==epsilon &&
				this.numberOfStreams==numberOfStreams &&
				this.acc == acc);
	}
}
