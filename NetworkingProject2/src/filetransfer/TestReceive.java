package filetransfer;

import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

import rxp.*;
import rxpexceptions.ConcurrentListenException;
import rxpexceptions.InvalidStateException;

public class TestReceive {

	public static void main(String[] args){
		System.out.println("RECEIVING STUFF");
		RxPSocket socket = new RxPSocket();
		try {
			socket.listen(9993, 10000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConcurrentListenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println("Ready");
//		byte[] temp = new byte[2];
//		while(true){
//			int num = socket.read(temp);
//			System.out.println("Read " + num + " bytes: " + new String(temp));
//			try {
//				Thread.sleep(4000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
}
