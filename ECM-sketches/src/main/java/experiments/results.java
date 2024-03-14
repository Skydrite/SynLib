package experiments;

class results {
	// configuration
	String path;
	String slidingWindowType;
	double epsilon;
	double delta;
	// structure
	int numberOfStreams;
	int siblings;
	int height;
	int totalEvents;
	// results
	double avgRelativeError;
	double maxRelativeError;
	double ratioOfSeriousErrors;

	double avgInnerProductRelativeError;
	double maxInnerProductRelativeError;
	// cost
	double avgMemory;
	double avgNetwork;
	// detailed
	double avgAbsError;
	double maxAbsError;
	double avgRealRelError;
	double avgInnerProductRealRelError;
	double maxRealRelError;
	double maxInnerProductRealRelError;
	boolean optimizeForIP;

	public results(boolean optimizeForIP, String path, String slidingWindowType, double epsilon, double delta, int numberOfStreams, int siblings, int height, int totalEvents, double avgRelativeError, double maxRelativeError,
			double ratioOfSeriousErrors, double avgInnerProductRelativeError, double maxInnerProductRelativeError, double avgMemory, double avgNetwork, double avgAbsError, double maxAbsError,
			double avgRealRelError, double avgInnerProductRealRelError, double maxRealRelError, double maxInnerProductRealRelError) {
		super();
		this.path=path;
		this.slidingWindowType = slidingWindowType;
		this.epsilon = epsilon;
		this.delta = delta;
		this.numberOfStreams = numberOfStreams;
		this.siblings = siblings;
		this.height = height;
		this.totalEvents = totalEvents;
		this.avgRelativeError = avgRelativeError;
		this.maxRelativeError = maxRelativeError;
		this.ratioOfSeriousErrors = ratioOfSeriousErrors;
		this.avgInnerProductRelativeError = avgInnerProductRelativeError;
		this.maxInnerProductRelativeError = maxInnerProductRelativeError;
		this.avgMemory = avgMemory;
		this.avgNetwork = avgNetwork;
		this.avgAbsError = avgAbsError;
		this.maxAbsError = maxAbsError;
		this.avgRealRelError = avgRealRelError;
		this.avgInnerProductRealRelError = avgInnerProductRealRelError;
		this.maxRealRelError = maxRealRelError;
		this.maxInnerProductRealRelError = maxInnerProductRealRelError;
		this.optimizeForIP = optimizeForIP;
	}
	public String printConfiguration(boolean labels) {
		if (labels) 
			return "Configuration:optimizeForIP," +optimizeForIP  + ",path," + path +",slidingWindowType,"+slidingWindowType + ",epsilon," + epsilon + ",delta," + delta + ",numberOfStreams," + numberOfStreams+ ",siblings," + siblings+ ",height," + height+ ",totalEvents," + totalEvents;
		else
			return  optimizeForIP + "," + path + "," + slidingWindowType + "," + epsilon + "," + delta + "," + numberOfStreams+ "," + siblings+ "," + height+ "," + totalEvents;
	}
	public String printResults(boolean labels) {
		if (labels)
			return "Results:avgRelativeError," + avgRelativeError + ",maxRelativeError," + maxRelativeError + ",ratioOfSeriousErrors," + ratioOfSeriousErrors + ",avgInnerProductRelativeError," + 
				avgInnerProductRelativeError + ",maxInnerProductRelativeError," + maxInnerProductRelativeError + ",avgMemory," + avgMemory + ",avgNetwork," + avgNetwork;
		else
			return avgRelativeError + "," + maxRelativeError + "," + ratioOfSeriousErrors + "," + avgInnerProductRelativeError + "," + maxInnerProductRelativeError + "," + avgMemory + "," + avgNetwork;
	}
	public String printDetailedResults(boolean labels) {
		if (labels)
			return "DetailedResults:avgAbsError," + avgAbsError + ",maxAbsError," + maxAbsError + ",avgRealRelError," + avgRealRelError + 
					",avgInnerProductRealRelError," + avgInnerProductRealRelError + ",maxRealRelError," + maxRealRelError + ",maxInnerProductRealRelError," + maxInnerProductRealRelError;
		else
			return avgAbsError + "," + maxAbsError + "," + avgRealRelError + "," + avgInnerProductRealRelError + "," + maxRealRelError + "," + maxInnerProductRealRelError;
	}
	

