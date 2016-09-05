package server;

import java.io.IOException;

public class ServerTester {
	public static void main(String args[]) throws IOException{

		if(args.length == 0){
			System.out.println("Usage: java server.ServerTester [port number]" );
		}
		else if(args.length == 1){
			try{
				int port = Integer.parseInt(args[0]);
				Server myServer = new Server(port);
			}catch (NumberFormatException e){
				System.out.println("Usage: java server.ServerTester [port number]" );	
			}
		}
		else{
			System.out.println("Usage: java server.ServerTester [port number]" );
		}
	}
}
