package dataGeneration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import structure.logEventInt;

public class ProcessedStreamLoaderGeneric {
	static boolean compression=true;
	
	public static void convert(String path) throws Exception {
		long checksum=0;
		int start,stop = 0;
		String outfile;
		DataOutputStream dos = null;
		if (path.endsWith("sc.txt.gz")) {
			start=0;
			stop=10367966;
			if (compression)
				outfile = "snmp.binary.gz";
			else 
				outfile = "snmp.binary";
			// now read and write
			ProcessedStreamLoader2 psl = new ProcessedStreamLoader2(path, -1, 1);
			if (compression) 
				dos = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(outfile))));
			else
				dos = new DataOutputStream((new BufferedOutputStream(new FileOutputStream(outfile))));

			dos.writeInt(start); dos.writeInt(stop);
			while (true) {
				logEventInt levent = psl.readNextLineInt2();
				if (levent==null) 
					break;
				dos.writeInt(levent.ipaddress);dos.writeInt(levent.seconds);dos.writeInt(levent.file); dos.writeInt(levent.streamid);
			}
			dos.flush();
			dos.close();
		} else if (path.contains("wc-")){ // wc-all.gz and wc-part.gz
			if (path.contains("wc-part")) {
				start=0;
				stop=768131;
			if (compression)
				outfile = "wc-part.binary.gz";
			else
				outfile = "wc-part.binary";
			} else {
				start=0;
				stop=7518578;
				if (compression)
					outfile = "wc-all.binary.gz";
				else
					outfile = "wc-all.binary";
			}
			ProcessedStreamLoader psl = new ProcessedStreamLoader(path, -1, 1);

			if (compression) 
				dos = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(outfile))));
			else
				dos = new DataOutputStream((new BufferedOutputStream(new FileOutputStream(outfile))));

			dos.writeInt(start); dos.writeInt(stop);
			while (true) {
				logEventInt levent = psl.readNextLineInt();
				if (levent==null) 
					break;
				dos.writeInt(levent.ipaddress);dos.writeInt(levent.seconds);dos.writeInt(levent.file); dos.writeInt(levent.streamid);
				checksum+=(levent.ipaddress+levent.seconds+levent.file+levent.streamid)%10;
			}
			dos.flush();
			dos.close();
		}
		System.err.println("Write checksum is " + checksum % 100000);
	}
	
	DataInputStream dis;
	int start,stop;
	public int getStart(){
		return start;
	}
	public int getStop() {
		return stop;
	}
	
	String filename = null;
	public void reset() {
		try {
			if (compression) 
				dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(filename))));
			else
				dis = new DataInputStream((new BufferedInputStream(new FileInputStream(filename))));
			start = dis.readInt();
			stop = dis.readInt();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public ProcessedStreamLoaderGeneric(String filename) {
		int bufferSize = 32768*256*16;//1024*1024;
//		System.err.println("Buffer size is " + bufferSize);
		this.filename = filename; 
		try {
			if (compression) 
				dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(filename),bufferSize),bufferSize));
			else
				dis = new DataInputStream((new BufferedInputStream(new FileInputStream(filename))));
			start = dis.readInt();
			stop = dis.readInt();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public logEventInt readNextEvent() {
		try {
			int ipaddress = dis.readInt();
			int seconds = dis.readInt();
			int file = dis.readInt();
			int streamid = dis.readInt();
			return new logEventInt(ipaddress, seconds, file, streamid);
		} catch (Exception e) {
		}
		return null;
	}

	public logEventInt readNextEvent(int numberOfStreams) {
		try {
			int ipaddress = dis.readInt();
			int seconds = dis.readInt();
			int file = dis.readInt();
			int streamid = dis.readInt();
			if (numberOfStreams>0)
				streamid = ipaddress%numberOfStreams;
			return new logEventInt(ipaddress, seconds, file, streamid);
		} catch (Exception e) {
		}
		return null;
	}

	public logEventInt readDump(int timeUntil, int numberOfStreams) {
		byte[] bb = new byte[1024*1024];
		try {
		while (true) {
			dis.readFully(bb);
			logEventInt levent = this.readNextEvent(numberOfStreams);
			if (levent.seconds>timeUntil) return levent;
		}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void readFile () {
		logEventInt l = readNextEvent();
		long checksum=0;
		HashSet<Integer> streams=new HashSet<>();
		while (l!=null) {
			checksum+=(l.ipaddress+l.seconds+l.file+l.streamid)%10;
			streams.add(l.streamid);
			l = readNextEvent();
		}
		System.err.println("Read checksum is " + checksum % 100000);
		System.err.println("Number of streams is " + streams.size());
	}
	
	public static void mainReadOnly(String[]args) {
		ProcessedStreamLoaderGeneric psl  = new ProcessedStreamLoaderGeneric(args[0]);
		psl.readFile();
	}
	
	public static void main(String[] args) throws Exception {
		if (args[args.length-1].equals("compress"))
			compression=true;
		System.err.println("Compress is " + compression);
		long startTime= System.currentTimeMillis();
		convert(args[0]);
		long stopTime= System.currentTimeMillis();
		System.err.println("Write took "+ (stopTime-startTime)/1000);
		
		String outfile=null;
		if (compression) {
			if (args[0].contains("wc-part")) {
				outfile = "wc-part.binary.gz";
			} else if (args[0].contains("wc-")) {
				outfile = "wc-all.binary.gz";
			} else {
				outfile = "snmp.binary.gz";
			}
		} else {
			if (args[0].contains("wc-part")) {
				outfile = "wc-part.binary";
			} else if (args[0].contains("wc-")) {
				outfile = "wc-all.binary";
			} else {
				outfile = "snmp.binary";
			}
		}
		ProcessedStreamLoaderGeneric psl  = new ProcessedStreamLoaderGeneric(outfile);
		psl.readFile();
		long newstopTime= System.currentTimeMillis();
		System.err.println("Read took "+ (newstopTime-stopTime)/1000);

	}

}
