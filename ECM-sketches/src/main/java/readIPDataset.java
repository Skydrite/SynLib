import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

public class readIPDataset {

	static int[] freqs = null;

	static int getIPSubnet(String fullip, int maskBytes) {
		String[] nums = fullip.split("\\.");
		if (nums.length < 4)
			return -1;
		int val = 0;
		if (nums.length==4 && maskBytes==4) 
			for (int i = 0; i < maskBytes; i++)
				val += (val * 255 + Integer.parseInt(nums[i].replace(":", "")));
		else
			for (int i = 0; i < maskBytes; i++)
				val += (val * 255 + Integer.parseInt(nums[i]));
		return val;
	}

	static long getTime(String time) {
		int hour = Integer.parseInt(time.substring(0, 2));
		int min = Integer.parseInt(time.substring(3, 5));
		int sec = Integer.parseInt(time.substring(6, 8));
		int microSecLooseOneChar = Integer.parseInt(time.substring(9, 14)); 
		long t = microSecLooseOneChar + sec * 100000 + min * 100000 * 60 + (hour-15) * 100000 * 60 * 60 - 354411237;
		return t;
	}

	static int[] ipSequence = new int[(int) Math.pow(2, 24)];
	static int cnt = 0;
	static HashMap<Integer, Integer> ips = new HashMap(100000);
	static line getLine(BufferedReader br, int maskBytes) throws Exception {
		String line = br.readLine();
		if (line==null) return null;
		while (line.startsWith("packet") || line.length() < 10)
			line = br.readLine(); // ignore this
		String[] strs = line.split(" ");
		long time = getTime(strs[0]);
		int firstIP = getIPSubnet(strs[1], maskBytes);
		if (firstIP == -1)
			return getLine(br, maskBytes);
		int secIP = getIPSubnet(strs[2], maskBytes);
		if (secIP == -1)
			return getLine(br, maskBytes);

		int firstIPSequential=0,secIPSequential=0;
		if (maskBytes<4) {
			if (ipSequence[firstIP] == 0)
				ipSequence[firstIP] = ++cnt;
			firstIPSequential = ipSequence[firstIP];
	
			if (ipSequence[secIP] == 0)
				ipSequence[secIP] = ++cnt;
			secIPSequential = ipSequence[secIP];
		} else {
			Integer ipid = ips.get(firstIP);
			if (ipid==null) {
				ips.put(firstIP, ++cnt);
				firstIPSequential = cnt;
			} else {
				firstIPSequential = ipid;
			}
			
			ipid = ips.get(secIP);
			if (ipid==null) {
				ips.put(secIP, ++cnt);
				secIPSequential = cnt;
			} else {
				secIPSequential = ipid;
			}
		}
		
		if (strs.length > 3) {
			String size = strs[3];
			int separatorpos = size.indexOf(':');
			if (separatorpos > 0) {
				long sizeStart = Long.parseLong(size.substring(0, separatorpos));
				long sizeStop = Long.parseLong(size.substring(separatorpos + 1, size.length() - 1));
				int s = (int) (sizeStop - sizeStart);
				return new line(time, firstIPSequential, secIPSequential, s);
			}
		}
		return new line(time, firstIPSequential, secIPSequential, 1);
	}

	public static Random rn = new Random();

	public static int getRouterId(int maxRouters, line l) {
		rn.setSeed(l.secIP);
		return rn.nextInt(maxRouters);
	}

	public static void main(String[] args) throws Exception {
		int maxRouters = 1024;
//		line waitingToExpire = null;
		int maskBytes = 4;
//		int slidingWindowLength = 100000000;
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		DataOutputStream dos = null;
		if (args.length>1) {
			dos = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(args[1]))));
		}
//		BufferedReader brExpire = new BufferedReader(new FileReader(args[0]));
//		waitingToExpire = getLine(brExpire, maskBytes);
		dos.writeInt(0); dos.writeInt(371995477); // start and stop
		while (br.ready()) {
			line l2 = getLine(br, maskBytes);
			if (l2 == null) break;
			long time = l2.time;
//			while (time - slidingWindowLength > waitingToExpire.time) { // expire
//				int routerid = getRouterId(maxRouters, waitingToExpire);
//				System.out.println(routerid + " -" + waitingToExpire.firstIP + " " + waitingToExpire.size);
//				waitingToExpire = getLine(brExpire, maskBytes);
//			}
			int routerid = getRouterId(maxRouters, l2);
			if (dos == null)
				System.out.println(l2.firstIP + " " + time + " " + l2.secIP + " " + routerid );
			else {
				dos.writeInt(l2.firstIP);
				dos.writeInt((int)time);
				dos.writeInt(l2.secIP);
				dos.writeInt(routerid);
			}
		}
		dos.flush();dos.close();
		System.err.println("Found ips: " + cnt);
	}

	public static void main2(String[] args) throws Exception {
		line waitingToExpire = null;
		int maskBytes = 2;
		int slidingWindowLength = 100000;
		freqs = new int[(int) Math.pow(2, 16)];
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		BufferedReader brExpire = new BufferedReader(new FileReader(args[0]));
		waitingToExpire = getLine(brExpire, maskBytes);

		while (br.ready()) {
			HashSet<Integer> allThisStep = new HashSet<>(1000);
			line l = getLine(br, maskBytes);
			allThisStep.add(l.firstIP);
			long time = l.time;
			freqs[l.firstIP]++;

			while (time - slidingWindowLength > waitingToExpire.time) { // expire
				freqs[waitingToExpire.firstIP]--; // expire
				allThisStep.add(waitingToExpire.firstIP);
				waitingToExpire = getLine(brExpire, maskBytes);
			}
			// now print freq. for allThisStep
			for (int i : allThisStep)
				System.err.println(i + " " + freqs[i]);
		}
	}
}

class line {
	long time;
	int firstIP;
	int secIP;
	int size;

	public line(long time, int firstIP, int secIP, int size) {
		this.time = time;
		this.firstIP = firstIP;
		this.secIP = secIP;
		this.size = size;
	}
}
