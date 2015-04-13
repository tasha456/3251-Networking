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
		Socket socket = new Socket();
		try {
			socket.listen(9993, 10000);
			socket.waitToClose();
		} catch (InvalidStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(socket.isInUse()){
			Scanner scanner = new Scanner(System.in);
			String line = scanner.nextLine();
			if(line.startsWith("send ")){
				line = line.replace("send ", "");
				socket.send(line.getBytes());
			}
			else if(line.startsWith("read")){
				try {
					byte[] temp = new byte[200];
					socket.read(temp);
					System.out.println(new String(temp));
				} catch (InvalidStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(line.startsWith("end")){
				socket.close();
			} else if(line.startsWith("canClose")){
				socket.readyToClose();
			}
		}
		System.out.println("Finished");
	}
}
