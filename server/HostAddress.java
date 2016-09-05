package server;

public class HostAddress {
	private String ipAddress; 
	private String port; 

	public HostAddress(String x, String y) { 
		this.ipAddress = x; 
		this.port = y; 
	}

	public String getIPAddress(){
		if(ipAddress.startsWith("/")){
			return ipAddress.substring(1);//remove / in front of IP-address
		}
		else{
			return ipAddress; 
		}
	}

	public int getPort(){
		int portInt = Integer.parseInt(port);
		return portInt;
	}

	public String toString(){
		String formattedFullAddress = ipAddress+":"+port;
		return formattedFullAddress;
	}

	public void setPort(String portToSet){
		this.port = portToSet;
	}

	public void setIP(String ip){
		this.ipAddress = ip;
	}
}
