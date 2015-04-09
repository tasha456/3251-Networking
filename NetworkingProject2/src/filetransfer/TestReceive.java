package filetransfer;

import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

import rxp.*;
import rxpexceptions.ConcurrentListenException;

public class TestReceive {

	public static void main(String[] args){
		System.out.println("RECEIVING STUFF");
		RxPSocket socket = new RxPSocket();
		try {
			socket.listen(9998, 10000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConcurrentListenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Stop blocking");
	}
}
