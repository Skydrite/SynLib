
public class minCostTestRandom {
//	static doubleTuple findOptimal(double e) {
//		double ecm = (e*e+e)/(e*e+e+1d);
//		double esw = e*e/(1d+e);
//		return new doubleTuple(ecm,esw);
//	}
//	
	static doubleTuple findOptimal(double e) {
		double ecm = e/(1+e);
		double esw = e;
		return new doubleTuple(ecm,esw);
	}
	static double findESW(double e, double ecm) {
		boolean firstValid=false,secondValid=false;
		double esw1 = (e/ecm-1);
		double esw2 = e;
		if (esw1<=ecm/(1-ecm))
			firstValid=true;
		if (esw2>ecm/(1-ecm))
			secondValid=true;
		if (firstValid==secondValid)
			System.err.println("What on earth");
		if (firstValid)
			return esw1;
		else 
			return esw2;
	}

	static void examinePossibilities(double e) {
		doubleTuple optimalPair = findOptimal(e);
		double minCost = 1d/(optimalPair.d1*optimalPair.d2*optimalPair.d2);
		if (optimalPair.d1*optimalPair.d2==0 || minCost<0)
			System.err.println("What on Pluto");
		for (double ecm=0;ecm<=e;ecm+=e/1000d) {
			double esw = findESW(e,ecm);
			double cost = 1d/(ecm*esw*esw);
			if (cost<minCost)
				System.err.println("What on Mars");
//			else System.err.println(cost-minCost);
		}
	}
	public static void main(String[]args){
		for (double e=0.01;e<=0.5;e+=0.01) {
			examinePossibilities(e);
		}
	}
	

}
