package rxp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Random;

public class RxPSocket {
	
	public static final int MAXIMUM_PACKET_SIZE = 300;
	private enum State{CLOSED,LISTEN,SYN_RCVD,CHAL_CHCK,SYN_SENT2,ESTABLISHED,
						CLOSED_WAIT,LAST_ACK,SYN_SENT,CHALLENGE,SYN_WAIT,
						FIN_WAIT1,FIN_WAIT2,TIMED_WAIT,CLOSING};
	private int connectionPort;
	private long sequenceNumber;
	private long ackNumber;
	private State state;
	private InetAddress connectionAddress;
	private RxPParent parent;
	private RxPReceiver receiver;
	private RxPSender sender;
	boolean connectionEstablished;
	private LinkedList<Packet> packetList;
	private int windowSize;
	private int portNumber;
	
	
	public RxPSocket(){
		this.connectionEstablished = false;
		this.state = State.CLOSED;
		this.packetList = new LinkedList<Packet>();
	}
	public void receivePacket(Packet packet){
		System.out.println("Received packet:");
		System.out.println(packet.toString());
		if(packet.getIsCorrupted()){
			return; //treat corrupted packets as lost packets
		}
		switch(state){
		case CLOSED:
			//do nothing. you shouldn't be getting packets in the closed state anyways
			break;
		case LISTEN:
			packetList.add(packet);
			break;
		case SYN_RCVD:
			break;
		case CHAL_CHCK:
			packetList.add(packet);
			break;
		case SYN_SENT2:
			break;
		case ESTABLISHED:
			this.receiver.receivePacket(packet);
			break;
		case CLOSED_WAIT:
			break;
		case LAST_ACK:
			break;
		case SYN_SENT:
			packetList.add(packet);
			break;
		case CHALLENGE:
			//done in syn_sent.  they shoulda been the same state.  oops :)
			break;
		case SYN_WAIT:
			packetList.add(packet);
			break;
		case FIN_WAIT1:
			break;
		case FIN_WAIT2:
			break;
		case TIMED_WAIT:
			break;
		case CLOSING:
			break;
		default:
			System.out.println("Unknown State: " + state);
			break;
		}
	}
	public InetAddress getDestinationAddress(){
		return this.connectionAddress;
	}
	public int getDestinationPort(){
		return this.connectionPort;
	}
	/**
	 * This method MUST be called before entering into the established state.
	 */
	public void establishedSetup(){
		this.sender = new RxPSender(connectionAddress, connectionPort);
		this.receiver = new RxPReceiver(500, connectionAddress, connectionPort, parent);
	}
	public void connect(int portNumber,InetAddress connectionAddress,int destinationPort,int windowSize){
		this.connectionAddress = connectionAddress;
		this.connectionPort = destinationPort;
		int repeatCount = 0;
		byte[] challengeAnswer = null;
		if(state != State.CLOSED){
			return; //can't connect if you're not in closed. throw some exception
		} else{
			try {
				this.parent = RxPParent.addSocket(this, portNumber);
				Packet packet = new Packet(this.sequenceNumber,false,false,true,
						windowSize,connectionAddress,connectionPort,null);
				parent.sendPacket(packet);
				state = State.SYN_SENT;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		while(this.state != State.ESTABLISHED){
			if(repeatCount %40 == 0){
				switch(this.state){
					case SYN_SENT:
						try {
							Packet packet = new Packet(this.sequenceNumber,
									false,false,true,windowSize,connectionAddress,
									connectionPort,null);
							parent.sendPacket(packet);
						} catch (IOException e) {
							e.printStackTrace();
						}
						break;
					default:
							//do absolutely nothing.
						break;
				}
			}
			if(this.packetList.size() == 0){
				try {
					Thread.sleep(100);
					repeatCount +=1;
					continue;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Packet packet = this.packetList.pop();
			switch(this.state){
			case SYN_SENT:
				System.out.println("SYN_SENT");
				if(packet.getAddress().equals(this.connectionAddress)){
					byte[] challenge = packet.getData();
					if(packet.getAckFlag() == true && challenge != null &&
							challenge.length > 0){
						//send challenge response
						try {
							MessageDigest md = MessageDigest.getInstance("MD5");
							Random rand = new Random();
							challengeAnswer = md.digest(challenge);
							Packet sendPacket = new Packet(this.sequenceNumber, 
									false, false, false, 0, connectionAddress,
									connectionPort, challengeAnswer);
							parent.sendPacket(sendPacket);
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				break;
			case CHALLENGE:
				//done in syn_sent.  they shoulda been the same state.  oops :)
				break;
			case SYN_WAIT:
				try{
					if(repeatCount %40 == 0){
						Packet sendPacket = new Packet(this.sequenceNumber, 
								false, false, false, 0, connectionAddress,
								connectionPort, challengeAnswer);
						parent.sendPacket(sendPacket);
					}
					if(packet.getAddress().equals(this.connectionAddress)){
						if(packet.getSynFlag()){
							this.ackNumber = packet.getSequenceNumber() + 1;
							Packet sendPacket = new Packet(this.ackNumber,
									true, false, false, 0, connectionAddress,
									connectionPort,null);
							parent.sendPacket(sendPacket);
							state = State.ESTABLISHED;
							return;
						} else if(packet.getFinFlag()){
							state = State.CLOSED;
							return; //TODO this is a failure to authenticate. throw exception or something
						}
					}
				} catch(IOException e){
					e.printStackTrace();
				}
				break;
			}
			repeatCount +=1;
		}
	}
	public void listen(int portNumber,int windowSize) throws NoSuchAlgorithmException, IOException{

		if(connectionEstablished == true){
			//Throw an exception
			
		}
		this.portNumber = portNumber;
		this.windowSize = windowSize;
		int repeatCount = 0;
		Random rand = new Random();
		byte[] challenge = new byte[20];
		byte[] challengeAns=null;
		byte[] challengeAnswer=null;
		InetAddress connectionAddress=null;
		rand.nextBytes(challenge);
		
		try{
			this.parent = RxPParent.addSocket(this, portNumber);
			
		} catch (IOException e) {
			e.printStackTrace();
		}	

		while(this.state != State.ESTABLISHED){
			switch(this.state){
			case LISTEN:		
					Packet packet=this.packetList.pop();
					connectionAddress=packet.getAddress();
					if(packet.getSynFlag()){
						this.ackNumber=packet.getSequenceNumber()+1;
						Packet sendPacket=new Packet(ackNumber,true,false,false,windowSize,packet.getAddress(),packet.getPort(),challenge);
						parent.sendPacket(sendPacket);
						state=State.SYN_RCVD;

					}else{
						break;
					}
				
			case SYN_RCVD:
				try{
					if(repeatCount %40 == 0){
						Packet sendPacket = new Packet(this.ackNumber, 
								true, false, false, 0, connectionAddress,
								connectionPort, challenge);
						parent.sendPacket(sendPacket);
					}
					Packet packet1 = this.packetList.pop();
					if(packet1.getAddress()==connectionAddress){
						MessageDigest md = MessageDigest.getInstance("MD5");
						challengeAns = md.digest(challenge);
						challengeAnswer=packet1.getData();
						state=State.CHAL_CHCK;
					}   
				}catch(IOException e){
					e.printStackTrace();
			}
			repeatCount +=1;
			case CHAL_CHCK:
				try {				
					if(challengeAns==challengeAnswer){
						Packet sendPacket=new Packet(this.sequenceNumber,false,false,true, windowSize, connectionAddress, portNumber, null);
						parent.sendPacket(sendPacket);  
					}else{
						Packet sendPacket=new Packet(this.sequenceNumber,false,true,false,windowSize,connectionAddress,portNumber,null);
						parent.sendPacket(sendPacket);
						state=State.LISTEN;
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
			case SYN_SENT2:
				try{
					if(repeatCount %40 == 0){
						Packet sendPacket = new Packet(this.sequenceNumber, 
								false, false, true, 0, connectionAddress,
								connectionPort, null);
						parent.sendPacket(sendPacket);
					}
					Packet pack=this.packetList.pop();
					if(pack.getAddress()==connectionAddress){
						if(pack.getAckFlag()){
							connectionEstablished=true;
							state=State.ESTABLISHED;
						}else{
							break;
						}
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
			}
			repeatCount +=1;
		}
			
	}
		
		
		
	
}

