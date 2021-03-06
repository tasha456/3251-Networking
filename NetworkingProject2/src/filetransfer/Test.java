package filetransfer;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;

import rxp.*;
import rxpexceptions.ConcurrentListenException;
import rxpexceptions.InvalidStateException;
import rxpexceptions.ValidationException;

public class Test {

	public static void main(String args[]){
		System.out.println("SENDING STUFF");
		Socket socket = new Socket();
		try {
			InetAddress address = InetAddress.getByName("localhost");
			socket.connect(9994,address,9993, 10000);
		} catch (InvalidStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("-----------Ready--------------");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
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
				byte[] temp = new byte[200];
				try {
					socket.read(temp);
					System.out.println(new String(temp));
				} catch (InvalidStateException e) {
					
				}
			}
			else if(line.startsWith("end")){
				socket.close();
			}
		}
	}
}
