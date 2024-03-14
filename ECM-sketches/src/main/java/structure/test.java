package structure;

import java.util.Random;

public class test {
	public static void mainOld2(String[] args) {
		Random rn = new Random();
		ArrayDeque<Integer> ad =new ArrayDeque(50);
		int [] intArray = new int[100];
		for (int i=0;i<intArray.length;i++) {
			intArray[i] = rn.nextInt();
			ad.add(intArray[i]);
			for (int j=0;j<i;j++) {
				int stored = ad.getSelected(j);
				if (stored!=intArray[j])
					System.err.println("Problem");;
			}
		}
	}
	public static void main3(String[] args) {
		Random rn = new Random();
		ArrayDeque ad =new ArrayDeque(50);
		int [] intArray = new int[100];
		for (int i=0;i<intArray.length;i++) {
			intArray[i] = i;
			ad.add(intArray[i]);
			for (int j=0;j<i;j++) {
				int stored = (Integer)ad.getSelected(j);
				if (stored!=intArray[j])
					System.err.println("Problem");;
			}
		}
	}
	public static void main(String[] args) {
		Random rn = new Random();
		cBuffer ad =new cBuffer<>(5);
		int [] intArray = new int[100];
		for (int i=0;i<intArray.length;i++) {
			intArray[i] = i;
			if (i%3==2) {
				System.err.println("Deleting oldest element");
				ad.pollFirst();
				System.err.println("And the buffer after delete is " + ad.toString());
			}
			System.err.println("Adding new element");
			ad.addLast(intArray[i]);
			System.err.println("And the buffer after add is " + ad.toString());
		}
	}

}
