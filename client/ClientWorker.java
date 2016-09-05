package client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

import server.HostAddress;

public class ClientWorker implements Runnable{
	private HostAddress listeningServer = null;
	private Scanner userScanner = null;
	private Socket messageSocket; 
	boolean run;
	private HashMap<String, HostAddress> privateUserHash;
	private String username;


	public ClientWorker(HostAddress listeningServer, HashMap<String, HostAddress> privateUsers, String username){
		this.listeningServer = listeningServer;
		userScanner = new Scanner(System.in);
		this.privateUserHash = privateUsers;
		this.username = username;	
	}
	
//	public ClientWorker(HostAddress listeningServer, String username){
//		this.listeningServer = listeningServer;
//		userScanner = new Scanner(System.in);
//		this.username = username;
//	}

	public void run(){
		run = true;
		while(run){
			if(userScanner.hasNextLine()){
				String command = userScanner.nextLine();

				if(command.startsWith("private ")){
					sendPrivateMessage(command);
				}
//				else if(command.equals("y") || command.equals("n")){
//					System.out.println("command: " + command);
//					sendMessageToServer(command + " :" + username);
//					userScanner.close();
//					run = false;
//				}
				else{
					sendMessageToServer(command);
				}
			}
		}
	}
	
	//send message to server
	private synchronized void sendMessageToServer(String message){
		try {
			//make new connection to server
			messageSocket = new Socket(listeningServer.getIPAddress(), listeningServer.getPort());			
			DataOutputStream outStream = new DataOutputStream(messageSocket.getOutputStream());
			outStream.writeBytes(message+"\r");
			messageSocket.close();
			if(message.equals("logout")){
				run = false;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}catch (ConnectException c){
			run = false;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void sendPrivateMessage(String message){
		String[] messageArr = message.split(" ");
		String recipient = messageArr[1];
		try {
			HostAddress recipientHostAddress = privateUserHash.get(recipient);
			if(recipientHostAddress == null){
				System.out.println("Private messaging for " + recipient + " not yet enabled.");
			}
			else{
				//make new connection to server
				messageSocket = new Socket(recipientHostAddress.getIPAddress(), recipientHostAddress.getPort());			
				DataOutputStream outStream = new DataOutputStream(messageSocket.getOutputStream());
				String messageToUser = formatPrivateMessage(messageArr);
				outStream.writeBytes(messageToUser+"\r");
				messageSocket.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}catch (ConnectException c){
			System.out.println("Cannot send private message to " + recipient + 
					". User is no longer online. Please send a regular message "
					+ "and it will be sent when the user logs in again.");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//format message to <senderUsername: message>
	private String formatPrivateMessage(String[] messageArr){
		String messageToUser = "[private] " + username + ": ";
		for (int i = 2; i<messageArr.length; i++){
			messageToUser += messageArr[i] + " ";
		}
		return messageToUser;
	}
}
