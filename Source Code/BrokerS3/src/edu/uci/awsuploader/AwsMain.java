package edu.uci.awsuploader;

public class AwsMain {

	public static void main(String args[])
	{

		String 	hostname = "169.234.19.85";
		int 	port = 8888;
		
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
