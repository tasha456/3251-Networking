package rxp;
import java.net.*;
import java.util.*;
/**
 * This class runs on a seperate thread to handle outgoing RxP Packets
 *
 */
public class RxPSender implements Runnable {

	InetAddress dest;
	int portNumber;
	int windowSize;
	LinkedList<byte[]> list;
	int packetLength=300;
	long sequenceNumber;

	public RxPSender(InetAddress dest, int portNumber){
		//this.data=new byte[numOfBytes];
		this.dest=dest;
		this.portNumber=portNumber;
		list= new LinkedList<byte[]>();
		Random rand=new Random();
		rand.nextLong();
	}

	public void send(byte[] toSend){
		list.add(toSend);
	}

	@Override
	public void run(){
		while(true){
			if(list.size()==0){
				try{
					Thread.sleep(500);
				}catch(InterupptedException){

				}

			}

			int dataSize=0;
			byte[] dataToSend=new byte[packetLength];
			byte[] currPacket=list.pop();
			int currSize=currPacket.size();
			dataSize=currSize;
			if(dataSize!=packetLength){

				if(dataSize>packetLength){
					byte[] newPacket=byte[currSize-300];
					System.arraycopy(currPacket,currPacket[0], dataToSend, dataToSend[0],300);
					System.arraycopy(currPacket,currPacket[299],newPacket, newPacket[0],currSize-300);
					list.addFirst(newPacket);
					dataSize=300;
				}


				while(dataSize<packetLength){
					System.arraycopy(currPacket,currPacket[0],dataToSend,dataToSend[0],dataSize);
					currPacket=list.pop();
					currSize=currPacket.size();
					if(currSize+dataSize=packetLength){
						System.arraycopy(currPacket,currPacket[0],dataToSend,dataToSend[dataSize],currSize);
						dataSize=300;
					}else if(currSize+dataSize<packetLength){
						System.arraycopy(currPacket,currPacket[0],dataToSend,dataToSend[dataSize],currSize);
						dataSize=dataSize+currSize;
					}else(currSize+dataSize>packet){
						byte newPack=byte[currSize-300];
						int left=300-datasize;
						System.arraycopy(currPacket,currPacket[0],dataToSend,dataTosend[dataSize],left);
						System.arraycopy(currPacket,currPacket[299],newPacket[0],currSize-300);		
						list.addFirst(newPacket);	
						dataSize=300;	

					}

				}
			}
			Packet pack=new Packet(sequenceNumber, false, false, false, windowSize, dataToSend);

		}

	}


}
