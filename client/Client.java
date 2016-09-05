package client;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import server.HostAddress;

public class Client {
	int listeningPort = 0;
	private Socket clientSocket = null;
	private ServerSocket listeningSocket = null;
	private String serverIpAddress = "";
	private int serverPort;
	private String username;
	private HashMap<String, HostAddress> privateUsers;

	public Client(String ipAddress, int serverPortnum){
		try {
			serverIpAddress = ipAddress;
			this.serverPort = serverPortnum;
			DataOutputStream outToServer;
			BufferedReader inFromServer;
			privateUsers = new HashMap<String, HostAddress>();
			clientSocket = new Socket(serverIpAddress, serverPort);

			//socket to listen to broadcast messages
			listeningSocket = new ServerSocket(0);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			listeningPort = listeningSocket.getLocalPort();
			boolean requestCredentials = true;


			//request credentials
			while (requestCredentials){
				String credentials;
				String loginResponse;
				try {
					credentials = requestCredentials();
					outToServer.writeBytes(credentials);
					loginResponse = inFromServer.readLine();

					if(loginResponse.equals("false")){
						requestCredentials=true;
					}
					else if (loginResponse.equals("block")){
						tooManyAttemptsMessage();
						requestCredentials=false;
					}
					else if (loginResponse.equals("enter")){
						enterChat();
						requestCredentials=false;
					}
					else if (loginResponse.equals("forbidden")){
						System.out.println("Due to multiple failures, your account "
								+ "has been blocked. Please try again later.");
						System.exit(0);
					}
					else{
						System.out.println(loginResponse);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			clientSocket.close();
		} catch (ConnectException c){
			System.out.println("Server is not responding. Please try again another time.");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String requestCredentials() throws IOException{
		System.out.println("Username:");
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		username = inFromUser.readLine();
		System.out.println("Password:"); 
		String password = inFromUser.readLine();
		String credentials = username+" "+password+"\r";
		return credentials;
	}

	private void tooManyAttemptsMessage(){
		System.out.println("You have made too many invalid attempts. Your account has been blocked. Please try again in one minute.");
		System.exit(0);
	}

	private void enterChat(){
		//send server listening port
		DataOutputStream outToServer;
		try {
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			outToServer.writeBytes(Integer.toString(listeningPort)); //send port
			clientSocket.close();//close authentication client socket

			while(true){
				Socket messageSocket = listeningSocket.accept();
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
						messageSocket.getInputStream()));
				String newMessage = inFromServer.readLine();
				messageSocket.close();

				//interpret message from server
				interpretMessage(newMessage);			
			}
		} catch (IOException e) {
			System.out.println("Cannot send listening port number or cannot accept connection on listening socket");
			e.printStackTrace();
		}
	}

	private void interpretMessage(String message){
		if(message.equals("abort")){//if logged on from new device
			System.out.println("You have been logged on from another device. This session will exit.");
			System.exit(0);
		}
		else if(message.startsWith("port:")){//set new listening port of server
			String[] messageArr = message.split(":");
			String messagePort = messageArr[1];
			//			System.out.println("Receieved port listener number: " +messagePort);
			//start worker thread to listen for user input
			HostAddress listeningServer = new HostAddress(serverIpAddress, messagePort);
			new Thread(new ClientWorker(listeningServer, privateUsers, username)).start();
		}
		else if(message.equals("logout")){
			System.out.println("Logging out...");
			System.exit(0);
		}
		else if(message.startsWith("heartbeat:")){
			String[] messageArr = message.split(":");
			String heartbeatPort = messageArr[1];
			HostAddress heartbeatListenerServer = new HostAddress(serverIpAddress, heartbeatPort);
			beginHeartbeat(heartbeatListenerServer);
		}
		else if(message.startsWith("desiredUser: ")){
			addPrivateUser(message);
		}//else if(message.startsWith("REQUEST_PORT:")){
//			privateRequest(message);
//		}
		else{
			System.out.println(message);
		}
	}

	private void beginHeartbeat(HostAddress heartbeatListener){
		TimerTask heartbeat = new ClientHeartbeat(heartbeatListener, username);
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(heartbeat, 0, 30000); //send heartbeat every 30 seconds
	}

	private void addPrivateUser(String message){
		String[] messageArr = message.split(" ");
		HostAddress privateUserAddress = new HostAddress(messageArr[2], messageArr[3]);
		String desiredUser = messageArr[1];
		System.out.println(desiredUser + ": " + privateUserAddress.toString());
		System.out.println("Private messaging to " + desiredUser + " now enabled.");
		synchronized(privateUsers){
			privateUsers.put(desiredUser, privateUserAddress);
		}
	}
	
//	private void privateRequest(String message){
//		
//		String[] messageArr = message.split(":");
//		String port = messageArr[1];
//		String user = messageArr[2];
//		System.out.println("Resquesting port: " +port);
//		HostAddress requestingAddress = new HostAddress(serverIpAddress, port);
//		
//		System.out.println("[Request] User " + user + " would like to start a private conversation."
//				+ " Do you accept this invitation? (y/n)");
//		ClientWorker privateRequestThread = new ClientWorker(requestingAddress, username);
//		Thread th = new Thread(privateRequestThread);
//		th.start();	
//	}


}