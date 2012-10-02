package edu.uci.awsuploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

public class AwsInit {
	
	public enum awsDirType{
		AWS_10_MIN,
		AWS_30_MIN,
		AWS_1_HOUR,
		AWS_2_HOUR,
		AWS_3_HOUR,
		AWS_4_HOUR,
		AWS_5_HOUR,
		AWS_EOD
	}
	
	public static awsDirType dirs;
	public static awsDirType currDir;
	
	static HashMap<String, String> 	awsRegisteredUserMap;
	static HashSet<String>			awsCurrentFilesInUse = new HashSet<String>();
	static final String				AWS_USER_DB_FILE = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\aws_user_info.db";
	static File						fileHandle;
	static final String 			AWS_FILE_10_MIN_DIR = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\monitor\\high\\10m\\";
	static final String 			AWS_FILE_30_MIN_DIR = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\monitor\\high\\30\\";
	static final String				AWS_FILE_1_HOUR_DIR = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\monitor\\high\\1h\\";
	static final String				AWS_FILE_2_HOUR_DIR = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\monitor\\low\\2h\\";
	static final String				AWS_FILE_3_HOUR_DIR = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\monitor\\low\\3h\\";
	static final String				AWS_FILE_4_HOUR_DIR = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\monitor\\low\\4h\\";
	static final String				AWS_FILE_5_HOUR_DIR = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\monitor\\low\\5h\\";
	static final String				AWS_FILE_EOD_DIR = "C:\\Users\\Swapnil\\workspace\\BrokerS3\\monitor\\low\\eod\\";
	static final String 			AWS_FILE_NAME_DELIMITER = "_";
	static final int				AWS_TOTAL_MONITOR_DIRS = 7;
	static int[]					awsCurrentDirSize;
	static final String 			AWS_UPD_BUCKET_NAME = "awsuploadbucket";
	static final String 			AWS_UPD_KEY = "awsuploadkey";	
	private static Logger 			logger = Logger.getLogger(AwsInit.class);
	
	
	static boolean AwsLoadUserDbFile()
	{
		boolean 				success = false;
		boolean 				scndAtmpt = false;
		FileInputStream 		fileStream = null;
		
		if (null == fileHandle) {
			logger.info("Creating user db file: ["+AWS_USER_DB_FILE+"]");
			fileHandle = new File(AWS_USER_DB_FILE);
		}
		
		try {
			fileHandle.createNewFile();
		} catch(IOException e) {
			logger.info("File creation error" + e.getMessage());
		}
		try {
			logger.info("Opening user db file: ["+AWS_USER_DB_FILE+"]");
			fileStream = new FileInputStream(fileHandle);
		} catch (FileNotFoundException e) {
			logger.error("File Not Found " + e.getMessage());
			logger.info("wtf! not possible.. create a new file u bitch!");
			if (scndAtmpt) {
				scndAtmpt = false;
				fileHandle = new File(AWS_USER_DB_FILE);
			}
			else {
				System.exit(1);
			}
		}
		
		/* allocate space to hash map and populate it */
		awsRegisteredUserMap = new HashMap<String, String>();
		AwsPopulateUserHashMap(fileStream);
		
		return success;
	}

	static void AwsPopulateUserHashMap(FileInputStream fileStream)
	{
		BufferedReader fileReader = new BufferedReader(new InputStreamReader(
				fileStream));
		String line = null;
		do {
			try {
				line = fileReader.readLine();
				if (null == line) break;
				String [] userCredentials = line.split(" ");
				awsRegisteredUserMap.put(userCredentials[0], 
					(userCredentials[1] + " " + userCredentials[2]));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (true);
	}
	
	static void AwsCreateUploadDirStructure()
	{
		boolean success = false;
		
		awsCurrentDirSize = new int[AWS_TOTAL_MONITOR_DIRS];
		
		success = (new File(AWS_FILE_10_MIN_DIR)).mkdirs();
		if (success) {
			logger.info("Directory: "+ AWS_FILE_10_MIN_DIR + " created");
		}
		success = (new File(AWS_FILE_30_MIN_DIR)).mkdirs();
		if (success) {
			logger.info("Directory: "+ AWS_FILE_30_MIN_DIR + " created");
		}
		success = (new File(AWS_FILE_1_HOUR_DIR)).mkdirs();
		if (success) {
			logger.info("Directory: "+ AWS_FILE_1_HOUR_DIR + " created");
		}
		success = (new File(AWS_FILE_2_HOUR_DIR)).mkdirs();
		if (success) {
			logger.info("Directory: "+ AWS_FILE_2_HOUR_DIR + " created");
		}
		success = (new File(AWS_FILE_3_HOUR_DIR)).mkdirs();
		if (success) {
			logger.info("Directory: "+ AWS_FILE_3_HOUR_DIR + " created");
		}
		success = (new File(AWS_FILE_5_HOUR_DIR)).mkdirs();
		if (success) {
			logger.info("Directory: "+ AWS_FILE_5_HOUR_DIR + " created");
		}
		success = (new File(AWS_FILE_EOD_DIR)).mkdirs();
		if (success) {
			logger.info("Directory: "+ AWS_FILE_EOD_DIR + " created");
		}
	}
}
