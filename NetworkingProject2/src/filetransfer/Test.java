package filetransfer;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import rxp.*;

public class Test {

	public static void main(String args[]){
		System.out.println("SENDING STUFF");
		RxPSocket socket = new RxPSocket();
		try {
			InetAddress address = InetAddress.getByName("localhost");
			socket.connect(9999,address,9998, 10000);
		} catch(UnknownHostException e){
			e.printStackTrace();
		}
	}
}
