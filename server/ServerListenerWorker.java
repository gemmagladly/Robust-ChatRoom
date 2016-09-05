package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

//thread to receive heartbeat signal from client
public class ServerListenerWorker implements Runnable{
	private ServerSocket serverListeningSocket = null;
	private Socket messageSocket;
	private HostAddress server;
	private Socket heartbeatSocket;


	public ServerListenerWorker(HostAddress server, ServerSocket listener){
		serverListeningSocket = listener;
		this.server = server;
	}

	public void run(){
		
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new heartbeatTimer(),0, 30000);
		
	}

	private class heartbeatTimer extends TimerTask{
		public void run() {
			String newMessage = "";
			try{
				serverListeningSocket.setSoTimeout(30000);
				messageSocket = serverListeningSocket.accept();
				Scanner inFromClient = new Scanner(new InputStreamReader(messageSocket.getInputStream()));
				if(inFromClient.hasNext()){
					newMessage = inFromClient.nextLine();
				}
				System.out.println(newMessage);
				messageSocket.close();
				inFromClient.close();
			}
			catch (SocketTimeoutException s){
				sendMessageToServer("logout");
			}
			catch (IOException e){
				e.printStackTrace();
			}
		}
	}

	private synchronized void sendMessageToServer(String message){
		try {
			//make new connection to server
			heartbeatSocket = new Socket(server.getIPAddress(), server.getPort());	
			DataOutputStream outStream = new DataOutputStream(heartbeatSocket.getOutputStream());
			outStream.writeBytes(message+"\r");
			heartbeatSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

}
