package experiments;

public class resultsContinuous {
	// configuration
	String path;
	double accuracy;
	
	public resultsContinuous(String path, String slidingWindowType, double epsilon, double delta, int numberOfStreams, int siblings, int height, long totalEvents, double... results) {
		super();
		this.path = path;
		this.slidingWindowType = slidingWindowType;
		this.epsilon = epsilon;
		this.delta = delta;
		this.numberOfStreams = numberOfStreams;
		this.siblings = siblings;
		this.height = height;
		this.totalEvents = totalEvents;
		this.results = results;
	}
	String slidingWindowType;
	double epsilon;
	double delta;
	// structure
	int numberOfStreams;
	int siblings;
	int height;
	long totalEvents;
	// results
	double[] results;
	
	public String printConfiguration(boolean labels) {
		if (labels) 
			return "Configuration:path," + path +",slidingWindowType,"+slidingWindowType + ",epsilon," + epsilon + ",delta," + delta + ",numberOfStreams," + numberOfStreams+ ",siblings," + siblings+ ",height," + height+ ",totalEvents," + totalEvents;
		else
			return path + "," + slidingWindowType + "," + epsilon + "," + delta + "," + numberOfStreams+ "," + siblings+ "," + height+ "," + totalEvents;
	}
	public String printResults(boolean labels) {
		if (labels) {
			int pos = 0;
			String s = "Results:naiveMsgs," + results[pos++] + ",naiveTV," + results[pos++] + ",intelligentMsgs," + results[pos++] + ",intelligentTV," + results[pos++] + ",avgRelativeError," + results[pos++] + ",avgRelativeErrorDivisor," + results[pos++];
			for (int queryId=0;queryId<(results.length-pos)/6;queryId ++) {
				s = s + ",[Q " + queryId + ":thresholdFactor," + results[pos++] + ",accuracy," + results[pos++] + ",queryLen," + results[pos++] + ",relativeError," + results[pos++] + ",precision," + results[pos++] + ",recall," + results[pos++] + ",]";
			}
			return s;
		} 
		else {
			int pos = 0;
			String s = "" + results[pos++] + "," + results[pos++] + "," + results[pos++] + "," + results[pos++] + "," + results[pos++] + "," + results[pos++];
			for (int queryId=0;queryId<(results.length-pos)/6;queryId ++) {
				s = s + ",[Q " + queryId + "," + results[pos++] + "," + results[pos++] + "," + results[pos++] + "," + results[pos++] + "," + results[pos++] + "," + results[pos++] + ",]";
			}
			return s;
		}
	}
}
class resultsContinuousSJ {
	// configuration
	String path;
	double accuracy;
	String slidingWindowType;
	int slidingWindowLength;
	double epsilon;
	double delta;
	// structure
	int numberOfStreams;
	int siblings;
	int height;
	long totalEvents;
	// results
	double[] results;

	public resultsContinuousSJ(String path, String slidingWindowType, int slidingWindowLength, double epsilon, double delta, int numberOfStreams, int siblings, int height, long totalEvents, double... results) {
		super();
		this.path = path;
		this.slidingWindowType = slidingWindowType;
		this.epsilon = epsilon;
		this.delta = delta;
		this.numberOfStreams = numberOfStreams;
		this.siblings = siblings;
		this.height = height;
		this.totalEvents = totalEvents;
		this.results = results;
		this.slidingWindowLength = slidingWindowLength;
	}
	
	public String printConfiguration(boolean labels) {
		if (labels) 
			return "Configuration:path," + path +",slidingWindowType,"+slidingWindowType + ",swLen," + slidingWindowLength + ",epsilon," + epsilon + ",delta," + delta + ",numberOfStreams," + numberOfStreams+ ",siblings," + siblings+ ",height," + height+ ",maxEventsInSW," + totalEvents;
		else
			return path + "," + slidingWindowType + "," + slidingWindowLength + "," + epsilon + "," + delta + "," + numberOfStreams+ "," + siblings+ "," + height+ "," + totalEvents;
	}
	public String printConfigurationLabels(){
		return "Configuration:path,slidingWindowType,swLen,epsilon,delta,numberOfStreams,siblings,height,maxEventsInSW";
	}
	public String printResultsLabels() {
		String s = "Results:acc,qLen,naiveMsgs,naiveTV,intelligentMsgs,intelligentTV,avgRelativeError,maxRelativeError,avgRelativeErrorAcc,maxRelativeErrorAcc,absoluteErrorAcc,intMsgsUp,intTVUp";
		return s;
	}

	
	public String printResults(boolean labels) {
		if (labels) {
			int pos = 0;
			String s = "Results:acc,"+ results[pos++] + ",qLen,"+ results[pos++] + 
					",naiveMsgs," + results[pos++] + ",naiveTV," + results[pos++] + 
					",intelligentMsgs," + results[pos++] + ",intelligentTV," + results[pos++] + 
					",avgRelativeError," + results[pos++] + 
					",maxRelativeError," + results[pos++] + 
					",avgRelativeErrorAcc," + results[pos++] + 
					",maxRelativeErrorAcc," + results[pos++] + 
					",absoluteErrorAcc,"+ results[pos++] +
					",intelligentMsgsUp," + results[pos++] + ",intelligentTVUp," + results[pos++]; 

			return s;
		} 
		else {
			int pos = 0;
			String s =  results[pos++] + ","+ results[pos++] + 
					"," + results[pos++] + "," + results[pos++] + 
					"," + results[pos++] + "," + results[pos++] + 
					"," + results[pos++] + "," + results[pos++] + 
					"," + results[pos++] + "," + results[pos++] +
					"," + results[pos++] + 
					"," + results[pos++] + "," + results[pos++]; // intMsgsUp+intTvUp
			return s;
		}
	}
}
