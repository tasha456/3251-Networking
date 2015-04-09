package rxp;
import java.io.IOException;
import java.net.*;
import java.util.*;
/**
 * This class runs on a seperate thread to handle outgoing RxP Packets
 *
 */
public class RxPSender{

	InetAddress dest;
	int portNumber;
	int windowSize;
	LinkedList<byte[]> list;
	LinkedList<Packet> packetList;
	LinkedList<Integer> timer;
	int PACKET_LENGTH=300;
	int TIMEOUT = 10000;
	long sequenceNumber;
	RxPParent parent;

	public RxPSender(long sequenceNumber,InetAddress dest, int portNumber, RxPParent parent){
		//this.data=new byte[numOfBytes];
		this.dest=dest;
		this.sequenceNumber = sequenceNumber;
		this.portNumber=portNumber;
		list= new LinkedList<byte[]>();
		Random rand=new Random();
		rand.nextLong();
		this.parent = parent;
		this.packetList = new LinkedList<Packet>();
		this.timer = new LinkedList<Integer>();
	}
	/**
	 * 
	 * @param deltaT number of miliseconds since the last update
	 */
	public void update(int deltaT){
		int index = 0;
		while(index < timer.size()){
			int temp = timer.get(index) + deltaT;
			if(temp >= TIMEOUT){
				timer.set(index, 0);
				try {
					parent.sendPacket(packetList.get(index));
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else{
				timer.set(index, temp);
			}
			index++;
		}
	}
	public void send(byte[] toSend){
		int index= 0;
		while(index < toSend.length){
			int length = PACKET_LENGTH;
			if(length+index >= toSend.length){
				length = toSend.length - index;
			}
			byte[] temp = new byte[length];
			System.arraycopy(toSend, index, temp, 0, length);
			Packet packet = new Packet(sequenceNumber, false, false, false, windowSize, dest, portNumber, temp);
			sequenceNumber += temp.length;
			packetList.add(packet);
			timer.add(0);
			try {
				parent.sendPacket(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			index+=length;
		}
		list.add(toSend);
	}
	public void acknowledge(long synNumber){
		int index = 0;
		while(index < packetList.size()){
			if(packetList.get(index).getSequenceNumber() == synNumber - packetList.get(index).getData().length){
				packetList.remove(index);
				timer.remove(index);
				break;
			}
			index++;
		}
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
