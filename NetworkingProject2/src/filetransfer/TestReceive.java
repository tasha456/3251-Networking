package filetransfer;

import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

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
		while(true){
			Scanner scanner = new Scanner(System.in);
			String line = scanner.nextLine();
			if(line.startsWith("send ")){
				line = line.replace("send ", "");
				socket.send(line.getBytes());
			}
			else if(line.startsWith("read")){
				byte[] temp = new byte[200];
				socket.read(temp);
				System.out.println(new String(temp));
			}
		}
	}
}
