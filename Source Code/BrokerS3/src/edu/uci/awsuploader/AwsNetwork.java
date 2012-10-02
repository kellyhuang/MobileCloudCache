package edu.uci.awsuploader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class AwsNetwork implements Runnable {
	private static final int	AWS_SERVER_MAX_BACKLOG = 1000;
	private static int			AWS_USER_REQ_COUNT = 0;
	private static final int	AWS_MAX_USER_COUNT = 5;
	private String 				serverHostname;
	private int					serverPort;
	private ServerSocket		serverSocket;
	private static Logger 		logger = Logger.getLogger(AwsNetwork.class);	
	
	public AwsNetwork() {
		this.serverHostname = "";
		this.serverPort = 0;
		this.serverSocket = null;
	}
	
	public AwsNetwork(String hostname, int port) {
		this.serverHostname = hostname;
		this.serverPort = port;
		this.serverSocket = null;
	}
	
	public static synchronized boolean INCR_USER_COUNT(){
		if(AwsNetwork.AWS_USER_REQ_COUNT < AwsNetwork.AWS_MAX_USER_COUNT){
			AwsNetwork.AWS_USER_REQ_COUNT++;
			return true;
		}
		else return false;
	}
	public static synchronized void DECR_USER_COUNT(){
		AwsNetwork.AWS_USER_REQ_COUNT--;
	}

	
	public String getServerHostname()
	{
		return this.serverHostname;
	}
	
	public int getServerPort()
	{
		return this.serverPort;
	}
	
	public void setServerHostname(String hostname)
	{
		this.serverHostname = hostname;
	}
	
	public void setServerPort(int port)
	{
		this.serverPort = port;
	}
	
	public ServerSocket awsCreateSocket() 
	{
		try {
			this.serverSocket = new ServerSocket(this.serverPort, AWS_SERVER_MAX_BACKLOG,
					InetAddress.getByName(this.serverHostname));
			this.serverSocket.setReuseAddress(true);
		} catch (UnknownHostException e) {
			logger.error("Unknown host: ["+this.serverHostname+"]");
			System.exit(1);
		} catch  (IOException e) {
			logger.error("socket error! "+e.getMessage());
			System.exit(1);
		}

		logger.info("Server IP address: ["+this.serverHostname+"], Port: ["+
				this.serverPort+"]");
		logger.info("Server socket id: ["+this.serverSocket+"] created successfully!");
		return this.serverSocket;
	}
	
	public void awsListenSocket(ServerSocket socket) 
	{
		Socket clientSocket = null;
		
		while(true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			/* wait for new connection from client */
			try {
				clientSocket = socket.accept();
				logger.info("New connection from: ["+
						clientSocket.getInetAddress().getCanonicalHostName()+
						"] at port: ["+clientSocket.getLocalPort()+"]");

				/* spawn new thread to handle the new client */
				Thread t = new Thread(new AwsHandleClient(clientSocket), 
						clientSocket.getInetAddress().getCanonicalHostName()+":"+
						clientSocket.getLocalPort());

				System.err.println("User ctr=" + AwsNetwork.AWS_USER_REQ_COUNT);
				if(AwsNetwork.INCR_USER_COUNT())
						t.start();
				else throw(new IOException("USER COUNT MAX OUT. PLS TRY AFTER SOMETIME"));
		    	
			} catch (IOException e) {
					logger.info("Accept failed! "+ e.getMessage());
			}
		}
	}

	@Override
	public void run() {
		System.err.println("Network started");
		ServerSocket socket = awsCreateSocket();
		awsListenSocket(socket);
	}
	

}
