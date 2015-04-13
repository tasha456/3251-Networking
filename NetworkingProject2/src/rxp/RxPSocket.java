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

import rxpexceptions.*;

public class RxPSocket {
	
	public static int MAXIMUM_PACKET_SIZE = 9000;
	public static final int TIMED_WAIT_DEFAULT = 10000;
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
	private int startingWindowSize;
	private int startingConnectionWindowSize;
	private int portNumber;
	private int timedWaitTimer;
	private boolean canClose;
	
	
	public RxPSocket(){
		this.connectionEstablished = false;
		this.state = State.CLOSED;
		this.packetList = new LinkedList<Packet>();
		this.canClose = true;
		timedWaitTimer = 0;
	}
	public void update(int deltaT){
		if(sender != null)
			sender.update(deltaT);
		if(receiver != null)
			receiver.update(deltaT);
		if(this.state == State.TIMED_WAIT){
			timedWaitTimer -=deltaT;
			if(timedWaitTimer <=0){
				parent.closeSocket(this);
				state = State.CLOSED;
				sender.close();
				receiver.close();
				sender = null;
				receiver = null;
				connectionPort = 0;
				connectionAddress = null;
				connectionEstablished = false;
			}
		}
	}
	public void send(byte[] data){
		sender.send(data);
	}
	public int read(byte[] data) throws InvalidStateException{
		try{
			return receiver.readData(data);
		} catch(Exception e){
			throw new InvalidStateException();
		}
	}
	public void receivePacket(Packet packet){
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
			packetList.add(packet);
			break;
		case CHAL_CHCK:
			packetList.add(packet);
			break;
		case SYN_SENT2:
			packetList.add(packet);

			break;
		case ESTABLISHED:
			if(packet.getFinFlag()){
				state = State.CLOSED_WAIT;
				Packet sendPacket = new Packet(packet.getSequenceNumber()+1, true, false, false, 
						connectionPort, connectionAddress, connectionPort, null);
				try {
					parent.sendPacket(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(canClose){
					sender.sendClose();
					state = State.LAST_ACK;
				}
			}
			else{
				this.receiver.receivePacket(packet);
			}
			break;
		case CLOSED_WAIT:
			if(packet.getFinFlag()){ //repeat from established
				Packet sendPacket = new Packet(packet.getSequenceNumber()+1, true, false, false, 
						connectionPort, connectionAddress, connectionPort, null);
				try {
					parent.sendPacket(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//this section is handled by the ready to close methods.
			break;
		case LAST_ACK:
			if(packet.getFinFlag()){
				Packet sendPacket = new Packet(packet.getSequenceNumber()+1, true, false, false, 
						connectionPort, connectionAddress, connectionPort, null);
				try {
					parent.sendPacket(sendPacket);
					sender.sendClose();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(canClose){
					sender.sendClose();
				}
			}
			
			if(packet.getAckFlag()){
				parent.closeSocket(this);
				state = State.CLOSED;
				sender.close();
				receiver.close();
				sender = null;
				receiver = null;
				connectionEstablished = false;
				connectionPort = 0;
				connectionAddress = null;
			}
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
			if(packet.getAckFlag()){
				state = State.FIN_WAIT2;
				receiver.receivePacket(packet);
			}
			break;
		case FIN_WAIT2:
			if(packet.getFinFlag()){
				ackNumber = packet.getSequenceNumber();
				Packet sendPacket = new Packet(ackNumber, true, false, false, 0, connectionAddress, connectionPort, null);
				try {
					parent.sendPacket(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				timedWaitTimer = TIMED_WAIT_DEFAULT;
				state = State.TIMED_WAIT;
			} else if(packet.getAckFlag()){
				receiver.receivePacket(packet);
			}
			break;
		case TIMED_WAIT: // timer stuff is done in update
			if(packet.getFinFlag()){
				Packet sendPacket = new Packet(ackNumber, true, false, false, 0, connectionAddress, connectionPort, null);
				try {
					parent.sendPacket(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			break;
		case CLOSING:
			break;
		default:
			System.out.println("Unknown State: " + state);
			break;
		}
	}
	public void waitToClose(){
		canClose = false;
	}
	public void readyToClose(){
		canClose = true;
		if(state == State.CLOSED_WAIT){
			sender.sendClose();
			state = State.LAST_ACK;
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
		this.sender = new RxPSender(this.sequenceNumber,startingConnectionWindowSize,
				connectionAddress, connectionPort, parent);
		this.receiver = new RxPReceiver(this.startingWindowSize, this.ackNumber,
				connectionAddress, connectionPort, parent,this.sender);
	}
	public void connect(int portNumber,InetAddress connectionAddress,int destinationPort,int windowSize) throws ValidationException, ConcurrentListenException, InvalidStateException{
		this.connectionAddress = connectionAddress;
		this.connectionPort = destinationPort;
		this.startingWindowSize = windowSize;
		int repeatCount = 0;
		byte[] challengeAnswer = null;
		if(state != State.CLOSED){
			throw new InvalidStateException();
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
			if(repeatCount - 40 <= 0){
				repeatCount = 0;
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
					Thread.sleep(40);
					repeatCount +=1;
					continue;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Packet packet = this.packetList.pop();
			switch(this.state){
			case SYN_SENT:
				this.sequenceNumber++;
				if(packet.getAddress().equals(this.connectionAddress)){
					byte[] challenge = packet.getData();		
					startingConnectionWindowSize = packet.getWindowSize();
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
							state=State.SYN_WAIT;
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
					if(repeatCount -40 <= 0){
						Packet sendPacket = new Packet(this.sequenceNumber, 
								false, false, false, 0, connectionAddress,
								connectionPort, challengeAnswer);
						parent.sendPacket(sendPacket);
						repeatCount = 0;
					}
					if(packet.getAddress().equals(this.connectionAddress)){
						if(packet.getSynFlag()){
							this.ackNumber = packet.getSequenceNumber() + 1;
							Packet sendPacket = new Packet(this.ackNumber,
									true, false, false, 0, connectionAddress,
									connectionPort,null);
							parent.sendPacket(sendPacket);
							state = State.ESTABLISHED;
							establishedSetup();
							parent.addConnectedSocket(this);
							return;
						} else if(packet.getFinFlag()){
							state = State.CLOSED;
							throw new ValidationException();
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
	public void listen(int portNumber,int windowSize) throws IOException, ConcurrentListenException, InvalidStateException{

		if(connectionEstablished == true){
			throw new InvalidStateException();
			
		}
		this.portNumber = portNumber;
		this.startingWindowSize = windowSize;
		int repeatCount = 0;
		Random rand = new Random();
		byte[] challenge = new byte[20];
		byte[] challengeAns=null;
		byte[] challengeAnswer=null;
		rand.nextBytes(challenge);

		
		try{
			this.parent=RxPParent.addSocket(this, portNumber);			
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
		while(this.state != State.ESTABLISHED){
			
			switch(this.state){
			case CLOSED:

					state=State.LISTEN;
			case LISTEN:	
					//System.out.println("LISTEN");

					if(packetList.size()>0){
						Packet packet=this.packetList.pop();
						if(packet.getSynFlag()&&!packet.getFinFlag()&&!packet.getAckFlag()){
							connectionAddress=packet.getAddress();
							connectionPort=packet.getPort();
							startingConnectionWindowSize = packet.getWindowSize();
							this.ackNumber=packet.getSequenceNumber()+1;
							Packet sendPacket=new Packet(ackNumber,true,false,false,windowSize,connectionAddress,connectionPort,challenge);
							parent.sendPacket(sendPacket);
							state=State.SYN_RCVD;
						}
					}
					break;
				
			case SYN_RCVD:

				try{
					if(repeatCount %400==0){
						state=State.LISTEN;
						break;
					}
					if(repeatCount %40 == 0){
						Packet sendPacket = new Packet(this.ackNumber, 
								true, false, false, 0, connectionAddress,
								connectionPort, challenge);
						parent.sendPacket(sendPacket);
					}
					if(packetList.size()>0){
						Packet packet1 = this.packetList.pop();
						challengeAnswer=packet1.getData();
						//System.out.println(challengeAnswer);

						if(packet1.getAddress().equals(connectionAddress)&& challengeAnswer != null &&
								challengeAnswer.length > 0){
							MessageDigest md = MessageDigest.getInstance("MD5");
							challengeAns = md.digest(challenge);

							//state=State.CHAL_CHCK;
							//System.out.println(challengeAns);
							//System.out.println(challengeAns.equals(challengeAnswer));
							boolean notEqual=false;
							for(int i=0;i<challengeAns.length;i++){
								if(challengeAns[i]!=challengeAnswer[i]){
									notEqual=true;
								}
							}
							if(!notEqual){
								Packet sendPacket=new Packet(this.sequenceNumber,false,false,true, windowSize, packet1.getAddress(), packet1.getPort(), null);
								parent.sendPacket(sendPacket);  
								state=State.SYN_SENT2;
							}else{
								Packet sendPacket=new Packet(this.sequenceNumber,false,true,false,windowSize,packet1.getAddress(), packet1.getPort(),null);
								parent.sendPacket(sendPacket);
								state=State.LISTEN;
							}
						}
					}   
				}catch(IOException e){
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					throw new IOException("Failed to find MD5 algorithm");
				}
				break;
			/*case CHAL_CHCK:
				System.out.print("CHAL_CHCK");

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
				break;
			*/
			case SYN_SENT2:
				try{
					if(repeatCount %400==0){
						state=State.LISTEN;
						break;
					}
					if(repeatCount %40 == 0){
						Packet sendPacket = new Packet(this.sequenceNumber, 
								false, false, true, 0, connectionAddress,
								connectionPort, null);
						parent.sendPacket(sendPacket);
					}
					if(packetList.size()>0){
						Packet pack=this.packetList.pop();
						if(pack.getAddress().equals(connectionAddress)){
							if(pack.getAckFlag()&&!pack.getFinFlag()&&!pack.getSynFlag()){
								connectionEstablished=true;
								state=State.ESTABLISHED;
								this.sequenceNumber = this.sequenceNumber + 1;
								establishedSetup();
								parent.addConnectedSocket(this);
							}
						}
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
				break;
			}
			repeatCount +=1;
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
			
	}
	public void close(){
		sender.sendClose();
		state = State.FIN_WAIT1;
	}
	public boolean isInUse(){
		return state != State.CLOSED;
	}
	public boolean isConnected(){
		return state == State.ESTABLISHED;
	}
	public void setWindowSize(int size){
		startingWindowSize = size*MAXIMUM_PACKET_SIZE;
		if(receiver != null){
			receiver.setMaximumWindowSize(startingWindowSize);
		}
	}
	
	
		
		
		
	
}

