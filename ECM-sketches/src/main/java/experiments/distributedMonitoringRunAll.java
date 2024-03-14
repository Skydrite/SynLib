package experiments;

import java.util.ArrayList;
import java.util.HashSet;

public class distributedMonitoringRunAll {

	/** 
	 * @param args
	 */
	public static void main(String[] args) {
		args = new String[8];
		int repeats=1;
		
		ArrayList<String> al = new ArrayList<>();
		for (String dataset : new String[] { "wc-full.binary.gz", "ips.binary.gz" }) { // repeat for both datasets
			args[1] = dataset;
			// first vary number of peers
			for (int numberOfPeers = 2; numberOfPeers <= 256; numberOfPeers *= 2) {
				args[0] = "" + numberOfPeers;
				args[2] = "" + 0.15; // delta
				float epsilon = 0.15f;
				args[3] = "" + epsilon;
				args[4] = "" + repeats; // repeats
				float accuracy = 0.15f;
				String dd = dataset.replaceAll(".binary.gz", ".txt");
				args[5] = "" + accuracy;
				args[6] = "0"; // unused - for point queries only
				if (dataset.startsWith("ips"))
					args[7] = "200000000"; // 2000 seconds - unit is 0.001 milliseconds
				else
					args[7] = "2000000"; // 2 million seconds
				String cmd = ("\njava -cp ContinuousEH.jar -Xmx4g experiments.distributedMonitoringSendCM ");
				for (String s : args)
					cmd = cmd + (s + " ");
				cmd = cmd + (" &> N" + numberOfPeers + ".E" + (int) (epsilon * 100) + ".A" + (int) (accuracy * 100) + "." + dd);
				if (!al.contains(cmd))
					al.add(cmd);
			}
		}
		

		for (String dataset : new String[] { "wc-full.binary.gz", "ips.binary.gz"  }) { // repeat for both datasets
			args[1] = dataset;
			int numberOfPeers=16;
			// vary accuracy
			for (float accuracy=0.05f;accuracy<=0.31;accuracy+=0.05) {
				args[0] = "" + numberOfPeers;
				args[2] = "" + 0.15f; // delta
				float epsilon = 0.15f;
				args[3] = "" + epsilon;
				args[4] = "" + repeats; // repeats
				String dd = dataset.replaceAll(".binary.gz", ".txt");
				args[5] = "" + accuracy;
				args[6] = "0"; // unused - for point queries only
				if (dataset.startsWith("ips"))
					args[7] = "200000000"; // 2000 seconds - unit is 0.001 milliseconds
				else
					args[7] = "2000000"; // 2 million seconds
				String cmd = ("\njava -cp ContinuousEH.jar -Xmx4g experiments.distributedMonitoringSendCM ");
				for (String s : args)
					cmd = cmd + (s + " ");
				cmd = cmd + (" &> N" + numberOfPeers + ".E" + (int) (epsilon * 100) + ".A" + (int) (accuracy * 100) + "." + dd);
				if (!al.contains(cmd))
					al.add(cmd);
			}
		}
		
		for (String dataset : new String[] { "wc-full.binary.gz", "ips.binary.gz"  }) { // repeat for both datasets
			args[1] = dataset;
			int numberOfPeers=16;
			float accuracy=0.15f;
			// vary accuracy
			for (float epsilon=0.05f;epsilon<=0.31;epsilon+=0.05) {
				args[0] = "" + numberOfPeers;
				args[2] = "" + 0.15; // delta
				args[3] = "" + epsilon;
				args[4] = "" + repeats; // repeats
				String dd = dataset.replaceAll(".binary.gz", ".txt");
				args[5] = "" + accuracy;
				args[6] = "0"; // unused - for point queries only
				if (dataset.startsWith("ips"))
					args[7] = "200000000"; // 2000 seconds - unit is 0.001 milliseconds
				else
					args[7] = "2000000"; // 2 million seconds
				String cmd = ("\njava -cp ContinuousEH.jar -Xmx4g experiments.distributedMonitoringSendCM ");
				for (String s : args)
					cmd = cmd + (s + " ");
				cmd = cmd + (" &> N" + numberOfPeers + ".E" + (int) (epsilon * 100) + ".A" + (int) (accuracy * 100) + "." + dd);
				if (!al.contains(cmd))
					al.add(cmd);
			}
		}

		for (String s:al) System.err.print(s);
//		for (String dataset:new String[]{"snmp.binary.gz","wc-full.binary.gz"}) {
//			args[1] = dataset;
//			for (int numberOfPeers=2;numberOfPeers<=256;numberOfPeers*=2) {
//				args[0] = ""+numberOfPeers;
//				args[2] = "" + 0.1; //delta
//				for (float epsilon=0.05f;epsilon<0.3;epsilon+=0.05) {
//					args[3] = ""+ epsilon;
//					args[4] = "" + repeats; // repeats
//					for (float accuracy=0.05f;accuracy<0.3;accuracy+=0.05) {
//						String dd = dataset.replaceAll(".binary.gz", ".txt");
//						args[5] = ""+accuracy;
//						args[6] = "0"; //unused -  for point queries only
//						args[7] = "1000000"; // 1 million seconds
//						System.err.print("\njava -cp ContinuousEH.jar -Xmx4g experiments.distributedMonitoringSendCM ");
//						for (String s:args) System.err.print(s + " ");
//						System.err.print( " &> N" +numberOfPeers+".E"+(int)(epsilon*100) + ".A"+(int)(accuracy*100) + "." + dd);
////						distributedMonitoringSendCM.main(args);
//
//					}
//				}
//			}
//		}
	}

}
