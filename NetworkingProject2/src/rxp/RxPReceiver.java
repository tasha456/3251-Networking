package rxp;

import java.io.IOException;
import java.net.InetAddress;

/**
 * this class runs on a seperate thread, to handle incoming RxP Packets
 *
 */
public class RxPReceiver {

	byte[] data;
	boolean[] validData;
	int readIndex; //last index it read
	int receiveIndex; //next index to write to
	InetAddress address;
	RxPParent parent;
	int port;
	/**
	 * creates a receiver socket whose job is to manage sending ACKs,
	 * and window sizes to the opposite end of the connection
	 * @param bufferSize the amount of space you want to be able to use to store incoming messages until the application requests the data.
	 * @param address the opposite address this connection is connected to.
	 * @param port the port number of the opposite end of this connection.
	 * @param parent the RxPParent object that should be used when sending packets.
	 */
	public RxPReceiver(int bufferSize,InetAddress address,int port,RxPParent parent){
		this.parent = parent;
		this.data = new byte[bufferSize + 1];
		validData = new boolean[data.length];
		this.address = address;
		this.port = port;
		readIndex = -1;
		receiveIndex = 0;
	}
	/**
	 * takes in a packet, and if there is space in the buffer it will store the data inside the packet.
	 * If it does not have room to store the data, it will fail silently.
	 * @param packet the Packet object who's data field we want to store.
	 */
	public void receivePacket(Packet packet){
		if(getRemainingSpace() >= packet.getData().length){
			copyData(packet.getData());
			long ackNumber = packet.getSequenceNumber() + packet.getData().length;
			Packet sendPacket = new Packet(ackNumber, true, false, false, 0, 
						address,port,null);
			try {
				parent.sendPacket(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void readData(byte[] receiver){
		int length = receiver.length;
		int maximumLength = getDataLength();
		if(length> maximumLength){
			length = maximumLength;
		}
		System.arraycopy(this.data, readIndex+1, receiver, 0, length);
		int temp = readIndex;
		int total = 0;
		while(total < length){
			validData[temp] = false;
			temp++;
			temp = temp % this.validData.length;
			total++;
		}
		readIndex += length % this.data.length;
	}
	private void copyData(byte[] data){
		System.arraycopy(data, 0, this.data, receiveIndex, data.length);
		int temp = receiveIndex;
		int total = 0;
		while(total < data.length){
			validData[temp] = true;
			temp++;
			temp = temp % this.validData.length;
			total++;
		}
		receiveIndex = receiveIndex + data.length % this.data.length;
		
	}
	private int getDataLength(){
		int theoryLength = 0;
		if(readIndex > receiveIndex){
			theoryLength = this.data.length - readIndex -1 + receiveIndex;
		} else{
			theoryLength =  receiveIndex - readIndex -1;
		}
		int actualLength = 0;
		while(actualLength < theoryLength){
			if(this.validData[readIndex + actualLength + 1] == false){
				return actualLength;
			}
			actualLength++;
		}
		return actualLength;
	}
	public int getRemainingSpace(){
		if(readIndex > receiveIndex){
			return readIndex - receiveIndex-1;
		} else{
			return (data.length - receiveIndex) + readIndex -1;
		}
	}
}
