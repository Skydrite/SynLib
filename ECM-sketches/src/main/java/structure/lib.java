package structure;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class lib {
	
	/*
	 * Helper functions
	 * 
	 */
	public static final double VerySmallNumber = 0.0000001d;
	public static boolean compareSignificantDifference(double[][] v1, double[][]v2) {
		for (int i=0;i<v1.length;i++) {
			for (int j=0;j<v1[0].length;j++) {
				if (Math.abs(v1[i][j]-v2[i][j])>VerySmallNumber)
					return true;
			}
		}
		return false;
	}
	public static String printSet(HashSet<Integer> items) {
		StringBuilder sb = new StringBuilder();
		for (int i : items ) sb.append(" " + i);
		return sb.toString();
	}
	public static double minArray(double[] d) {
		double minReturn = Double.MAX_VALUE;
		for (double dd:d) minReturn=Math.min(minReturn,dd);
		return minReturn;
	}
	public static double[] sqrt(double[] d) {
		double[] r = new double[d.length];
		for (int i=0;i<d.length;i++)
			r[i] = Math.sqrt(d[i]);
		return r;
	}
	public static double[][] sqrt(double[][] d) {
		double[][] r = new double[d.length][];
		for (int i=0;i<d.length;i++)
			r[i] = lib.sqrt(d[i]);
		return r;
	}
	
	public static double[] computeInnerProductRange(slidingCMSketch mergedSketch, slidingCMSketch localSketch, slidingCMSketch lastSyncedLocal, int numberOfNodes, int queryRange, 
			int startTimeWithCurrentTime, boolean average) {
		int w = mergedSketch.w;
		int d = mergedSketch.d;
		slidingCMSketch sketch = localSketch;
		slidingCMSketch lastSyncedSketch = lastSyncedLocal; 
		// now compute drift
		double[]centerMagnitude = new double[d];
		double[]radiusMagnitude = new double[d];
		double[] drift = new double[w];
		double[] currentEstimation = new double[w];
		double[] center = new double[w];
		double[] radius = new double[w];
		for (int dcnt = 0; dcnt < d; dcnt++) {
			for (int wcnt = 0; wcnt < w; wcnt++) {
				currentEstimation[wcnt] = mergedSketch.array[wcnt][dcnt].getEstimationRange(queryRange)/numberOfNodes;
				drift[wcnt] = currentEstimation[wcnt] + (sketch.array[wcnt][dcnt].getEstimationRealtime(startTimeWithCurrentTime) - 
																	lastSyncedSketch.array[wcnt][dcnt].getEstimationRange(queryRange));
				center[wcnt] = (currentEstimation[wcnt]+drift[wcnt])/2d;
				radius[wcnt] = (currentEstimation[wcnt]-drift[wcnt])/2d;
			}

			if (average) {
				{
					centerMagnitude[dcnt] = lib.getMagnitude(center);
					radiusMagnitude[dcnt] = lib.getMagnitude(radius);
				}
			} else {
				{
					centerMagnitude[dcnt] = lib.getMagnitudeMultipliedByNodes(center, numberOfNodes);
					radiusMagnitude[dcnt] = lib.getMagnitudeMultipliedByNodes(radius, numberOfNodes);
				}
			}
		}
		
		// now compute local inner product
		double minMaxInnerProductPerRow = Double.MAX_VALUE;
		double minMinInnerProductPerRow = Double.MAX_VALUE;
		for (int dcnt = 0; dcnt < d; dcnt++) {
			minMaxInnerProductPerRow = Math.min(minMaxInnerProductPerRow, slidingCMSketch.square(centerMagnitude[dcnt]+radiusMagnitude[dcnt]));
			minMinInnerProductPerRow = Math.min(minMinInnerProductPerRow, slidingCMSketch.square(centerMagnitude[dcnt]-radiusMagnitude[dcnt]));
		}
		if (minMinInnerProductPerRow<0) minMinInnerProductPerRow=0;
		return new double[]{minMinInnerProductPerRow, minMaxInnerProductPerRow};
	}


	
	public static double[] computeEventFrequencyRange(int[]hashes, double[][] mergedSketch, slidingCMSketch localSketch, double[][] lastSyncedLocal, 
			int numberOfNodes, int queryRange, int startTimeWithCurrentTime, boolean average, int queryId) {
		int w = localSketch.w;
		int d = localSketch.d;
		double[][] sketchEstimations = localSketch.getMaintainedEstimations(queryId);
		// now compute drift
		double[] drift = new double[d];
		double[] currentEstimation = new double[d];
		double[] center = new double[d];
		double[] radius = new double[d];
		for (int dcnt = 0; dcnt < d; dcnt++) {
			int wcnt=hashes[dcnt];
			currentEstimation[dcnt] = mergedSketch[wcnt][dcnt]/numberOfNodes; // estimate vector
			drift[dcnt] = currentEstimation[dcnt] + (sketchEstimations[wcnt][dcnt] - lastSyncedLocal[wcnt][dcnt]);
			center[dcnt] = (currentEstimation[dcnt]+drift[dcnt])/2d;
			radius[dcnt] = Math.abs(currentEstimation[dcnt]-drift[dcnt])/2d;
		}
		
		// now compute local inner product
		double minMaxValue = Double.MAX_VALUE;
		double minMinValue = Double.MAX_VALUE;
		for (int dcnt = 0; dcnt < d; dcnt++) {
			minMaxValue = Math.min(minMaxValue, (center[dcnt]+radius[dcnt]));
			minMinValue = Math.min(minMinValue, (center[dcnt]-radius[dcnt]));
		}
		if (minMinValue<0) minMinValue=0;
		return new double[]{minMinValue, minMaxValue};
	}
	
	public static double computeL2(double[] r) {
		double l2=0;
		for (double d:r)
			l2+=lib.square(d);
		return Math.sqrt(l2);
	}
	
	public static TupleGeneric<double[], Double>  computeBallCenterAndRadius(double[] syncedSketch, double[] lastLocalStatistics, double[] newLocalStatistics) {
		int d = syncedSketch.length;
		double[] center = new double[d];
		double[] deltaSV = new double[d];
		double[] driftVector = new double[d];
		double radius = 0;
//		double radius2 =0;
		for (int i=0;i<d;i++) {
			deltaSV[i] = newLocalStatistics[i] - lastLocalStatistics[i];
			driftVector[i] = syncedSketch[i] + deltaSV[i];
			center[i] = (syncedSketch[i] + driftVector[i])/2d;
			radius+=lib.square(deltaSV[i]);
//			radius2+=lib.square((syncedSketch[i] - driftVector[i])/2d);
		}
		radius=Math.sqrt(radius)/2d;
//		radius2 = Math.sqrt(radius2);
//		System.err.println("Radius " + radius  + "equals to Radius2 " + radius2);
		return new TupleGeneric(center,radius);
	}
	
	public static double computeMinDistanceFromInadmissibleRegion(boolean isFrequent, double[]center, double threshold) {
		double minDistance=0;
		int d=center.length;
		if (isFrequent) { // since it is frequent already, I need to compute the radius for the single closest dimension
			minDistance=Double.MAX_VALUE;
			for (int i=0;i<d;i++) {
				if (center[i]<=threshold)
					minDistance=0;
				else 
					minDistance=Math.min(minDistance, center[i]-threshold);
			}
			
		} else { // all dimensions must surpass the threshold
			for (int i=0;i<d;i++) {
				if (center[i]<threshold)
					minDistance+=lib.square(threshold-center[i]);
			}
			minDistance=Math.sqrt(minDistance);
		}
		return minDistance;
	}
	public static double computeMinDistanceFromInadmissibleRegion(boolean isFrequent, double[]center, double[] threshold) {
		double minDistance=0;
		int d=center.length;
		if (isFrequent) { // since it is frequent already, I need to compute the radius for the single closest dimension
			System.err.println("Why is this even used?");
			minDistance=Double.MAX_VALUE;
			for (int i=0;i<d;i++) {
				if (center[i]<=threshold[i])
					minDistance=0;
				else 
					minDistance=Math.min(minDistance, center[i]-threshold[i]);
			}
			
		} else { // all dimensions must surpass the threshold
			for (int i=0;i<d;i++) {
				if (center[i]<threshold[i])
					minDistance+=lib.square(threshold[i]-center[i]);
			}
			minDistance=Math.sqrt(minDistance);
		}
		return minDistance;
	}
	
	
	/*/
	public static double[] computeBoundingBallAcceptableDistance(double[] syncedSketch, double[] DeltaV, double threshold) {
		int d = syncedSketch.length;
		double sumDistance=0;
		double minDistance=0;
		// now compute distance to threshold
		for (int singleRow=0;singleRow<d;singleRow++) {
			double c = syncedSketch[singleRow] + DeltaV[singleRow]/2d;
			if (c < threshold) {
				sumDistance+=lib.square(threshold-c);
			} else {
				minDistance = Math.min(minDistance, c-threshold);
			}
		}
		return new double[]{Math.sqrt(sumDistance),minDistance};
	}
	
	public static double computeBoundingBallRange(double[]syncedSketch, double[] DeltaV, )

	
	// computes the bounding ball maximum acceptable radius
	// ret[0]: if v < \tau, then sqrt(sum_d (\tau - v[i])^2) ONLY when v[i] < \tau
	// ret[1]: if v > \tau, then sqrt(min_d (v[i] - \tau)^2) = min_d (v[i] - \tau) 
	public static double[] computeBoundingBallMaxAcceptableRadius(int[] hashes, double[][] mergedSketch, double threshold) {
		int d = mergedSketch[0].length;
		double sumDistance=0;
		double minDistance=0;
		// now compute distance to threshold
		for (int singleRow=0;singleRow<d;singleRow++) {
			int wcnt=hashes[singleRow];
			if (mergedSketch[wcnt][singleRow] < threshold) {
				sumDistance+=lib.square(threshold-mergedSketch[wcnt][singleRow]);
			} else {
				minDistance = Math.min(minDistance, mergedSketch[wcnt][singleRow]-threshold);
			}
		}
		return new double[]{Math.sqrt(sumDistance),minDistance};
	}
	// computes the bounding ball maximum acceptable radius
	// ret[0]: if v < \tau, then sqrt(sum_d (\tau - v[i])^2) ONLY when v[i] < \tau
	// ret[1]: if v > \tau, then sqrt(min_d (v[i] - \tau)^2) = min_d (v[i] - \tau) 
	public static double[] computeBoundingBallMaxAcceptableRadius(double[] mergedSketch, double threshold) {
		int d = mergedSketch.length;
		double sumDistance=0;
		double minDistance=0;
		// now compute distance to threshold
		for (int singleRow=0;singleRow<d;singleRow++) {
			if (mergedSketch[singleRow] < threshold) {
				sumDistance+=lib.square(threshold-mergedSketch[singleRow]);
			} else {
				minDistance = Math.min(minDistance, mergedSketch[singleRow]-threshold);
			}
		}
		return new double[]{Math.sqrt(sumDistance),minDistance};
	}
	
	public static double computeRadius(double[] center, double[] shift) {
		double r = 0;
		for (int i=0;i<center.length;i++) r+=lib.square(shift[i]-center[i]);
		return Math.sqrt(r);
	}

	public static boolean hasThresholdCrossing(double[] boundingBallRadius, double threshold, double[] center, double[] shift, double lastEstimation) {
		double radius = computeRadius(center, shift);
		if (lastEstimation<threshold) { // not frequent
			if (radius<boundingBallRadius[0]) 
				return false;
			else 
				return true;
		} else { // frequent
			if (radius<boundingBallRadius[1]) 
				return false;
			else 
				return true;
		}
		
	}

//	public static double computeInfrequentBoundingBallRadius(int singleRow, int[] hashes, double[][] mergedSketch, slidingCMSketch localSketch, double[][] lastSyncedLocal, 
//			int numberOfNodes, int queryRange, int startTimeWithCurrentTime, boolean average, int queryId) {
//		int d = localSketch.d;
//		double[][] sketchEstimations = localSketch.getMaintainedEstimations(queryId);
//		// now compute drift
//		double[] DeltaV = new double[d];
//		double[] currentEstimation = new double[d];
//		int wcnt=hashes[singleRow];
//		currentEstimation[singleRow] = mergedSketch[wcnt][singleRow]; // estimate vector
//		DeltaV[singleRow] = (sketchEstimations[wcnt][singleRow] - lastSyncedLocal[wcnt][singleRow]);
//		double radius = DeltaV[singleRow];
//		return radius;
//	}
//
//	
//	public static double computeInfrequentBoundingBallRadius(int singleRow, int[] hashes, double[][] mergedSketch, slidingCMSketch localSketch, double[][] lastSyncedLocal, 
//			int numberOfNodes, int queryRange, int startTimeWithCurrentTime, boolean average, int queryId) {
//		int d = localSketch.d;
//		double[][] sketchEstimations = localSketch.getMaintainedEstimations(queryId);
//		// now compute drift
//		double[] DeltaV = new double[d];
//		double[] currentEstimation = new double[d];
//		int wcnt=hashes[singleRow];
//		currentEstimation[singleRow] = mergedSketch[wcnt][singleRow]; // estimate vector
//		DeltaV[singleRow] = (sketchEstimations[wcnt][singleRow] - lastSyncedLocal[wcnt][singleRow]);
//		double radius = DeltaV[singleRow];
//		return radius;
//	}
//	
//	public static double[] computeFrequentBoundingBallRadius(int[] hashes, double[][] mergedSketch, slidingCMSketch localSketch, double[][] lastSyncedLocal, 
//			int numberOfNodes, int queryRange, int startTimeWithCurrentTime, boolean average, int queryId) {
//		int d = localSketch.d;
//		double[][] sketchEstimations = localSketch.getMaintainedEstimations(queryId);
//		// now compute drift
//		double[] DeltaV = new double[d];
//		double[] currentEstimation = new double[d];
//		int smallestDiffRow=-1;
//		double minDiff=Double.MAX_VALUE;
//		for (int singleRow=0;singleRow<d;singleRow++) {
//			int wcnt=hashes[singleRow];
//			currentEstimation[singleRow] = mergedSketch[wcnt][singleRow]; // estimate vector
//			DeltaV[singleRow] = (sketchEstimations[wcnt][singleRow] - lastSyncedLocal[wcnt][singleRow]);
//			if (currentEstimation[singleRow]<minDiff) {minDiff=currentEstimation[singleRow];smallestDiffRow=singleRow;}
//		}
//		// now compute local inner product
//		double minMaxValue = Double.MAX_VALUE;
//		double minMinValue = Double.MAX_VALUE;
//
////		for (int dcnt = 0; dcnt < d; dcnt++) 
//		int dcnt=bestRow; // this will be common across all nodes, since the estimate vector is identical across the nodes
//		{
//			double minDcnt = Math.min(currentEstimation[dcnt], currentEstimation[dcnt]+DeltaV[dcnt]*numberOfNodes);
//			double maxDcnt = Math.max(currentEstimation[dcnt], currentEstimation[dcnt]+DeltaV[dcnt]*numberOfNodes);
//			minMaxValue = Math.min(minMaxValue, maxDcnt);
//			minMinValue = Math.min(minMinValue, minDcnt);
//		}
//		if (minMinValue<0) 
//			minMinValue=0;
//		return new double[]{minMinValue, minMaxValue};
//	}
//

	public static double[] computeEventFrequencyRangeLinearSingleRow(int singleRow, int[] hashes, double[][] mergedSketch, slidingCMSketch localSketch, double[][] lastSyncedLocal, 
			int numberOfNodes, int queryRange, int startTimeWithCurrentTime, boolean average, int queryId) {
		int d = localSketch.d;
		double[][] sketchEstimations = localSketch.getMaintainedEstimations(queryId);
		// now compute drift
		double[] DeltaV = new double[d];
		double[] currentEstimation = new double[d];
		int bestRow=-1;
		double minScore=Double.MAX_VALUE;
		int wcnt=hashes[singleRow];
		currentEstimation[singleRow] = mergedSketch[wcnt][singleRow]; // estimate vector
		DeltaV[singleRow] = (sketchEstimations[wcnt][singleRow] - lastSyncedLocal[wcnt][singleRow]);
		if (currentEstimation[singleRow]<minScore) {minScore=currentEstimation[singleRow];bestRow=singleRow;}
		
		// now compute local inner product
		double minMaxValue = Double.MAX_VALUE;
		double minMinValue = Double.MAX_VALUE;

//		for (int dcnt = 0; dcnt < d; dcnt++) 
		int dcnt=bestRow; // this will be common across all nodes, since the estimate vector is identical across the nodes
		{
			double minDcnt = Math.min(currentEstimation[dcnt], currentEstimation[dcnt]+DeltaV[dcnt]*numberOfNodes);
			double maxDcnt = Math.max(currentEstimation[dcnt], currentEstimation[dcnt]+DeltaV[dcnt]*numberOfNodes);
			minMaxValue = Math.min(minMaxValue, maxDcnt);
			minMinValue = Math.min(minMinValue, minDcnt);
		}
		if (minMinValue<0) 
			minMinValue=0;
		return new double[]{minMinValue, minMaxValue};
	}


	public static double[] computeEventFrequencyRangeLinearAllRows(int[]hashes, double[][] mergedSketch, slidingCMSketch localSketch, double[][] lastSyncedLocal, 
			int numberOfNodes, int queryRange, int startTimeWithCurrentTime, boolean average, int queryId) {
		int d = localSketch.d;
		double[][] sketchEstimations = localSketch.getMaintainedEstimations(queryId);
		// now compute drift
		double[] DeltaV = new double[d];
		double[] currentEstimation = new double[d];
		int bestRow=-1;
		double minScore=Double.MAX_VALUE;
		for (int dcnt = 0; dcnt < d; dcnt++) {
			int wcnt=hashes[dcnt];
			currentEstimation[dcnt] = mergedSketch[wcnt][dcnt]; // estimate vector
			DeltaV[dcnt] = (sketchEstimations[wcnt][dcnt] - lastSyncedLocal[wcnt][dcnt]);
			if (currentEstimation[dcnt]<minScore) {minScore=currentEstimation[dcnt];bestRow=dcnt;}
		}
		
		// now compute local inner product
		double minMaxValue = Double.MAX_VALUE;
		double minMinValue = Double.MAX_VALUE;

//		for (int dcnt = 0; dcnt < d; dcnt++) 
		int dcnt=bestRow; // this will be common across all nodes, since the estimate vector is identical across the nodes
		{
			double minDcnt = Math.min(currentEstimation[dcnt], currentEstimation[dcnt]+DeltaV[dcnt]*numberOfNodes);
			double maxDcnt = Math.max(currentEstimation[dcnt], currentEstimation[dcnt]+DeltaV[dcnt]*numberOfNodes);
			minMaxValue = Math.min(minMaxValue, maxDcnt);
			minMinValue = Math.min(minMinValue, minDcnt);
		}
		if (minMinValue<0) 
			minMinValue=0;
		return new double[]{minMinValue, minMaxValue};
	}

	public static double[] computeInnerProductRange(double[][] mergedSketch, slidingCMSketch localSketch, double[][] lastSyncedLocal, int numberOfNodes, int queryRange, 
			int startTimeWithCurrentTime, boolean average, int queryId) {
		int w = localSketch.w;
		int d = localSketch.d;
//		slidingCMSketch sketch = localSketch;
		double[][] sketchEstimations = localSketch.getMaintainedEstimations(queryId);
		// now compute drift
		double[]centerMagnitude = new double[d];
		double[]radiusMagnitude = new double[d];
		double[] drift = new double[w];
		double[] currentEstimation = new double[w];
		double[] center = new double[w];
		double[] radius = new double[w];
		for (int dcnt = 0; dcnt < d; dcnt++) {
			for (int wcnt = 0; wcnt < w; wcnt++) {
				currentEstimation[wcnt] = mergedSketch[wcnt][dcnt]; // estimate vector
				drift[wcnt] = currentEstimation[wcnt] + (sketchEstimations[wcnt][dcnt] - lastSyncedLocal[wcnt][dcnt])*numberOfNodes;
				center[wcnt] = (currentEstimation[wcnt]+drift[wcnt])/2d;
				radius[wcnt] = (currentEstimation[wcnt]-drift[wcnt])/2d;
			}
			centerMagnitude[dcnt] = lib.getMagnitude(center);
			radiusMagnitude[dcnt] = lib.getMagnitude(radius);
		}
		
		// now compute local inner product
		double minMaxInnerProductPerRow = Double.MAX_VALUE;
		double minMinInnerProductPerRow = Double.MAX_VALUE;
		for (int dcnt = 0; dcnt < d; dcnt++) {
			minMaxInnerProductPerRow = Math.min(minMaxInnerProductPerRow, slidingCMSketch.square(centerMagnitude[dcnt]+radiusMagnitude[dcnt]));
			minMinInnerProductPerRow = Math.min(minMinInnerProductPerRow, slidingCMSketch.square(centerMagnitude[dcnt]-radiusMagnitude[dcnt]));
		}
		if (minMinInnerProductPerRow<0) minMinInnerProductPerRow=0;
		return new double[]{minMinInnerProductPerRow, minMaxInnerProductPerRow};
	}
	
	public static double[] computeInnerProductRangeCorrectButMemHungry(slidingCMSketch mergedSketch, slidingCMSketch localSketch, slidingCMSketch lastSyncedLocal, int numberOfNodes, int queryRange, 
			int startTimeWithCurrentTime, boolean average) {
		int w = mergedSketch.w;
		int d = mergedSketch.d;
		slidingCMSketch sketch = localSketch;
		slidingCMSketch lastSyncedSketch = lastSyncedLocal; 
		// now compute drift
		double[][] drift = new double[w][d];
		double[][] currentEstimation = new double[w][d];
		double[][] center = new double[w][d];
		double[][] radius = new double[w][d];
		for (int wcnt = 0; wcnt < w; wcnt++) {
			for (int dcnt = 0; dcnt < d; dcnt++) {
				currentEstimation[wcnt][dcnt] = mergedSketch.array[wcnt][dcnt].getEstimationRange(queryRange)/numberOfNodes;
				drift[wcnt][dcnt] = currentEstimation[wcnt][dcnt] + (sketch.array[wcnt][dcnt].getEstimationRealtime(startTimeWithCurrentTime) - 
																	lastSyncedSketch.array[wcnt][dcnt].getEstimationRange(queryRange));
				center[wcnt][dcnt] = (currentEstimation[wcnt][dcnt]+drift[wcnt][dcnt])/2d;
				radius[wcnt][dcnt] = (currentEstimation[wcnt][dcnt]-drift[wcnt][dcnt])/2d;
			}
		}
		double[]centerMagnitude = new double[d];
		double[]radiusMagnitude = new double[d];
		if (average) {
			for (int dcnt = 0; dcnt < d; dcnt++) {
				centerMagnitude[dcnt] = lib.getMagnitudeOfRow(center, dcnt);
				radiusMagnitude[dcnt] = lib.getMagnitudeOfRow(radius, dcnt);
			}
		} else {
			for (int dcnt = 0; dcnt < d; dcnt++) {
				centerMagnitude[dcnt] = lib.getMagnitudeOfRowMultipliedByNodes(center, dcnt,numberOfNodes);
				radiusMagnitude[dcnt] = lib.getMagnitudeOfRowMultipliedByNodes(radius, dcnt,numberOfNodes);
			}
		}
		
		// now compute local inner product
		double minMaxInnerProductPerRow = Double.MAX_VALUE;
		double minMinInnerProductPerRow = Double.MAX_VALUE;
		for (int dcnt = 0; dcnt < d; dcnt++) {
			minMaxInnerProductPerRow = Math.min(minMaxInnerProductPerRow, slidingCMSketch.square(centerMagnitude[dcnt]+radiusMagnitude[dcnt]));
			minMinInnerProductPerRow = Math.min(minMinInnerProductPerRow, slidingCMSketch.square(centerMagnitude[dcnt]-radiusMagnitude[dcnt]));
		}
		if (minMinInnerProductPerRow<0) minMinInnerProductPerRow=0;
		return new double[]{minMinInnerProductPerRow, minMaxInnerProductPerRow};
	}
	
			/*/

	public static double[] deepClone(double[] d) {
		return d.clone();
	}
	public static double[][] deepClone(double[][] d) {
		double[][]dd = new double[d.length][];
		for (int i=0;i<d.length;i++)
			dd[i] = d[i].clone();
		return dd;
	}
	public static double[][][] deepClone(double[][][] d) {
		double[][][]dd = new double[d.length][d[0].length][];
		for (int i=0;i<d.length;i++)
			for (int j=0;j<d[0].length;j++)
				dd[i][j] = d[i][j].clone();
		return dd;
	}
	
	public static boolean saveNetwork=false;
	public static double computeOptimalTV(double[][]initial,double[][]updated) {
		int w=initial.length;
		int d=initial[0].length;
		double intTVFull = w*d*32/8d;
		if (saveNetwork) {
			int diffs=0;
			for (int wcnt=0;wcnt<w;wcnt++)
				for (int dcnt=0;dcnt<d;dcnt++)
					if ((int)(updated[wcnt][dcnt]-initial[wcnt][dcnt])!=0) diffs++;
			double intTVDiff = diffs*(32+32)/8d; // 32 bits for the value, 32 bits for the address
			return Math.min(intTVDiff,intTVFull);
		}
		else 
			return intTVFull;
	}
	
	public static double computeInnerProduct(double[][] sketch) {
		int w = sketch.length;
		int d = sketch[0].length;
		double minInnerProduct = Double.MAX_VALUE;
		for (int row=0;row<d;row++) {
			double ip=0;
			for (int wcnt=0;wcnt<w;wcnt++) {
				ip+=sketch[wcnt][row]*sketch[wcnt][row];
			}
			minInnerProduct=Math.min(ip,minInnerProduct);
		}
		return minInnerProduct;
	}
	
//	public static double[] computeInnerProductAndMinRow(double[][] sketch) {
//		int w = sketch.length;
//		int d = sketch[0].length;
//		double minInnerProduct = Double.MAX_VALUE;
//		int minRow=0;
//		for (int row=0;row<d;row++) {
//			double ip=0;
//			for (int wcnt=0;wcnt<w;wcnt++) {
//				ip+=lib.square(sketch[wcnt][row]);
//			}
//			if (minInnerProduct>ip) {
//				minRow=row;
//				minInnerProduct=ip;
//			}
//		}
//		return new double[]{minInnerProduct, minRow};
//	}
	
	public static double[] computeInnerProductPerRow(double[][] sketch) {
		int w = sketch.length;
		int d = sketch[0].length;
		double[] ipPerRow = new double[d];		
		for (int row=0;row<d;row++) {
			for (int wcnt=0;wcnt<w;wcnt++) {
				ipPerRow[row]+=lib.square(sketch[wcnt][row]);
			}
		}
		return ipPerRow;
	}
	
	public static ArrayList<Integer> setStartAndShuffleRest(HashSet<Integer> start, int total) {
		ArrayList<Integer> t = new ArrayList<>(total);
		for (int i=0;i<total;i++){
			if (!start.contains(i))
				t.add(i);
		}
		Collections.shuffle(t);
		t.addAll(start);
		Collections.reverse(t);
		return t;
	}
	static ArrayList<Integer> lastShuffled=null;
	public static ArrayList<Integer> getShuffle(int total) {
		if (lastShuffled==null || lastShuffled.size()!=total) {
			lastShuffled = new ArrayList<>(total);
			for (int i=0;i<total;i++)lastShuffled.add(i);
		}
		Collections.shuffle(lastShuffled);
		return lastShuffled;
	}
	
	// 0 is for Q_toBecomeInfrequent Q_TBI
	// 1 is for Q_toBecomeFrequent   Q_TBF
	public static double[][] computePosNegInnerProductPerRow(double[][] sketch) { // sketch equals to CURRENT-LastSynced --> 
																					// if negative-->CURRENT is less than LastSynced, possible become non-frequent --> threshold crossing from below
		int w = sketch.length;
		int d = sketch[0].length;
		double[][] ipPerRow = new double[2][d];		
		for (int row=0;row<d;row++) {
			for (int wcnt=0;wcnt<w;wcnt++) {
				if (sketch[wcnt][row]<0) // current < last synced --> reduced --> examine for TBI
					ipPerRow[0][row]+=lib.square(sketch[wcnt][row]);
				else
					ipPerRow[1][row]+=lib.square(sketch[wcnt][row]);
			}
		}
		return ipPerRow;
	}
	
	public static double[] computeInnerProductAndMinRowOverall(double[] s) {
		int d = s.length;
		double[] ipAndRow = new double[2];
		ipAndRow[0] = Double.MAX_VALUE;
		for (int row=0;row<d;row++) {
			if (ipAndRow[0]>s[row]) {
				ipAndRow[0] = s[row];
				ipAndRow[1] = row;
			}
		}
		return ipAndRow;
	}
	

	/*/

	public static double[] computeInnerProductRangeDebug(slidingCMSketch mergedSketch, slidingCMSketch localSketch, slidingCMSketch lastSyncedLocal, int numberOfNodes, int queryRange, int startTimeWithCurrentTime) {
		int w = mergedSketch.w;
		int d = mergedSketch.d;
		slidingCMSketch sketch = localSketch;
		slidingCMSketch lastSyncedSketch = lastSyncedLocal; 
		// now compute drift
		double[][] drift = new double[w][d];
		double[][] currentEstimation = new double[w][d];
		double[][] center = new double[w][d];
		double[][] radius = new double[w][d];
		for (int wcnt = 0; wcnt < w; wcnt++) {
			for (int dcnt = 0; dcnt < d; dcnt++) {
				currentEstimation[wcnt][dcnt] = mergedSketch.array[wcnt][dcnt].getEstimationRange(queryRange)/numberOfNodes;
				drift[wcnt][dcnt] = currentEstimation[wcnt][dcnt] + (sketch.array[wcnt][dcnt].getEstimationRealtime(startTimeWithCurrentTime) - 
																	lastSyncedSketch.array[wcnt][dcnt].getEstimationRange(queryRange));
				center[wcnt][dcnt] = (currentEstimation[wcnt][dcnt]+drift[wcnt][dcnt])/2d;
				radius[wcnt][dcnt] = (currentEstimation[wcnt][dcnt]-drift[wcnt][dcnt])/2d;
				if (radius[wcnt][dcnt]!=0) {
					System.err.println("Problem here");
					double est0 = sketch.array[wcnt][dcnt].getEstimationRealtime(startTimeWithCurrentTime);
					double est1 = lastSyncedSketch.array[wcnt][dcnt].getEstimationRange(queryRange);
				}
			}
		}
		double[]centerMagnitude = new double[d];
		double[]radiusMagnitude = new double[d];
		for (int dcnt = 0; dcnt < d; dcnt++) {
			centerMagnitude[dcnt] = lib.getMagnitudeOfRowMultipliedByNodes(center, dcnt,numberOfNodes);
			radiusMagnitude[dcnt] = lib.getMagnitudeOfRowMultipliedByNodes(radius, dcnt,numberOfNodes);
		}
		
		// now compute local inner product
		double minMaxInnerProductPerRow = Double.MAX_VALUE;
		double minMinInnerProductPerRow = Double.MAX_VALUE;
		for (int dcnt = 0; dcnt < d; dcnt++) {
			minMaxInnerProductPerRow = Math.min(minMaxInnerProductPerRow, Math.pow(centerMagnitude[dcnt]+radiusMagnitude[dcnt],2));
			minMinInnerProductPerRow = Math.min(minMinInnerProductPerRow, Math.pow(centerMagnitude[dcnt]-radiusMagnitude[dcnt],2));
		}
		if (minMinInnerProductPerRow<0) minMinInnerProductPerRow=0;
		return new double[]{minMinInnerProductPerRow, minMaxInnerProductPerRow};
	}
		/*/

	static double getMagnitude(double[] vals) {
		double v = 0;
		for (double i:vals) v+=(i*i);
		return Math.sqrt(v);
	}
	static double getMagnitudeMultipliedByNodes(double[] vals, int nodes) {
		double v = 0;
		for (double i:vals) v+=(i*i);
		v*=(nodes*nodes);
		return Math.sqrt(v);
	}
	
	static double getMagnitudeOfRow(double[][] vals, int rowId) {
		double v = 0;
		int numberOfColumns = vals.length;
		for (int cnt=0;cnt<numberOfColumns;cnt++) v+=(vals[cnt][rowId]*vals[cnt][rowId]);
		return Math.sqrt(v);
	}
	
	static double getMagnitudeOfRowMultipliedByNodes(double[][] vals, int rowId, int nodes) {
		double v = 0;
		int numberOfColumns = vals.length;
		for (int cnt=0;cnt<numberOfColumns;cnt++) {
			v+=vals[cnt][rowId]*vals[cnt][rowId];
		}
		v*=(nodes*nodes);
		return Math.sqrt(v);
	}
	public static double[]subtractTwoVectors(double[]a,double[]b) {
		double[]c = new double[a.length];
		for (int i=0;i<a.length;i++) {
			c[i]=a[i]-b[i];
		}
		return c;
	}
	public static double[][]subtractTwoVectors(double[][]a,double[][]b) {
		double[][]c = new double[a.length][a[0].length];
		for (int i=0;i<a.length;i++) {
			c[i]=lib.subtractTwoVectors(a[i],b[i]);
		}
		return c;
	}
	static double[]subtractTwoVectorsAbsolute(double[]a,double[]b) {
		double[]c = new double[a.length];
		for (int i=0;i<a.length;i++) {
			c[i]=Math.abs(a[i]-b[i]);
		}
		return c;
	}

	public static double[] VectorSqrt(double[]a) {
		double[] b = new double[a.length];
		for (int i=0;i<a.length;i++)
			b[i] = Math.sqrt(a[i]);
		return b;
	}
	public static double[]addTwoVectors(double[]a,double[]b) {
		if (b==null)
			return a.clone();
		if (a.length!=b.length)
			System.err.println("Problem on sizes");
		double[]c = new double[Math.min(a.length,b.length)];
		for (int i=0;i<Math.min(a.length,b.length);i++) {
			c[i]=a[i]+b[i];
		}
		return c;
	}
	public static double[]scaleVector(double[]a,double b) {
		double[]c = a.clone();
		for (int i=0;i<a.length;i++) {
			c[i]/=b;
		}
		return c;
	}
	public static double[][]scaleVector(double[][]a,double b) {
		double[][]c = a.clone();
		for (int i=0;i<a.length;i++) {
			for (int j=0;j<a.length;j++) {
				c[i][j]/=b;
			}
		}
		return c;
	}
	public static double[][]multVectorNoCloning(double[][]a,double b) {
		double[][]c = a;
		for (int i=0;i<a.length;i++) {
			for (int j=0;j<a[0].length;j++) {
				c[i][j]*=b;
			}
		}
		return c;
	}
	public static double[] multVectorNoCloning(double[]a,double b) {
		double[]c = a;
		for (int i=0;i<a.length;i++) {
			c[i]*=b;
		}
		return c;
	}	
	public static double[][]addTwoVectors(double[][]a,double[][]b) {
		if (b==null)
			return a.clone();
		double[][]c = new double[a.length][];
		for (int i=0;i<a.length;i++)
			c[i] = addTwoVectors(a[i], b[i]);
		return c;
	}
	public static double[][]subtractTwoVectors2d(double[][]a,double[][]b) {
		if (b==null)
			return a.clone();
		double[][]c = new double[a.length][];
		for (int i=0;i<a.length;i++)
			c[i] = subtractTwoVectors(a[i], b[i]);
		return c;
	}
	
	public static double[][] deepCopy(double[][] original) {
	    if (original == null) {
	        return null;
	    }

	    final double[][] result = new double[original.length][];
	    for (int i = 0; i < original.length; i++) {
	        result[i] = Arrays.copyOf(original[i], original[i].length);
	    }
	    return result;
	}

	public static double[][]addManyVectors(double[][]...params) {
		double[][]c = new double[params[0].length][];
		for (int i=0;i<params.length;i++)
			for (int w=0;w<params[0].length;w++)
				c[w] = addTwoVectors(c[w], params[i][w]);
		return c;
	}

	static double[]keepMax(double[]a,double[]b) {
		double[]c = new double[a.length];
		for (int i=0;i<a.length;i++) {
			c[i]=Math.max(a[i],b[i]);
		}
		return c;
	}
	
	static double[]addTwoVectorsAbsolute(double[]a,double[]b) {
		if (b==null)
			return a.clone();
		double[]c = new double[a.length];
		for (int i=0;i<a.length;i++) {
			c[i]=a[i]+Math.abs(b[i]);
		}
		return c;
	}

	static double[] shiftByAdd(double[]oldValues,double[]shift) {
		double[] newValues= lib.addTwoVectors(oldValues, shift);
		return newValues;
	}
	
	static double[] shiftByPercent(double[]oldValues,double[]shift) {
		double[] newValues= lib.addTwoVectors(oldValues, lib.multVectors(oldValues, shift));
		return newValues;
	}
	
	public static double[]divVector(double[]a,double b) {
		double[]c = new double[a.length];
		for (int i=0;i<a.length;i++) {
			c[i]=a[i]/b;
		}
		return c;
	}
	public static double[]multVector(double[]a,double b) {
		double[]c = new double[a.length];
		for (int i=0;i<a.length;i++) {
			c[i]=a[i]*b;
		}
		return c;
	}
	static double[]multVectors(double[]a,double[]b) {
		double[]c = new double[a.length];
		for (int i=0;i<a.length;i++) {
			c[i]=a[i]*b[i];
		}
		return c;
	}

	public static String printDoubleArray(double[] array) {
		String res = "( ";
		for (int i = 0; i < array.length; i++) {
			res = res + roundDouble(array[i],100000) + " ";
		}
		res += " )";
		return res;
	}
	public static String printDoubleArrayUnrounded(double[] array) {
		String res = "( ";
		for (int i = 0; i < array.length; i++) {
			res = res + array[i] + " ";
		}
		res += " )";
		return res;
	}

	public static String roundDouble(double val, int dec) {
		if (Double.isInfinite(val)|| Double.isNaN(val)) return "NaN";
		BigDecimal bd = new BigDecimal(Double.toString(val));
		bd = bd.setScale((int)Math.log10(dec), BigDecimal.ROUND_HALF_UP);
		return ""+bd.doubleValue();
	}
	public static String syncTypeStr(int syncType) {
		switch (syncType) {
			case 0: return "Full";
			case 1: return "Partial";
			case 2: return "Lazy";
			default: return "???";
		}
	}
	public static int pow10(int power) {
		int res = 10;
		while (power-->1) res*=10;
		return res;
	}
	public static int pow2(int power) {
		int res = 2;
		while (power-->1) res*=2;
		return res;
	}
	public static double square(double val) {
		return val*val;
	}
	public static int squareInt(int val) {
		return val*val;
	}
	public static int getLog10Ceiling(double val) {
		return (int)Math.ceil(Math.log10(val));
	}
	public static int getLog2Ceiling(double val) {
		return (int)Math.ceil(Math.log(val)/Math.log(2));
	}
	
	static int NumberOfDistanceGroups; // including the zero-distance group
	static boolean useMaxDistanceGroups=false;
	static boolean ungroupSmallSteps=false;
	static final int ungroupThreshold=100;
	static boolean useDifferentExpectedStepsForStreaming=true;

	public static double[] findMiddlePoint(double[]x, double[]y) {
		double[]z = new double[x.length];
		for (int i=0;i<z.length;i++) {
			z[i]=(x[i]+y[i])/2d;
		}
		return z;
	}
	
	public static boolean vectorsEqual(double[]first,double[]second) {
		for (int d=0;d<first.length;d++)
			if (first[d]!=second[d])
				return false;
		return true;
	}

	public static double[][] fullClone(double[][] ori) {
		double[][] cloned = new double[ori.length][];
		for(int i=0;i<ori.length;i++)
			cloned[i] = ori[i].clone();
		return cloned;
	}
	public static HashSet<Integer> onlyLeft(HashSet left, HashSet right) {
		HashSet onlyLeft = new HashSet<>();
		for (Object i : left)
			if (!right.contains(i))
				onlyLeft.add(i);
		return onlyLeft;
	}
	public static HashSet<Integer> onlyRight(HashSet left, HashSet right) {
		HashSet onlyRight = new HashSet<>();
		for (Object i : right)
			if (!left.contains(i))
				onlyRight.add(i);
		return onlyRight;
	}

	
	public static HashSet<Integer> findSkylineDifferences(boolean[] current, boolean[] previous) {
		HashSet<Integer> changes = new HashSet<>();
		if (previous==null) { // i need to send the whole skyline
			// count the true bits in current
			for (int i=0;i<current.length;i++)
				if (current[i]) changes.add(i);
			return changes; //(int) Math.min(truebits*Coordinator.wordSize, current.length/8); // either 1 word for each skyline item, or the length of current/bytesize (to get the number of bytes)
		}
		else {
			for (int i=0;i<current.length;i++)
				if (current[i]!=previous[i]) changes.add(i);
			return changes;
		}
	}
	
	public static String printValues(double[] val) { 
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (double f:val) {
			sb.append(f).append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	public static void printProblem(String s) {
		System.err.println(s);
	}
	public static double getMaxDifference(double[] x, double[]y) {
		double maxDiff=0;
		for (int i=0;i<x.length;i++)  maxDiff=Math.max(Math.abs(x[i]-y[i]),maxDiff);
		return maxDiff;
	}

	public static void fixSecondDimension(double[] ds) {
		ds[2] = Math.pow(ds[1], 2);
	}
	
	public static Double[] convertToDoubleArray(double[]f) {
		Double[] ff= new Double[f.length];
		for (int i=0;i<f.length;i++)ff[i]=f[i];
		return ff;
	}

	public static void addToList(Map<Integer, ArrayList<Integer>> map, int objectid, int target) {
		ArrayList<Integer> objList = map.get(objectid);
		if (objList == null) {
			objList = new ArrayList<>();
			map.put(objectid, objList);
		}
		objList.add(target);
	}
	public static  void addToList(Map<Integer, ArrayList<Integer>> map, int objectid, ArrayList<Integer> targets) {
		ArrayList<Integer> objList = map.get(objectid);
		if (objList == null) {
			objList = new ArrayList<>();
			map.put(objectid, objList);
		}
		objList.addAll(targets);
	}

	public static double[] subtractEpsilonFromVector(double[] vector, double epsilon) {
		double[] vals = vector.clone();
		for (int i=0;i<vals.length;i++) vals[i]-=epsilon;
		return vals;
	}

	public static boolean nonZero(double[]vector) {
		boolean zeros=true;
		for (double f:vector) {
			if (f!=0) {zeros=false; break;}
		}
		return !zeros;
	}
	public static boolean equal(double[]a, double[]b) {
		for (int i=0;i<a.length;i++)
			if (a[i]!=b[i]) {
				return false;
			}
		return true;
	}
	public static boolean substantiallyDifferent(double[]a, double[]b) {
		for (int i=0;i<a.length;i++)
			if (Math.abs(a[i]-b[i])>0.000001) {
				return true;
			}
		return false;
	}
	public static boolean substantiallyDifferent(double a, double b) {
        return Math.abs(a - b) > 0.00000001;
    }

	public static boolean aGEQb(double[] a,double[]b) {
		for (int i=0;i<a.length;i++) if (a[i]<b[i]) return false;
		return true;
	}
	public static double[] averageVectors(double[][] ds) { // nodeid, vector
		int d = ds[0].length;
		int nodes = ds.length;
		double[] r = new double[d];
		for (int node=0;node<nodes;node++)
			for (int l=0;l<d;l++)
				r[l]+=ds[node][l];
		for (int l=0;l<d;l++)
			r[l]/=nodes;
		return r;
	}	
}
