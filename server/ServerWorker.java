package server;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class ServerWorker implements Runnable{
	protected Socket connectionSocket = null;
	protected Authenticator userAuthenticator = null;
	protected HashMap<String, HostAddress> onlineUsers = null;
	private HashMap<String, LinkedList<String>> offlineMessageHash = null;
	private HashMap<String, LinkedList<String>> blockHash = null;
	private LinkedList<String> loginBlockList = null;
	protected static HashMap<String, Integer> timerHash;
	//	private int clientListeningPort = 0;
	private String username = "";
	private HostAddress clientHost = null;
	private ServerSocket serverListeningSocket;
	private Thread heartbeatThread;
	private boolean run;

	public ServerWorker(Socket clientSocket, Authenticator authenticator,
			HashMap<String, HostAddress> onlineList, HashMap<String, 
			LinkedList<String>> offlineMessages, HashMap<String, 
			LinkedList<String>> blockHash, LinkedList<String> loginBlock){
		this.connectionSocket = clientSocket;
		userAuthenticator = authenticator;
		onlineUsers = onlineList;
		offlineMessageHash = offlineMessages;
		this.blockHash = blockHash;
		this.loginBlockList = loginBlock;
	}

	public void run(){
		BufferedReader inFromClient;

		boolean blockedFromLogin = false;
		try {
			inFromClient = new BufferedReader(new InputStreamReader(
					connectionSocket.getInputStream()));

			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			int validUser = -1;
			do{

				String usernameAndPass = "";
				try {
					usernameAndPass = inFromClient.readLine();
				} catch (IOException e) {
					System.out.println("Could not read buffered stream from client");
					e.printStackTrace();
				}
				username = getUsername(usernameAndPass);

				//check username is not on loginBlockList
				synchronized(loginBlockList){
					if(!loginBlockList.isEmpty()){
						for(int i = 0; i < loginBlockList.size(); i++){
							if(username.equals(loginBlockList.get(i))){
								outToClient.writeBytes("forbidden\r");
								System.out.println(username + " has tried to log in");
								blockedFromLogin = true;
							}
						}
					}
				}//end synchronized loginBlockList

				if(!blockedFromLogin){
					synchronized(userAuthenticator){
						validUser = userAuthenticator.authenticateUser(username, usernameAndPass);
					}
					if(validUser == 0){
						enterChat(username, connectionSocket);
					}
					else if(validUser == 1){
						requestNewAttempt(connectionSocket);
					}
					else if(validUser == 2){
						blockInvalidUser(username, connectionSocket);
					}
					else if(validUser == -1){
						System.out.println("Authenication went wrong");
						break;
					}		
				}
			}
			while (validUser == 1);
		}
		catch (IOException e) {
			System.out.println("Could not read buffered stream from client");
			e.printStackTrace();
		}
	}

	//extracts username from credentials string
	public String getUsername(String credential){
		String[] credentialsSplit = credential.split(" ");
		String username = credentialsSplit[0];
		return username;
	}

	//ask client for another credential entry
	public void requestNewAttempt(Socket connectionSocket) throws IOException{
		DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		outToClient.writeBytes("false\r");
		//		System.out.println("Sever sent retry response");
	}

	//allow user into chatroom
	public void enterChat(String username, Socket connectionSocket) throws IOException{
		synchronized(onlineUsers){
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(
					connectionSocket.getInputStream()));

			outToClient.writeBytes("enter\r");
			String clientIPAddress = connectionSocket.getRemoteSocketAddress().toString();

			//check to see if user is already logged on
			if(alreadyOnline(username) == true){
				System.out.println("User " +username+" is already online at "+ onlineUsers.get(username) + "!");
				HostAddress clientHost = onlineUsers.get(username);
				//send abort message to old ip address
				sendMessage("abort", clientHost);
			}else{
				//broadcast user login event
				broadcast(username, 1);
			}

			//receive and set client listening port
			String clientPort = inFromClient.readLine();
			//			clientListeningPort = Integer.parseInt(clientPort);

			//change info in user hash to listening port
			HostAddress address = formatAddress(clientIPAddress);
			address.setPort(clientPort);
			onlineUsers.put(username, address);
			clientHost = onlineUsers.get(username);
			sendMessage("Welcome to the chatroom!", clientHost);
		}
		serverListeningSocket = new ServerSocket(0);
		System.out.println("listening port: " + serverListeningSocket.getLocalPort());
		sendOfflineMessages();
		establishHeartBeatListeningSocket();
		//set up listening socket
		establishMessageListeningSocket(username);	


	}

	//block user for 60 seconds
	public synchronized void blockInvalidUser(String username, Socket connectionSocket) throws IOException{
		DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		//		outToClient.writeBytes("Too many attempts. You will be blocked for 1 minute.");
		System.out.println("Too many attempts, blocking user: " + username);

		//put user in loginBLockList
		synchronized(loginBlockList){
			loginBlockList.add(username);
		}
		outToClient.writeBytes("block\r");
		Timer newTimer = new Timer();
		newTimer.schedule(new blockTimer(), 60000); //user is blocked from signing in for 60 seconds
	}

	private class blockTimer extends TimerTask{
		public void run() {
			//take username off of loginBlockList
			synchronized(loginBlockList){
				for(int i = 0; i<loginBlockList.size(); i++){
					if(loginBlockList.get(i).equals(username)){
						loginBlockList.remove(i);
						System.out.println("Removing " + username + " from login block list");
					}
				}
			}//end syncrhonized LoginBlockList
		}//end run
	}

	//check if user is already online
	public synchronized boolean alreadyOnline(String username){
		if(onlineUsers.get(username) == null){
			return false; 
		}
		else{
			return true;
		}
	}

	//send message to client on port that it's listening to
	private synchronized void sendMessage(String message, HostAddress address){
		try {
			Socket messageSocket = new Socket(address.getIPAddress(), address.getPort());
			DataOutputStream outStream = new DataOutputStream(messageSocket.getOutputStream());
			outStream.writeBytes(message+"\r");
			messageSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (ConnectException c) {
			//do nothing
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	//OVERLOADED - this is used in case where user may not be online to provide bonus guaranteed message functionality
	private synchronized void sendMessage(String message, HostAddress address, String recieverName){
		String recipientName = recieverName;
		try {
			Socket messageSocket = new Socket(address.getIPAddress(), address.getPort());
			DataOutputStream outStream = new DataOutputStream(messageSocket.getOutputStream());
			outStream.writeBytes(message+"\r");
			messageSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (ConnectException c) {
			String userOffline = "Desired user is no longer online. They will recieve the message when they log in again.";
			sendMessage(userOffline, clientHost);

			//add to offline messages of recipient
			synchronized(offlineMessageHash){
				LinkedList<String> messageList = offlineMessageHash.get(recipientName);
				if(messageList == null){ //currently no offline messages
					messageList = new LinkedList<String>();
				}
				messageList.add(message);

				offlineMessageHash.put(recipientName, messageList);
			}//end synchronized block
			
			//take user out of online hash
			synchronized(onlineUsers){
				onlineUsers.remove(recipientName);
			}

		} catch (IOException e){
			e.printStackTrace();
		}
	}

	private synchronized HostAddress formatAddress(String fullAddress){
		String[] addressSplit = fullAddress.split(":");
		HostAddress host = new HostAddress(addressSplit[0], addressSplit[1]);
		return host;
	}

	//broadcast user login (type 1), logout (type 2)
	private synchronized void broadcast(String username, int type){
		if(type == 1){ //login event
			for(String user : onlineUsers.keySet()){
				if(!isBlockedBy(user) && !isOnBlockList(user)){ //don't send to users that blocked you and users that you block
					if(!isOnBlockList(user)){ // don't send to users you block
						String message = username + " is now online.";
						sendMessage(message, onlineUsers.get(user));
					}
				}
			}
		}
		if(type == 2){//logout event
			for(String user : onlineUsers.keySet()){
				if(!isBlockedBy(user) && !isOnBlockList(user)){ //don't send to users that blocked you and users that you block
					if(!isOnBlockList(user)){ //don't send to users you block
						String message = username + " has logged off.";
						sendMessage(message, onlineUsers.get(user));
					}
				}
			}
		}
	}

	private synchronized void broadcastMessage(String message){
		for(String user : onlineUsers.keySet()){
			if(!isBlockedBy(user) && !isOnBlockList(user)){ //don't send messages to users that blocked client
				String messageToSend = message.substring(10);
				if(user != username){
					sendMessage(username+ ": " + messageToSend, onlineUsers.get(user));
				}
			}
		}
	}

	//establish listening socket for messages from server
	private void establishMessageListeningSocket(String username){
		try {
			run = true;
			//alert client what port the server is listening for messages on
			sendMessage("port:"+serverListeningSocket.getLocalPort(), clientHost);
			while (run){
				Socket messageSocket = serverListeningSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(messageSocket.getInputStream()));
				String newMessage = inFromClient.readLine();
				messageSocket.close();
				handleMessage(newMessage);
			}
		}catch (IOException e) {
			e.printStackTrace();
		} 
	}

	private void establishHeartBeatListeningSocket(){
		ServerSocket heartbeatListeningSocket;
		try{
			HostAddress serverWorkerAddress = new HostAddress(Inet4Address.getLocalHost().getHostAddress(), Integer.toString(serverListeningSocket.getLocalPort()));
			heartbeatListeningSocket = new ServerSocket(0);
			sendMessage("heartbeat:"+heartbeatListeningSocket.getLocalPort(), clientHost);
			heartbeatThread = new Thread(new ServerListenerWorker(serverWorkerAddress, heartbeatListeningSocket));
			heartbeatThread.start();

		}catch (IOException e){
			e.printStackTrace();
		}
	}

	//determine how to handle message from client
	private void handleMessage(String message){
		if(message.startsWith("message ")){
			sendMessageToOtherUser(message);
		}
		else if(message.startsWith("broadcast ")){
			broadcastMessage(message);
		}
		else if(message.equals("online")){
			sendOnlineList();
		}
		else if (message.equals("logout")){
			logout(username);
		}
		else if (message.startsWith("block ")){
			blockUser(message);
		}
		else if (message.startsWith("unblock ")){
			unblockUser(message);
		}
		else if (message.startsWith("getaddress ")){
			//			permissionToPrivateChat(message);
			sendUserAddress(message);
			//			}
			//			else{
			//				sendMessage("Client did not agree to private chat.", clientHost);
			//			}
		}
		//		else if (message.startsWith("y :") || message.startsWith("n :")){
		//			handlePrivateRequestResponse(message);
		//		}
		else{
			System.out.println(message);
		}
	}

	//send a message from user to another user in the chatroom
	private void sendMessageToOtherUser(String message){
		String[] messageArr = message.split(" ");
		String recipientName = messageArr[1];
		HostAddress recipientHost;

		//check if blocked
		boolean blockedBy = isBlockedBy(recipientName); 
		boolean blocked = isOnBlockList(recipientName);
		if(blockedBy == false && blocked == false){
			synchronized(onlineUsers){
				recipientHost = onlineUsers.get(recipientName);
			}
			if(recipientHost == null){ //user is not online
				String offlineMessage = recipientName + " is not online. The message will be sent when "
						+ recipientName + " logs in again.";
				sendMessage(offlineMessage, clientHost);

				//add to offline messages of recipient
				synchronized(offlineMessageHash){
					LinkedList<String> messageList = offlineMessageHash.get(recipientName);
					if(messageList == null){ //currently no offline messages
						messageList = new LinkedList<String>();
					}
					String messageToAdd = formatMessage(messageArr);
					messageList.add(messageToAdd);
					offlineMessageHash.put(recipientName, messageList);
				}//end synchronized block
			}
			else{ //user is online
				String messageToUser = formatMessage(messageArr);
				sendMessage(messageToUser, recipientHost, recipientName);
			}
		}
		else if(blockedBy == true){//user is blocked
			sendMessage("Cannot send message to " + recipientName +". User has blocked you.", clientHost);
		}
		else if (blocked == true){
			sendMessage("Cannot send message to " + recipientName +". You have blocked this user.", clientHost);

		}
	}

	//send user a list of people currently online 
	private void sendOnlineList(){
		String message = "Users currently online:";
		sendMessage(message, onlineUsers.get(username));
		boolean usersAreOnline = false;
		synchronized(onlineUsers){
			if(onlineUsers.size() == 1){ //client is the only one online
				usersAreOnline = false;
				//				sendMessage("There are no users online.", clientHost);
			}
			else{
				for(String user : onlineUsers.keySet()){
					if(!isBlockedBy(user)){ // if client has not blocked this user
						if(user != username){ // don't print current client
							usersAreOnline = true;
							if(isOnBlockList(user)){
								sendMessage(user + " [blocked]", clientHost);
							}
							else{
								sendMessage(user, clientHost);
							}
						}
					}
				}
			}

			if(!usersAreOnline){
				sendMessage("There are no users online.", clientHost);
			}	
		}
	}

	//user has logged themselves out
	private void logout(String userToLogout){
		synchronized(this){
			try{
				sendMessage("logout", clientHost);
			}
			catch(Exception e){
				//do nothing
			}
			onlineUsers.remove(userToLogout);
			broadcast(userToLogout, 2);
			System.out.println(username + " has logged out");
			run = false;
		}
	}

	private void sendOfflineMessages(){
		LinkedList<String> offlineMessages; 
		synchronized(offlineMessageHash){
			offlineMessages = offlineMessageHash.get(username);
			if(offlineMessages == null){
				System.out.println("No offline messages for " + username + ".");
			}
			else{
				String offlineMess = "You recieved these messages while offline:";
				sendMessage(offlineMess, clientHost);
				for(int i = 0; i < offlineMessages.size() ; i++){
					sendMessage(offlineMessages.get(i), clientHost);
				}
				offlineMessageHash.remove(username); //clear offline message after they have been sent
			}
		} //end synchronized block
	}

	//format message to <senderUsername: message>
	private String formatMessage(String[] messageArr){
		String messageToUser = username + ": ";
		for (int i = 2; i<messageArr.length; i++){
			messageToUser += messageArr[i] + " ";
		}
		return messageToUser;
	}

	private void blockUser(String blockMessage){
		LinkedList<String> userBlockList;
		String[] messageArr = blockMessage.split(" ");
		String userToBlock = messageArr[1];
		if (userToBlock != null){
			synchronized(blockHash){
				userBlockList = blockHash.get(username);
				if(userBlockList == null){
					userBlockList = new LinkedList<String>();
				}
				userBlockList.add(userToBlock);
				blockHash.put(username, userBlockList);
				System.out.println(username + " has blocked " + userToBlock);
				String blockConfirmation = "You have now blocked " +userToBlock+ ". You will not recieve messages from this user anymore.";
				sendMessage(blockConfirmation, clientHost);
			}//end synchronized block
		}
		else{
			sendMessage("Usage to block a user: block [user to block]", clientHost);
		}
	}

	private void unblockUser(String unblockMessage){
		LinkedList<String> userBlockList;
		String[] messageArr = unblockMessage.split(" ");
		String userToUnblock = messageArr[1];
		if (userToUnblock != null){
			synchronized(blockHash){
				userBlockList = blockHash.get(username);
				if(userBlockList == null){
					sendMessage("You have not blocked this user.", clientHost);
					return;
				}
				else{
					for(int i = 0; i<userBlockList.size(); i++){
						if(userToUnblock.equals(userBlockList.get(i))){
							userBlockList.remove(i);
						}
					}
				}
				//replace new block list in hash
				blockHash.put(username, userBlockList);
				System.out.println(username + " has unblocked " + userToUnblock);
				String unblockConfirmation = "You have now unblocked " +userToUnblock+ ". You will now recieve messages and information regarding this user.";
				sendMessage(unblockConfirmation, clientHost);
			}//end synchronized block
		}
		else{
			sendMessage("Usage to unblock a user: unblock [user to unblock]", clientHost);
		}
	}

	//check if client is blocked by user parameter
	private boolean isBlockedBy(String blockingUser){
		boolean blocked = false;
		LinkedList<String> blockedList;
		synchronized(blockHash){
			blockedList = blockHash.get(blockingUser);
			if(blockedList != null){
				for(int i = 0; i < blockedList.size(); i++){
					if(blockedList.get(i).equals(username)){
						blocked = true;
					}
				}
			}
		}//end synchronized block
		return blocked;
	}

	private boolean isOnBlockList(String user){
		boolean blocked = false;
		LinkedList<String> blockedList;
		synchronized(blockHash){
			blockedList = blockHash.get(username);
			if(blockedList != null){
				for(int i = 0; i < blockedList.size(); i++){
					if(blockedList.get(i).equals(user)){
						blocked = true;
					}
				}
			}
		}//end synchronized block
		return blocked;
	}

	private void sendUserAddress(String message){
		String[] messageArr = message.split(" ");
		String desiredUser = messageArr[1];
		synchronized(onlineUsers){
			for(String user : onlineUsers.keySet()){
				if (user.equals(desiredUser)){
					if(!isBlockedBy(user)){
						HostAddress userHostAddress = onlineUsers.get(user);
						String desiredUserString = "desiredUser: " + user + " " + userHostAddress.getIPAddress() + " " + Integer.toString(userHostAddress.getPort());
						sendMessage(desiredUserString, clientHost);
					}
					else{
						sendMessage("Cannot provide user information because this user has blocked you.", clientHost);
					}
				}
			}
		}//end synchronized block
	}

	//	private void permissionToPrivateChat(String message){
	//		String[] messageArr = message.split(" ");
	//		String desiredUser = messageArr[1];
	//		HostAddress desiredHost = onlineUsers.get(desiredUser);
	//		try{
	//			if(desiredHost != null){
	//				Socket userSocket = new Socket(desiredHost.getIPAddress(), desiredHost.getPort());
	//				DataOutputStream outStream = new DataOutputStream(userSocket.getOutputStream());
	//				outStream.writeBytes("REQUEST_PORT:" + Integer.toString(serverListeningSocket.getLocalPort()) + 
	//						":" + username + "\r");
	//			}//end if
	//		}catch(ConnectException c){
	//			System.out.println("Canot connect to desired user: " + desiredUser);	
	//		}catch(IOException e){
	//			e.printStackTrace();
	//		}
	//
	//	}

	//	private void handlePrivateRequestResponse(String message){
	//		String[] messageArr = message.split(":");
	//		String response = messageArr[1];
	//
	//		if(response.startsWith("y :")){
	//			System.out.println("user accepted");
	//		}
	//		else{
	//			System.out.println(response);
	//			System.out.println("user denied");
	//		}
	//	}

}
