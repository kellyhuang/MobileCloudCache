package edu.uci.awsuploader;

public class AwsMain {

	public static void main(String args[])
	{

		String 	hostname = "127.0.0.1";
		int 	port = 8999;
		
		/* do the initialization */
		new AwsInit();
		AwsInit.AwsLoadUserDbFile();
		AwsInit.AwsCreateUploadDirStructure();

		/* spawn 2 main threads */
		Thread scheduler = new Thread(new AwsScheduler(), "Scheduler");
		scheduler.start();

		Thread network = new Thread(new AwsNetwork(hostname, port), "Network");
		network.start();
		

	}
}
