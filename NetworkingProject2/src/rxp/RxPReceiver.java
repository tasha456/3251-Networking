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

	Byte[] data;
	int readIndex; //last index it read
	int receiveIndex; //next index to write to
	long indexOffset;
	InetAddress address;
	RxPParent parent;
	RxPSender sender;
	int port;
	int windowSize;
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
		this.address = address;
		this.port = port;
		this.timeDifference = 0;
		this.windowSize = data.length;
		readIndex = -1;
		receiveIndex = 0;
	}
	/**
	 * takes in a packet, and if there is space in the buffer it will store the data inside the packet.
	 * If it does not have room to store the data, it will fail silently.
	 * @param packet the Packet object who's data field we want to store.
	 */
	public void receivePacket(Packet packet){
		
		lock.lock();
		System.out.println("received " + packet.getSequenceNumber());
		if(packet.getAckFlag()){
			sender.acknowledge(packet.getSequenceNumber(),packet.getWindowSize());
		} else if(indexOffset <= packet.getSequenceNumber() && 
					indexOffset + data.length > packet.getSequenceNumber()){
			if(packet.getData() != null){
				boolean result = copyData(packet.getData(),packet.getSequenceNumber());
				System.out.println(windowSize);
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
				System.out.println("Sequence Number: " + packet.getSequenceNumber());
				System.out.println("range: " + indexOffset + " - " + (indexOffset + data.length));
				long ackNumber = packet.getSequenceNumber() + packet.getData().length;
				Packet sendPacket = new Packet(ackNumber, true, false, false, windowSize, 
						address,port,null);
				try {
					parent.sendPacket(sendPacket);
					System.out.println("SENT ACK");
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
				System.out.println("Sequence Number: " + packet.getSequenceNumber());
				System.out.println("range: " + indexOffset + " - " + (indexOffset + data.length));
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
		if(originalBufferIndex + data.length > this.data.length){
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
		if(windowSize < 0)
			recalculateWindowSize();
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
}
