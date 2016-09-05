package client;

import java.io.*;
import java.net.*;

public class ClientTester {
	public static void main(String args[]) throws IOException{
		
		if(args.length == 0){
			System.out.println("Usage: java client.ClientTester [ip address] [port number]" );
		}
		else if(args.length == 2){
			try{
				int port = Integer.parseInt(args[1]);
				String ipAddress = args[0];
				Client myClient = new Client(ipAddress, port);
			}
			catch (NumberFormatException e){
				System.out.println("Usage: java client.ClientTester [ip address] [port number]");	
			}
		}
		else {
			System.out.println("Usage: java client.ClientTester [ip address] [port number]");
		}
	}
}
