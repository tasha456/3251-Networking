package rxp;
/**
 * this class runs on a seperate thread, to handle incoming RxP Packets
 *
 */
public class RxPReceiver {

	byte[] data;
	int readIndex; //last index it read
	int receiveIndex; //next index to write to
	/**
	 * creates a receiver socket whose job is to manage sending acks,
	 * and window sizes to the opposite end of the connection
	 * @param bufferSize 
	 */
	public RxPReceiver(int bufferSize){
		this.data = new byte[bufferSize + 1];
		readIndex = -1;
		receiveIndex = 0;
	}
	public void receivePacket(Packet packet){
		if(getRemainingSpace() >= packet.getData().length){
			copyData(packet.getData());
			long ackNumber = packet.getSequenceNumber() + packet.getData().length;
			Packet sendPacket = new Packet(ackNumber, true, false, false, 0, null);
		}
	}
	public void readData(byte[] receiver){
		int length = receiver.length;
		int maximumLength = getDataLength();
		if(length> maximumLength){
			length = maximumLength;
		}
		System.arraycopy(this.data, readIndex+1, receiver, 0, length);
		readIndex += length % this.data.length;
	}
	private void copyData(byte[] data){
		System.arraycopy(data, 0, this.data, receiveIndex, data.length);
		receiveIndex = receiveIndex + data.length % this.data.length;
	}
	private int getDataLength(){
		if(readIndex > receiveIndex){
			return this.data.length - readIndex -1 + receiveIndex;
		} else{
			return receiveIndex - readIndex -1;
		}
	}
	public int getRemainingSpace(){
		if(readIndex > receiveIndex){
			return readIndex - receiveIndex-1;
		} else{
			return (data.length - receiveIndex) + readIndex -1;
		}
	}
}
