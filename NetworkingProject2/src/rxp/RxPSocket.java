package rxp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class RxPSocket {
	
	public static final int MAXIMUM_PACKET_SIZE = 300;
	private enum State{CLOSED,LISTEN,SYN_RCVD,CHAL_CHCK,SYN_SENT2,ESTABLISHED,
						CLOSED_WAIT,LAST_ACK,SYN_SENT,CHALLENGE,SYN_WAIT,
						FIN_WAIT1,FIN_WAIT2,TIMED_WAIT,CLOSING};
	private int destinationPort;
	private State state;
	private InetAddress address;
	private RxPParent parent;
	boolean connectionEstablished;
	
	
	public RxPSocket(){
		this.connectionEstablished = false;
		state = State.CLOSED;
	}
	public void receivePacket(Packet packet){
		switch(state){
		case CLOSED:
			//do nothing. you shouldn't be getting packets in the closed state anyways
			break;
		case LISTEN:
			if(packet.getSynFlag() && !packet.getAckFlag() && !packet.getFinFlag()){
				System.out.println("Received syn request");
				
			}
			break;
		case SYN_RCVD:
			break;
		case CHAL_CHCK:
			break;
		case SYN_SENT2:
			break;
		case ESTABLISHED:
			break;
		case CLOSED_WAIT:
			break;
		case LAST_ACK:
			break;
		case SYN_SENT:
			break;
		case CHALLENGE:
			break;
		case SYN_WAIT:
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
		return this.address;
	}
	public int getDestinationPort(){
		return this.destinationPort;
	}
	public void listen(int portNumber,int windowSize) throws SocketException{
		if(connectionEstablished == true){
			//Throw an exception
		}
		this.parent = RxPParent.addSocket(this,portNumber);
	}
}

