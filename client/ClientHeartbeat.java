package client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.TimerTask;

import server.HostAddress;

public class ClientHeartbeat extends TimerTask{
	private Socket messageSocket; 
	private HostAddress listeningServer = null;
	private String username;
	
	public ClientHeartbeat(HostAddress heartbeatListenerSocket, String username){
		listeningServer = heartbeatListenerSocket;
		this.username = username;
	}

	public void run(){
		sendMessageToServer("alive from " + username);
	}

	//send message to server
	private synchronized void sendMessageToServer(String message){
		try {
			//make new connection to server
			messageSocket = new Socket(listeningServer.getIPAddress(), listeningServer.getPort());			
			DataOutputStream outStream = new DataOutputStream(messageSocket.getOutputStream());
			outStream.writeBytes(message+"\r");
			messageSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (ConnectException c){
			System.out.println("The server is no longer available.");
			System.exit(0);
		}catch (IOException e) {
		
			e.printStackTrace();
		}
	}
}
