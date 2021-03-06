package rxp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;

import rxp.*;
import rxpexceptions.ConcurrentListenException;

public class RxPParent implements Runnable{

	private HashMap<String,RxPSocket> rxpSocket;
	private static HashMap<Integer,RxPParent> parentSocket;
	private RxPSocket listeningSocket = null;
	private int portNumber;
	private int counter;
	private boolean active;
	private DatagramSocket socket;
	private LinkedList<Packet>packetList;
	private RxPParent(int portNumber) throws SocketException{
		this.portNumber = portNumber;
		this.socket = new DatagramSocket(portNumber);
		this.socket.setSoTimeout(200);
		this.counter = 0;
		rxpSocket = new HashMap<String,RxPSocket>();
		packetList = new LinkedList<Packet>();
	}
	public void run(){
		active = true;
		while(active){
			try {
				byte[] data = new byte[RxPSocket.MAXIMUM_PACKET_SIZE + 100];
				DatagramPacket rawPacket = new DatagramPacket(data,RxPSocket.MAXIMUM_PACKET_SIZE + 100);
				socket.receive(rawPacket);
				int len = rawPacket.getLength();
				byte[] actualPacket = new byte[len];
				System.arraycopy(rawPacket.getData(),0, actualPacket, 0, len);
				InetAddress sourceAddress = rawPacket.getAddress();
				int portNumber = rawPacket.getPort();
				Packet packet = new Packet(actualPacket,sourceAddress,portNumber);
				if(packet.getIsCorrupted() == false){
					receivePacket(packet);
				} else{
					//System.out.println("Corrupted packet ");
					//System.out.println(packet.toString());
				}
				sendQueuedPackets();
			}   
			catch(SocketTimeoutException e){
				sendQueuedPackets();
				
			}
			catch (IOException e) {
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
	private void sendQueuedPackets(){
		for(RxPSocket item:rxpSocket.values()){
			try {
				item.update(socket.getSoTimeout());
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		int packetLength = packetList.size();
		int i = 0;
		while(i< packetLength){
			Packet packet = packetList.pop();
			byte[] data = packet.getRawBytes();
			DatagramPacket datagram = new DatagramPacket(data,data.length,
					packet.getAddress(),packet.getPort());
			try {
				this.socket.send(datagram);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
		}
	}
	public void sendPacket(Packet packet) throws IOException{
		this.packetList.add(packet);
	}
	private void receivePacket(Packet packet){
		String address = createKey(packet);
		if(rxpSocket.containsKey(address)){
			RxPSocket socket = rxpSocket.get(address);
			socket.receivePacket(packet);
		} else{
			if(listeningSocket != null){
				listeningSocket.receivePacket(packet);
			} else{
				System.out.println("received packet for unknown socket");
			}
		}
	}
	public void closeSocket(RxPSocket socket){
		if(rxpSocket.containsKey(createKey(socket))){
			rxpSocket.remove(createKey(socket));
			if(rxpSocket.isEmpty()){
				active = false;
			}
		}
	}
	private void addRxPSocket(RxPSocket socket) throws ConcurrentListenException{
		if(listeningSocket == null){
			this.listeningSocket = socket;
		} else{
			throw new ConcurrentListenException();
		}
	}
	private String createKey(Packet packet){
		return createKey(packet.getAddress(),packet.getPort());
	}
	private String createKey(RxPSocket socket){
		return createKey(socket.getDestinationAddress(),socket.getDestinationPort());
	}
	private String createKey(InetAddress address,int socket){
		return address.toString().replace("localhost", "") + socket;
	}
	public static RxPParent addSocket(RxPSocket socket,int portNumber) throws SocketException, ConcurrentListenException{
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
