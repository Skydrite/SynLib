package structure;

public class ArrayDequeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i=0;
		ArrayDeque<Integer> ad = new ArrayDeque<>(15);
		for (int cnt=0;cnt<30;cnt++) ad.addLast(i++);
		for (Integer j:ad)
			System.err.println(j);
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.addLast(new Integer(0));
		ad.addLast(new Integer(1));
		ad.addLast(new Integer(2));
		ad.addLast(new Integer(3));
		ad.addLast(new Integer(4));
		ad.addLast(new Integer(5));
		ad.addLast(new Integer(6));
		ad.addLast(new Integer(7));
		ad.addLast(new Integer(8));
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();
		ad.pollFirst();

		for (Integer j:ad)
			System.err.println(j);
		for (int cnt=0;cnt<30;cnt++) ad.addLast(i++);
		for (Integer j:ad)
			System.err.println(j);
	}

}
