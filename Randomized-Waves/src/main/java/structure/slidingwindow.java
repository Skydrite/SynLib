package structure;

public interface slidingwindow extends Cloneable {
	public double getEpsilon();
	public double getEstimationRealtime(int query);
//	public IntDoubleTuple getEstimationRealtimeWithExpiration(int query);
	public double getEstimationRange(int query);
	public void batchUpdate(Stream s);
	public void addAZero(int time);
	public void addAOne(int time);	
	public double getRequiredMemory();
	public double getRequiredNetwork();
	public void removeExpired(int currentTime);
	public void cloneForQuerying();
	public slidingwindow clone();
	public int getLastSyncedTime();
	public int getLastUpdateTime();
	public void setLastSyncedTime(int time);
//	public int getNextExpirationTime(int startTime);
//	public int getSecondBucketExpirationTime(int startTime);
	public Pair getEstimationRealtimeWithExpiryTime(int startTime, int queryLength);
	public void removeExpiredWithExpiryTime(int startTime);
}
