package rxp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * this class runs on a seperate thread, to handle incoming RxP Packets
 *
 */
public class RxPReceiver {

	public static final int TIMEOUT = 100; //miliseconds before pseudo ack for window size update is sent.
	Byte[] data;
	int readIndex; //last index it read
	int receiveIndex; //next index to write to
	long indexOffset;
	InetAddress address;
	RxPParent parent;
	RxPSender sender;
	int port;
	int windowSize;
	int maximumWindowSize;
	int timeDifference;
	private final Lock lock = new ReentrantLock();
	/**
	 * creates a receiver socket whose job is to manage sending ACKs,
	 * and window sizes to the opposite end of the connection
	 * @param bufferSize the amount of space you want to be able to use to store incoming messages until the application requests the data.
	 * @param address the opposite address this connection is connected to.
	 * @param port the port number of the opposite end of this connection.
	 * @param parent the RxPParent object that should be used when sending packets.
	 */
	public RxPReceiver(int bufferSize,long sequenceNumber,
			InetAddress address,int port,RxPParent parent,RxPSender sender){
		this.indexOffset = sequenceNumber;
		this.parent = parent;
		this.sender = sender;
		this.data = new Byte[bufferSize + 1];
		maximumWindowSize = this.data.length;
		this.address = address;
		this.port = port;
		this.timeDifference = 0;
		this.windowSize = data.length;
		readIndex = -1;
		receiveIndex = 0;
	}
	/**
	 * This is used to check if a pseudoAck needs to be sent.
	 * @param deltaT the time difference since the last time this method was called.
	 */
	public void update(int deltaT){
		//timeDifference += deltaT;
		if(timeDifference >= TIMEOUT){
			timeDifference = 0;
			Packet packet = new Packet(indexOffset-1, true, false, false, this.windowSize, address, port, null);
			try {
				parent.sendPacket(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * takes in a packet, and if there is space in the buffer it will store the data inside the packet.
	 * If it does not have room to store the data, it will fail silently.
	 * @param packet the Packet object who's data field we want to store.
	 */
	public void receivePacket(Packet packet){
		
		lock.lock();
		if(packet.getAckFlag()){
			sender.acknowledge(packet.getSequenceNumber(),packet.getWindowSize());
		} else if(indexOffset <= packet.getSequenceNumber() && 
					indexOffset + data.length > packet.getSequenceNumber()){
			if(packet.getData() != null){
				boolean result = copyData(packet.getData(),packet.getSequenceNumber());
				if(result == true){
					long ackNumber = packet.getSequenceNumber() + packet.getData().length;
					Packet sendPacket = new Packet(ackNumber, true, false, false, windowSize, 
								address,port,null);
					try {
						parent.sendPacket(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else if(windowSize > 0){  //control packet, just send an ack with the windowSize
				long ackNumber = packet.getSequenceNumber();
				Packet sendPacket = new Packet(ackNumber, true, false, false, windowSize, 
							address,port,null);
				try {
					parent.sendPacket(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if(indexOffset > packet.getSequenceNumber()){
			if(packet.getData() != null){
				//we've already received and read the packet, really late duplicate packet
				long ackNumber = packet.getSequenceNumber() + packet.getData().length;
				Packet sendPacket = new Packet(ackNumber, true, false, false, windowSize, 
						address,port,null);
				try {
					parent.sendPacket(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if(windowSize > 0){  //control packet, just send an ack with the windowSize
				long ackNumber = packet.getSequenceNumber();
				Packet sendPacket = new Packet(ackNumber, true, false, false, windowSize, 
							address,port,null);
				try {
					parent.sendPacket(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else{
			if(packet.getData() != null){
			} else if(windowSize > 0){  //control packet, just send an ack with the windowSize
				long ackNumber = packet.getSequenceNumber();
				Packet sendPacket = new Packet(ackNumber, true, false, false, windowSize, 
							address,port,null);
				try {
					parent.sendPacket(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//System.out.println("window size: " + windowSize);
		lock.unlock();
	}
	public void setMaximumWindowSize(int size){
		lock.lock();
		if(size > maximumWindowSize){
			Byte[] temp = new Byte[size];
			int i = 0;
			while(i < data.length){
				temp[i] = data[i];
				i++;
			}
			this.data = temp;
		} else if(size < maximumWindowSize){
			int difference = maximumWindowSize - size;
			windowSize = windowSize - difference;
			maximumWindowSize = size;
		}
		lock.unlock();
	}
	public int readData(byte[] receiver){
		
		lock.lock();
		int index = 0;
		while(index < receiver.length && index < data.length){
			if(data[index] != null){
				receiver[index] = data[index];
			} else{
				break;
			}
			index++;
		}
		
		int totalBytesRead = index;
		this.indexOffset += totalBytesRead;
		this.windowSize = windowSize + totalBytesRead;
		//index now holds the index where we should start reading from next time
		//move remaining data up
		int replacement = 0;
		while(index < data.length){
			data[replacement] = data[index];
			index++;
			replacement++;
		}
		//replacement now holds the index after the last byte of potentially valid data
		while(replacement < data.length){
			data[replacement] = null;
			replacement++;
		}
		lock.unlock();
		
		return totalBytesRead;
	}
	private boolean copyData(byte[] data,long seqNum){
		int originalBufferIndex = (int)(seqNum - indexOffset);
		int bufferIndex = originalBufferIndex;
		if(originalBufferIndex + data.length > this.maximumWindowSize){
			return false;
		}
		int i = 0;
		while(i < data.length){
			this.data[bufferIndex] = new Byte(data[i]);
			i++;
			bufferIndex++;
		}
		windowSize = Math.min(this.data.length - (originalBufferIndex + data.length),windowSize);
		return true;
	}
	public int getWindowSize(){
		if(windowSize < 0){}
		//	recalculateWindowSize();
		return windowSize;
	}
	private void recalculateWindowSize(){
		int i = 0;
		int winSize = 0;
		while(i < data.length){
			if(data[i] != null){
				winSize++;
			}
		}
		this.windowSize = winSize;
	}
	public void close(){
		windowSize = -33;
	}
}
