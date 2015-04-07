package filetransfer;

import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

import rxp.*;

public class TestReceive {

	public static void main(String[] args) throws NoSuchAlgorithmException, IOException{
		System.out.println("RECEIVING STUFF");
		try{
			RxPSocket socket = new RxPSocket();
			socket.listen(9998, 10000);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
