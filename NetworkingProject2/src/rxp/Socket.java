package rxp;

import java.io.IOException;
import java.net.InetAddress;

import rxpexceptions.ConcurrentListenException;
import rxpexceptions.InvalidStateException;
import rxpexceptions.ValidationException;

public class Socket {
	public static final int WINDOW_SIZE = 1000;
	private RxPSocket socket;
	public Socket(){
		RxPSocket socket = new RxPSocket();
	}
	public void listen(int portNumber) throws InvalidStateException{
		listen(portNumber,WINDOW_SIZE);
	}
	public void listen(int portNumber,int windowSize) throws InvalidStateException{
		try {
			socket.listen(portNumber, windowSize);
		} catch (IOException e) {
			throw new InvalidStateException(e.getMessage());
		} catch (ConcurrentListenException e) {
			e.printStackTrace();
			throw new InvalidStateException(e.getMessage());
		}
	}
	public void connect(int portNumber,InetAddress address,int destinationPort) throws InvalidStateException{
		connect(portNumber,address,destinationPort,WINDOW_SIZE);
	}
	public void connect(int portNumber,InetAddress address,int destinationPort,int windowSize) throws InvalidStateException{
		try {
			socket.connect(portNumber, address, destinationPort, windowSize);
		} catch (ValidationException e) {
			e.printStackTrace();
			throw new InvalidStateException(e.getMessage());
			
		} catch (ConcurrentListenException e) {
			e.printStackTrace();
			throw new InvalidStateException(e.getMessage());
		}
	}
	public void send(byte[] data){
		socket.send(data);
	}
	public int read(byte[] data){
		return socket.read(data);
	}
	
}
