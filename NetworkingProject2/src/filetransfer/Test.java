package filetransfer;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import rxp.*;
import rxpexceptions.ConcurrentListenException;
import rxpexceptions.InvalidStateException;
import rxpexceptions.ValidationException;

public class Test {

	public static void main(String args[]){
		System.out.println("SENDING STUFF");
		RxPSocket socket = new RxPSocket();
		try {
			InetAddress address = InetAddress.getByName("localhost");
			socket.connect(9999,address,9998, 10000);
			System.out.println("Stopped blocking");
		} catch (ValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConcurrentListenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
