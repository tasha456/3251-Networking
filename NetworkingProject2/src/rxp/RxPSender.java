package rxp;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 * This class runs on a seperate thread to handle outgoing RxP Packets
 *
 */
public class RxPSender{

	InetAddress dest;
	boolean pseudoPacket;
	int portNumber;
	int windowSize;
	LinkedList<byte[]> list;
	LinkedList<Packet> packetList;
	LinkedList<Integer> timer;
	int PACKET_LENGTH=9000;
	int TIMEOUT = 1000;
	long sequenceNumber;
	long sentSequenceNumber;
	RxPParent parent;
	private final Lock lock = new ReentrantLock();
	public static final int CONTROL_SEQUENCE_NUMBER = 0;

	public RxPSender(long sequenceNumber,int windowSize,InetAddress dest, int portNumber, RxPParent parent){
		//this.data=new byte[numOfBytes];
		this.windowSize = windowSize;
		this.dest=dest;
		this.sequenceNumber = sequenceNumber;
		this.sentSequenceNumber = sequenceNumber;
		this.portNumber=portNumber;
		Random rand=new Random();
		rand.nextLong();
		this.parent = parent;
		this.packetList = new LinkedList<Packet>();
		this.timer = new LinkedList<Integer>();
		this.pseudoPacket = false;
	}
	/**
	 * 
	 * @param deltaT number of miliseconds since the last update
	 */
	public void update(int deltaT){
		lock.lock();
		sendPseudoPacket();
		sendUnsentPackets();
		int index = 0;
		while(index < timer.size()){
			int temp = timer.get(index) + deltaT;
			if(temp >= TIMEOUT){
				timer.set(index, 0);
				if(packetList.get(index).getHasSent()){
					try {
						parent.sendPacket(packetList.get(index));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else{
				timer.set(index, temp);
			}
			index++;
		}
		lock.unlock();
	}
	private void sendUnsentPackets(){
		int index = 0;
		while(index < packetList.size()){
			Packet packet = packetList.get(index);
			if(packet.getHasSent() == false){
				if(windowSize > 0){ //put a lock for window size in update and send (2 seperate threads)
					windowSize = windowSize - packet.getData().length;
					packet.send();
					timer.set(index, 0);
					try {
						if(packet.getData() != null){
							sentSequenceNumber = Math.max(sentSequenceNumber, 
									packet.getSequenceNumber() + packet.getData().length);
						} else{
							sentSequenceNumber = Math.max(sentSequenceNumber, packet.getSequenceNumber());
						}
						parent.sendPacket(packetList.get(index));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			index++;
		}
	}
	private void sendPseudoPacket(){
		if(windowSize == 0 && pseudoPacket == false){
			Packet packet = new Packet(sequenceNumber,false,false,false,0,dest,portNumber,null);
			packet.send();
			packetList.add(packet);
			timer.add(0);
			pseudoPacket = true;
		}
	}
	public void send(byte[] toSend){
		lock.lock();
		sendPseudoPacket();
		sendUnsentPackets();
		int index= 0;
		while(index < toSend.length){
			int length = PACKET_LENGTH;
			if(length+index >= toSend.length){
				length = toSend.length - index;
			}
			byte[] temp = new byte[length];
			System.arraycopy(toSend, index, temp, 0, length);
			Packet packet = new Packet(sequenceNumber, false, false, false, 0, dest, portNumber, temp);
			sequenceNumber += temp.length;
			packetList.add(packet);
			timer.add(0);
			if(windowSize > 0){
				try {
					parent.sendPacket(packet);
					packet.send();
					windowSize = windowSize - packet.getData().length;
					if(packet.getData() != null){
						sentSequenceNumber = Math.max(sentSequenceNumber, 
								packet.getSequenceNumber() + packet.getData().length);
					} else{
						sentSequenceNumber = Math.max(sentSequenceNumber, packet.getSequenceNumber());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else{
			}
			index+=length;
		}
		lock.unlock();
	}
	public void acknowledge(long synNumber,int packetWindow){
		lock.lock();
		int index = 0;
		while(index < packetList.size()){
			if(packetList.get(index).getData() != null){
				if(packetList.get(index).getSequenceNumber() == synNumber - packetList.get(index).getData().length){
					packetList.remove(index);
					timer.remove(index);
					break;
				}
			} else{
				if(packetList.get(index).getSequenceNumber() == synNumber){
					packetList.remove(index);
					timer.remove(index);
					pseudoPacket = false;
					break;
				}
			}
			index++;
		}
		this.windowSize = packetWindow - (int)(sentSequenceNumber - synNumber);
		lock.unlock();
	}
	public void sendClose(){
		lock.lock();
		Packet packet = new Packet(sequenceNumber, false, true, false, 0, dest, portNumber, new byte[1]);
		packetList.add(packet);
		timer.add(0);
		try {
			parent.sendPacket(packet);
			packet.send();
		} catch (IOException e) {
			e.printStackTrace();
		}
		lock.unlock();
	}
	public void close(){
		timer = null;
		packetList = null;
	}
//	@Override
//	public void run(){
//		while(true){
//			if(list.size()==0){
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//			}
//
//			int dataSize=0;
//			byte[] dataToSend=new byte[PACKET_LENGTH];
//			byte[] currPacket=list.pop();
//			int currSize=currPacket.length;
//			dataSize=currSize;
//			if(dataSize!=PACKET_LENGTH){
//
//				if(dataSize>PACKET_LENGTH){
//					byte[] newPacket=new byte[currSize-300];
//					System.arraycopy(currPacket,0, dataToSend, 0,300);
//					System.arraycopy(currPacket,299,newPacket, 0,currSize-300);
//					list.addFirst(newPacket);
//					dataSize=300;
//				}
//
//
//				while(dataSize<PACKET_LENGTH){
//					System.arraycopy(currPacket,0,dataToSend,0,dataSize);
//					currPacket=list.pop();
//					currSize=currPacket.length;
//					int totalSize =currSize+dataSize;
//					if(totalSize==PACKET_LENGTH){
//						System.arraycopy(currPacket,0,dataToSend,dataSize,currSize);
//						dataSize=300;
//					}else if(totalSize<PACKET_LENGTH){
//						System.arraycopy(currPacket,0,dataToSend,dataSize,currSize);
//						dataSize=totalSize;
//					}else{
//						byte[] newPack=new byte[currSize-300];
//						int left=300-dataSize;
//						System.arraycopy(currPacket,0,dataToSend,dataSize,left);
//						System.arraycopy(currPacket,299,newPack,0,currSize-300);		
//						list.addFirst(newPack);	
//						dataSize=300;	
//					}
//
//				}
//			}
//			Packet pack=new Packet(sequenceNumber, false, false, false, windowSize,dest, portNumber,dataToSend);
//
//		}
//
//	}
//

}
