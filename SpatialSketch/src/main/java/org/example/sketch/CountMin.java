package org.example.sketch;



import com.fasterxml.jackson.databind.JsonNode;
import org.example.messages.Datapoint;
import org.example.messages.Estimation;
import org.example.messages.Request;

import java.util.Arrays;
import java.util.Random;

public class CountMin extends Synopsis{

	private int[][] CM;
int width;
int depth;
int seed;

	final Random rn = new Random();
	public CountMin(int uid, String[] parameters) {
     super(uid,parameters[0],parameters[1], parameters[2]);

		this.width = Integer.parseInt(parameters[3]);
		this.depth = Integer.parseInt(parameters[4]);
		this.seed = Integer.parseInt(parameters[5]);
		CM = new int[depth][width];
		initSketch();
	}

	public void initSketch() {
		for (int j = 0; j < depth; j++) {
			for (int i = 0; i < width; i++) {
				CM[j][i] = 0;
			}
		}
	}

	int[] hash(long attrValue, int depth, int width) {
		int[] hash = new int[depth];
		rn.setSeed(attrValue + seed);
		for (int i = 0; i < depth; i++) hash[i] = rn.nextInt(width);
		return hash;
	}
	@Override
	public void add(Object k) {
		//ObjectMapper mapper = new ObjectMapper();
		JsonNode node = ((Datapoint) k).getValues();
        /*try {
            node = mapper.readTree(j);
        } catch (IOException e) {
            e.printStackTrace();
        } */
		String key = node.get(this.keyIndex).asText();
		String value = node.get(this.valueIndex).asText();
		int[] hashes = hash(Math.abs((key).hashCode()), depth, width);
		for (int j = 0; j <depth; j++) {
			int w = hashes[j];
			CM[j][w] += (int) Double.parseDouble(value); // Hash in Sample based on id
		}

	}
	public int estimateCount(long attrValue) {
		int[] hashes = hash(attrValue, depth,width);

		int[] result = new int[depth];
		for (int j = 0; j < depth; j++) {
			int w = hashes[j];
			result[j] = CM[j][w];
			//result.add(new DistinctSample(CM.get(j).get(w)));
		}
		Arrays.sort(result);
		return result[0];
	}
	@SuppressWarnings("deprecation")
	@Override
	public Object estimate(Object k)
	{
		return Long.toString(estimateCount((long) k));
	}

	@Override
	public Synopsis merge(Synopsis sk) {
		return sk;	
	}

	@Override
	public Estimation estimate(Request rq) {
		return new Estimation(rq, Double.toString((double)estimateCount(Math.abs((rq.getParam()[0]).hashCode()))), Integer.toString(rq.getUID()));


	}
	
	
	
	
}
