package server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class Authenticator {
	protected static volatile HashMap<String, Integer> userAuthentificationHash;
	
	public Authenticator(){
		userAuthentificationHash = new HashMap<String, Integer>();
	}
	
	//verifies credentials in text file
		private synchronized boolean verifyCredentials(String usrCredential) throws FileNotFoundException{
			boolean accepted=false;
			Scanner reader = new Scanner(new FileInputStream("server/credentials.txt"));
			while (reader.hasNextLine()){
				String credential = reader.nextLine();
				if(usrCredential.equals(credential)){
					accepted=true;
					break;
				}
			}
			return accepted;
		}
		
		public synchronized int getNumOfUserAttempts(String username){
			int attempts = userAuthentificationHash.get(username);
			return attempts;
		}
		
		public synchronized int authenticateUser(String username, String credentials) throws FileNotFoundException{
			//userStatus = 0 (accepted), 1 (rejected, try again), 2 (rejected, blocked); 
			int userStatus=-1;
			boolean firstAttempt = checkIfFirstAttempt(username);
			boolean validCredentials = verifyCredentials(credentials);
			
			//valid credentials
			if(validCredentials){
				userStatus = 0; //accepted
			}
			//invalid credentials
			else{
								
				//case: first invalid attempt
				if(firstAttempt){
					userAuthentificationHash.put(username, 1);
					userStatus = 1; // try again
				}
				//case: not the first invalid attempt
				else{
					int numOfAttempts = userAuthentificationHash.get(username);
					//second invalid attempt
					if(numOfAttempts == 1){
						userAuthentificationHash.put(username, 2);
						userStatus = 1; // try again
					}
					//third invalid attempt
					else if(numOfAttempts == 2){
						userAuthentificationHash.remove(username); //set attempts back to zero
						userStatus = 2; //block
					}
				}
			}
			return userStatus;
		}
		
		private synchronized boolean checkIfFirstAttempt(String username){
			if(userAuthentificationHash.get(username) == null){
				System.out.println("This is the first attempt for: " + username);
				return true;
			}else{
				return false;
			}
		}
		
		private void printHash(){
			if(userAuthentificationHash.isEmpty()){
				System.out.println("Hash is currently empty");
			}
			for (String name: userAuthentificationHash.keySet()){
				String key =name.toString();
				String value = userAuthentificationHash.get(name).toString();  
				System.out.println(key + " " + value);  
			} 
		}
}
