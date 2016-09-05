package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
	protected Authenticator userAuthenticator = null;
	protected HashMap<String, HostAddress> onlineUsers = null;
	protected HashMap<String, LinkedList<String>> offlineMessages = null;
	protected HashMap<String, LinkedList<String>> blockHash = null;
	protected LinkedList<String> loginBlock = null;
	private int serverSocketPort;
	
	public Server(int port) {
		ServerSocket welcomeSocket = null;
		userAuthenticator = new Authenticator();
		onlineUsers = new HashMap<String, HostAddress>();
		offlineMessages = new HashMap<String, LinkedList<String>>();
		blockHash = new HashMap<String, LinkedList<String>>();
		loginBlock = new LinkedList<String>();
		serverSocketPort = port;
		
		try {
			welcomeSocket = new ServerSocket(serverSocketPort);
		} catch (IOException e) {
			System.out.println("Cannot open port "+ serverSocketPort);
			e.printStackTrace();
		}
		
			
		while(true){
			Socket connectionSocket = null;
			try {
				connectionSocket = welcomeSocket.accept();
			} catch (IOException e) {
				System.out.println("Error accepting client connection.");
				e.printStackTrace();
			}
			new Thread(new ServerWorker(connectionSocket, userAuthenticator, onlineUsers, offlineMessages, blockHash, loginBlock)).start();
		}
	}
}
