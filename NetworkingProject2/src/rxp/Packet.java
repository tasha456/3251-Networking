package rxp;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Packet {
	public final int HEADER_LENGTH = 31;
	public final int ERROR_DETECTION_LENGTH = 31-12;
	long sequenceNumber;
	int windowSize;
	byte[] errorDetection;
	byte[] data;
	boolean ackFlag;
	boolean finFlag;
	boolean synFlag;
	boolean isCorrupted;
	InetAddress address;
	int portNumber;
	
	/**
	 * This is used to create a new packet where you manually input the flags
	 * and data to be used
	 * @param sequenceNumber
	 * @param ackFlag
	 * @param finFlag
	 * @param synFlag
	 * @param windowSize
	 * @param data
	 */
	public Packet(long sequenceNumber,boolean ackFlag,boolean finFlag,boolean synFlag,
			int windowSize,byte[] data){
		this.sequenceNumber = sequenceNumber;
		this.ackFlag = ackFlag;
		this.finFlag = finFlag;
		this.synFlag = synFlag;
		this.windowSize = windowSize;
		this.data = data;
	}
	/**
	 * This is used to convert a byte array into a readable packet
	 * @param rawBytes
	 * @param address
	 * @param portNumber
	 */
	public Packet(byte[] rawBytes,InetAddress address,int portNumber){
		this.address = address;
		this.portNumber = portNumber;
		this.sequenceNumber = readLong(rawBytes,0);
		boolean[] flags = readBoolean(rawBytes[8]);
		this.ackFlag = flags[5];
		this.finFlag = flags[6];
		this.synFlag = flags[7];
		this.windowSize = readInt(rawBytes,9);
		this.errorDetection = new byte[ERROR_DETECTION_LENGTH];
		int dataLength = rawBytes.length - HEADER_LENGTH;
		if(dataLength > 0){
			this.data = new byte[dataLength];
			System.arraycopy(rawBytes, HEADER_LENGTH, this.data, 0, dataLength);
		} else{
			this.data = null;
		}
		//Error detection
		byte[] adjustedBytes = new byte[rawBytes.length];
		System.arraycopy(rawBytes, 0, adjustedBytes, 0, rawBytes.length);
		prepareForHash(adjustedBytes);
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(adjustedBytes);
			this.isCorrupted = false;
			for(int i = 0;i<hash.length;i++){
				if(hash[i] != errorDetection[i]){
					this.isCorrupted = true;
				}
			}
		} catch (NoSuchAlgorithmException e) {
			this.isCorrupted = true;
		}
	}
	public byte[] getRawBytes(){
		byte[] answer = new byte[data.length+ HEADER_LENGTH];
		byte[] seqNum = java.nio.ByteBuffer.allocate(8).putLong(this.sequenceNumber).array();
		System.arraycopy(seqNum, 0, answer, 0, 8);
		String flagString = "00000";
		flagString += this.ackFlag == true? "1":"0";
		flagString += this.finFlag == true? "1":"0";
		flagString += this.synFlag == true? "1":"0";
		answer[8] = Byte.parseByte(flagString,2);
		byte[] winSize = java.nio.ByteBuffer.allocate(4).putInt(this.windowSize).array();
		System.arraycopy(winSize, 0, answer, 9, 4);
		
		byte[] adjustedBytes = new byte[this.data.length];
		System.arraycopy(this.data, 0, adjustedBytes, 0, this.data.length);
		prepareForHash(adjustedBytes);
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			this.errorDetection = md.digest(adjustedBytes);
		} catch (NoSuchAlgorithmException e) {
			return null; //failed to hash...for some unknown reason
		}
		if(errorDetection == null || errorDetection.length != ERROR_DETECTION_LENGTH){
			return null;
		}
		System.arraycopy(this.errorDetection, 0, answer, 
				HEADER_LENGTH-ERROR_DETECTION_LENGTH,ERROR_DETECTION_LENGTH);
		return answer;
	}
	public long getSequenceNumber(){
		return this.sequenceNumber;
	}
	public long getWindowSize(){
		return this.windowSize;
	}
	public byte[] getData(){
		return this.data;
	}
	public boolean getAckFlag(){
		return this.ackFlag;
	}
	public boolean getFinFlag(){
		return this.finFlag;
	}
	public boolean getSynFlag(){
		return this.synFlag;
	}
	public boolean getIsCorrupted(){
		return this.isCorrupted;
	}
	public InetAddress getAddress(){
		return this.address;
	}
	public int getPort(){
		return this.portNumber;
	}
	
	/**
	 * this method clears the errorDetection bytes out of the supplied array
	 * so that the rest of the array can be hashed, in the same way it is 
	 * hashed to set the errorDetection bits
	 * @param adjustedBytes a byte array representing the packet you want prepared
	 */
	private void prepareForHash(byte[] adjustedBytes){
		int errorIndex = 11;
		int errorLength = 20;
		for(int index = errorIndex;index < errorIndex + errorLength;index++){
			adjustedBytes[index] = 0;
		}
	}
	/**
	 * input a byte array, and it outputs the long number value of the array
	 * @param array the byte[] array to be used
	 * @param startIndex the starting index in the array of the bytes you want 
	 * converted to a number
	 * @return the long number value of the bytes at the specified index
	 */
	private long readLong(byte[] array,int startIndex){
		byte[] temp = new byte[8];
		System.arraycopy(array, startIndex, temp, 0, 8);
		return java.nio.ByteBuffer.wrap(temp).getLong();
	}
	/**
	 * input a byte array, and it outputs the integer value of the array
	 * @param array the byte[] array to be used
	 * @param startIndex the starting index in the array of the bytes you want 
	 * converted to a number
	 * @return the integer value of the bytes at the specified index
	 */
	private int readInt(byte[] array,int startIndex){
		byte[] temp = new byte[4];
		System.arraycopy(array, startIndex, temp, 0, 4);
		return java.nio.ByteBuffer.wrap(temp).getInt();
	}
	/**
	 * converts a byte to an array of boolean values
	 * @param array the byte[] array to be used, will always have a length of 8 
	 * @return an array of booleans
	 */
	private boolean[] readBoolean(byte input){
		boolean[] ans = new boolean[8];
		for(int position = 0;position < 8;position++){
			ans[position] = ((input >> position) & 1) == 1;
		}
		return ans;
	}
}
