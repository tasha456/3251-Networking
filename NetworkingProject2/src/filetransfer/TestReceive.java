package filetransfer;

import java.net.SocketException;

import rxp.*;

public class TestReceive {

	public static void main(String[] args){
		try{
			RxPSocket socket = new RxPSocket();
			socket.listen(9998, 10000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
