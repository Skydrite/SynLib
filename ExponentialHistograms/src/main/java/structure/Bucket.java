package structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import dataGeneration.StreamConstructor;



class Bucket implements Cloneable {
	int time;
	float trueBits;
	public Bucket(int time, float trueBits) {
		this.time = time; this.trueBits = trueBits;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public void setTrueBits(float trueBits) {
		this.trueBits = trueBits;
	}
	public int getTime() {
		return time;
	}
	public float getTrueBits() {
		return trueBits;
	}
	public void addTrueBits(float tb) {
		this.trueBits+=tb;
	}
	public Bucket clone() {
		return new Bucket(this.time, this.trueBits);
	}
}

