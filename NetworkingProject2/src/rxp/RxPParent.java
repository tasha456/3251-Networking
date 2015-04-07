package rxp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;

import rxp.*;

public class RxPParent implements Runnable{

	private HashMap<String,RxPSocket> rxpSocket;
	private static HashMap<Integer,RxPParent> parentSocket;
	private RxPSocket listeningSocket = null;
	private int portNumber;
	private boolean active;
	private DatagramSocket socket;
	private RxPParent(int portNumber) throws SocketException{
		this.portNumber = portNumber;
		this.socket = new DatagramSocket(portNumber);
	}
	public void run(){
		active = true;
		while(active){
			try {
				System.out.println("Parent is listening");
				byte[] data = new byte[RxPSocket.MAXIMUM_PACKET_SIZE];
				DatagramPacket rawPacket = new DatagramPacket(data,RxPSocket.MAXIMUM_PACKET_SIZE);
				socket.receive(rawPacket);
				System.out.println("Packet received");
				InetAddress sourceAddress = rawPacket.getAddress();
				int portNumber = rawPacket.getPort();
				Packet packet = new Packet(rawPacket.getData(),sourceAddress,portNumber);
				if(packet.getIsCorrupted() == false){
					receivePacket(packet);
				}
			}  catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public boolean getIsActive(){
		return this.active;
	}
	public boolean addConnectedSocket(RxPSocket socket){
		if(rxpSocket.containsKey(createKey(socket)) == false){
			if(listeningSocket.equals(socket)){
				rxpSocket.put(createKey(socket), socket);
				listeningSocket = null;
				return true;
			}
			return false;
		}
		return true;
	}
	public void sendPacket(Packet packet) throws IOException{
		byte[] data = packet.getRawBytes();
		DatagramPacket datagram = new DatagramPacket(data,data.length,
				packet.getAddress(),packet.getPort());
		this.socket.send(datagram);
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void receivePacket(Packet packet){
		String address = createKey(packet);
		if(rxpSocket.containsKey(address)){
			RxPSocket socket = rxpSocket.get(address);
			socket.receivePacket(packet);
		} else{
			if(listeningSocket != null){
				listeningSocket.receivePacket(packet);
			}
		}
	}
	private void addRxPSocket(RxPSocket socket){
		if(listeningSocket == null){
			this.listeningSocket = socket;
		} else{
			//throw error because trying to establish multiple connections on the
			//same socket, at the same time instead of iteratively
		}
	}
	private String createKey(Packet packet){
		return createKey(packet.getAddress(),packet.getPort());
	}
	private String createKey(RxPSocket socket){
		return createKey(socket.getDestinationAddress(),socket.getDestinationPort());
	}
	private String createKey(InetAddress address,int socket){
		return address.toString() + socket;
	}
	public static RxPParent addSocket(RxPSocket socket,int portNumber) throws SocketException{
		if(parentSocket == null){
			parentSocket = new HashMap<Integer,RxPParent>();
		}
		if(parentSocket.containsKey(portNumber) == false){
			parentSocket.put(portNumber, new RxPParent(portNumber));
		}
		RxPParent parent = parentSocket.get(portNumber);
		parent.addRxPSocket(socket); 
		//start new thread for parent
		if(parent.getIsActive() == false){
			(new Thread(parent)).start();
		}
		return parent;
	}
}
