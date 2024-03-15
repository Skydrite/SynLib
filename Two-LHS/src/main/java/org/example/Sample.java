package org.example;

public abstract class Sample {
    public int n;
    public long hashDomain;
    public int bitSize = 31;
    protected String setting;
    protected int ram;
    public int memUsageSynopsis;
    public int curSampleSize;

    public Sample() {

    }
    //public abstract void add(int id);
    public abstract void add(long hx);

    public abstract void ingest(long hx, int sign);

    //public abstract Sample intersect(Sample sample);
    //public abstract Sample union(Sample sample);

    public abstract void ingest(long[] vals, int sign);

    public abstract void reset();

    public long getMemoryUsage() {
        return memUsageSynopsis;
    }
}
