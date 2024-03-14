package structure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class sketch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Random rn = new Random(System.currentTimeMillis());

		int rows = 100, columns = 100;
		int numberOfElements = 1000;
		int domainLength = 1000;
		Xi_EH3[] agms_eh3 = new Xi_EH3[rows * columns];
		// Xi_EH3
		{
			for (int i = 0; i < rows * columns; i++) {
				int I1 = rn.nextInt();
				int I2 = rn.nextInt();
				agms_eh3[i] = new Xi_EH3(I1, I2);
			}
		}

		sketch sk1 = new sketch(rows, columns, agms_eh3);
		sketch sk2 = new sketch(rows, columns, agms_eh3);
		int[] series1 = new int[domainLength];
		int[] series2 = new int[domainLength];

		for (int cnt = 0; cnt < numberOfElements; cnt++) {
			int val = rn.nextInt(domainLength);
			series1[val]++;
		}
		for (int cnt = 0; cnt < numberOfElements; cnt++) {
			int val = rn.nextInt(domainLength);
			series2[val]++;
		}
		// compute joinsize
		double joinsize = 0;
		for (int cnt = 0; cnt < domainLength; cnt++) {
			joinsize += (series1[cnt] * series2[cnt]);
		}
		System.err.println("Accurate join size is " + joinsize);

		// do sketches
		for (int cnt = 0; cnt < domainLength; cnt++)
			for (int i = 0; i < series1[cnt]; i++)
				sk1.update(cnt, +1);
		for (int cnt = 0; cnt < domainLength; cnt++)
			for (int i = 0; i < series2[cnt]; i++)
				sk2.update(cnt, +1);

		System.err
				.println("Estimate join size is " + sk1.estimateJoinSize(sk2));
	}

	final int rows, columns;
	final Xi_EH3[] agms_eh3;
	public final double sketch_elem[];

	public sketch(int rows, int columns, Xi_EH3[] agms_eh3) {
		this.rows = rows;
		this.columns = columns;
		this.agms_eh3 = agms_eh3;
		this.sketch_elem = new double[rows * columns];
	}

	public void update(int key, double func) {
		for (int i = 0; i < rows * columns; i++)
			sketch_elem[i] = sketch_elem[i] + agms_eh3[i].element(key) * func;
	}

	public double estimateJoinSize(sketch sk1) {
		double[] basic_est = new double[rows * columns];
		for (int i = 0; i < rows * columns; i++)
			basic_est[i] = sketch_elem[i] * sk1.sketch_elem[i];

		double[] avg_est = new double[rows];
		for (int i = 0; i < rows; i++)
			avg_est[i] = Average(basic_est, i * columns, columns);

		double result = Median(avg_est, rows);

		return result;

	}

	double Average(double[] x, int startPoint, int n) {
		double sum = 0;
		for (int i = startPoint; i < startPoint + n; i++)
			sum += x[i];

		sum = sum / (double) n;
		return sum;
	}

	double Median(double[] x, int n) {
		if (n == 1)
			return x[0];

		if (n == 2)
			return (x[0] + x[1]) / 2;

		ArrayList<Double> valsToSort = new ArrayList<Double>(x.length);
		for (double xx : x)
			valsToSort.add(xx);
		Collections.sort(valsToSort);
		double res;

		if (n % 2 == 0)
			res = (valsToSort.get(n / 2 - 1) + valsToSort.get(n / 2)) / 2.0;
		else
			res = valsToSort.get(n / 2);
		return res;
	}
}

class Xi_EH3 {
	final long[] seeds = new long[2];

	public double element(int key) {
		long i0 = seeds[0];
		long i1 = seeds[1];
		double res = hash(i0, i1, key);
		return res;
	}

	double hash(long i0, long I1, long j) {
		long mask = 0xAAAAAAAA;
		long p_res = (I1 & j) ^ (j & (j << 1) & mask);

		int res = (((i0 ^ seq_xor(p_res)) & 1l) == 1l) ? 1 : -1;
		return res;
	}

	long seq_xor(long x) {
		x ^= (x >> 16);
		x ^= (x >> 8);
		x ^= (x >> 4);
		x ^= (x >> 2);
		x ^= (x >> 1);

		return (x & 1l);
	}

	public Xi_EH3(long I1, long I2) {
		long k_mask = 0xffffffff;

		seeds[0] = ((I1 << 16) ^ (I2 & 0177777)) & 1;

		I1 = 36969 * (I1 & 0177777) + (I1 >> 16);
		I2 = 18000 * (I2 & 0177777) + (I2 >> 16);

		seeds[1] = ((I1 << 16) ^ (I2 & 0177777)) & k_mask;
	}

}