	public String getSlidingWindowType() {
		return slidingWindowType;
	}

	public void setSlidingWindowType(String slidingWindowType) {
		this.slidingWindowType = slidingWindowType;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}

	public int getNumberOfStreams() {
		return numberOfStreams;
	}

	public void setNumberOfStreams(int numberOfStreams) {
		this.numberOfStreams = numberOfStreams;
	}

	public int getSiblings() {
		return siblings;
	}

	public void setSiblings(int siblings) {
		this.siblings = siblings;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getTotalEvents() {
		return totalEvents;
	}

	public void setTotalEvents(int totalEvents) {
		this.totalEvents = totalEvents;
	}

	public double getAvgRelativeError() {
		return avgRelativeError;
	}

	public void setAvgRelativeError(double avgRelativeError) {
		this.avgRelativeError = avgRelativeError;
	}

	public double getMaxRelativeError() {
		return maxRelativeError;
	}

	public void setMaxRelativeError(double maxRelativeError) {
		this.maxRelativeError = maxRelativeError;
	}

	public double getRatioOfSeriousErrors() {
		return ratioOfSeriousErrors;
	}

	public void setRatioOfSeriousErrors(double ratioOfSeriousErrors) {
		this.ratioOfSeriousErrors = ratioOfSeriousErrors;
	}

	public double getAvgInnerProductRelativeError() {
		return avgInnerProductRelativeError;
	}

	public void setAvgInnerProductRelativeError(double avgInnerProductRelativeError) {
		this.avgInnerProductRelativeError = avgInnerProductRelativeError;
	}

	public double getMaxInnerProductRelativeError() {
		return maxInnerProductRelativeError;
	}

	public void setMaxInnerProductRelativeError(double maxInnerProductRelativeError) {
		this.maxInnerProductRelativeError = maxInnerProductRelativeError;
	}

	public double getAvgMemory() {
		return avgMemory;
	}

	public void setAvgMemory(double avgMemory) {
		this.avgMemory = avgMemory;
	}

	public double getAvgNetwork() {
		return avgNetwork;
	}

	public void setAvgNetwork(double avgNetwork) {
		this.avgNetwork = avgNetwork;
	}

	public double getAvgAbsError() {
		return avgAbsError;
	}

	public void setAvgAbsError(double avgAbsError) {
		this.avgAbsError = avgAbsError;
	}

	public double getMaxAbsError() {
		return maxAbsError;
	}

	public void setMaxAbsError(double maxAbsError) {
		this.maxAbsError = maxAbsError;
	}

	public double getAvgRealRelError() {
		return avgRealRelError;
	}

	public void setAvgRealRelError(double avgRealRelError) {
		this.avgRealRelError = avgRealRelError;
	}

	public double getAvgInnerProductRealRelError() {
		return avgInnerProductRealRelError;
	}

	public void setAvgInnerProductRealRelError(double avgInnerProductRealRelError) {
		this.avgInnerProductRealRelError = avgInnerProductRealRelError;
	}

	public double getMaxRealRelError() {
		return maxRealRelError;
	}

	public void setMaxRealRelError(double maxRealRelError) {
		this.maxRealRelError = maxRealRelError;
	}

	public double getMaxInnerProductRealRelError() {
		return maxInnerProductRealRelError;
	}

	public void setMaxInnerProductRealRelError(double maxInnerProductRealRelError) {
		this.maxInnerProductRealRelError = maxInnerProductRealRelError;
	}
}
