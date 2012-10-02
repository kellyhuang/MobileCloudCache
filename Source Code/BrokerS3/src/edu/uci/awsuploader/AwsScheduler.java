package edu.uci.awsuploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import edu.uci.awsuploader.AwsInit.awsDirType;

public class AwsScheduler implements Runnable {

	public int 						awsLogicalTimestamp = 0;
	private final long				AWS_MAX_UPLOAD_LIMIT = (1<<19); 	/* 1 GB per req */
	private final long				AWS_SCHEDULER_SLEEP_DURATION = (10);	/* 10 mins */
	private final long				AWS_SCHEDULER_MAX_WAIT = (300); 		/* 5 hours */
	private static Logger 			logger = Logger.getLogger(AwsScheduler.class);
	private static int				awsBucketId = 0;
	private static int[]			timerArray = {10, 30, 60, 120, 180, 240, 300};
	
	public AwsScheduler(){
		AwsInit.awsCurrentDirSize = new int[10];
		for(int i = 0; i<AwsInit.awsCurrentDirSize.length; i++)
			AwsInit.awsCurrentDirSize[i] = 0;
	}
	
	private LinkedList<String> awsGetDirToUpload()
	{
		LinkedList<String> dirToUpload = new LinkedList<String>();
	
		/* Returns list of directory containing <= 1Gb files in total
		 *  to upload. Returns empty list if no files in any directory
		 */

		if (this.awsLogicalTimestamp <= this.AWS_SCHEDULER_MAX_WAIT) {

			int totalFileBytes = 0;
			HashSet<Integer> dirNums = new HashSet<Integer>();
			
			/* We take 2 passes, one for all directories timing out right now
			 *  and the other pass to fill up space in the upload request
			 */
			
			for(int pass = 0; pass <2; pass++){			
				if(totalFileBytes >= this.AWS_MAX_UPLOAD_LIMIT)
					break;

				for(awsDirType curr  : AwsInit.awsDirType.values()){
					if(AwsInit.awsCurrentDirSize[curr.ordinal()] == 0)
						continue;
					else if((AwsInit.awsCurrentDirSize[curr.ordinal()] + totalFileBytes)
							>= this.AWS_MAX_UPLOAD_LIMIT)
						break;
	
					else if((((this.awsLogicalTimestamp % timerArray[curr.ordinal()])) == 0
							&& pass == 0)||(pass == 1 && !dirNums.contains(curr.ordinal()) )){

						System.err.println("Pass: " + pass + "   Directoy and its size: " + curr.ordinal() + 
								"   " + AwsInit.awsCurrentDirSize[curr.ordinal()]);

						synchronized (AwsInit.awsCurrentDirSize){
							totalFileBytes += AwsInit.awsCurrentDirSize[curr.ordinal()];
						}
						dirNums.add(curr.ordinal());
						String dirPath = null;
						
						switch(curr.ordinal()){
							case 0:
								dirPath = AwsInit.AWS_FILE_10_MIN_DIR;
								break;
							case 1:
								dirPath = AwsInit.AWS_FILE_30_MIN_DIR;
								break;
							case 2:
								dirPath = AwsInit.AWS_FILE_1_HOUR_DIR;
								break;
							case 3:
								dirPath = AwsInit.AWS_FILE_2_HOUR_DIR;
								break;
							case 4:
								dirPath = AwsInit.AWS_FILE_3_HOUR_DIR;
								break;
							case 5:
								dirPath = AwsInit.AWS_FILE_4_HOUR_DIR;
								break;
							case 6:
								dirPath = AwsInit.AWS_FILE_5_HOUR_DIR;
								break;
							default:
								dirPath = AwsInit.AWS_FILE_EOD_DIR;
						}
						
						/* Mark the folder for upload and reduce its size back to 0*/
						dirToUpload.add(dirPath);
					}
				}
			}
			
		}
		
		/* After collecting directories, we check if timer max-out then reset */
		if(this.awsLogicalTimestamp >= this.AWS_SCHEDULER_MAX_WAIT)
			this.awsLogicalTimestamp = 0;

		return dirToUpload;
	}
	
	
	public static void awsCopyFile(File source, File dest)
	{
        InputStream 	in = null;
        OutputStream 	out = null;

        try {
        	in = new FileInputStream(source);
        	out = new FileOutputStream(dest);
    
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	            out.write(buf, 0, len);
	        }
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        finally {
        	try {
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	private void awsMoveFiles(String sourceDir, String destParentDir)
	{
		File[] fileList = (new File(sourceDir)).listFiles();
		
		for (File srcFileHandle : fileList) {
			String srcFileName = srcFileHandle.getName();
			/* check if file is currently used by some upload msg handler */
			if (AwsInit.awsCurrentFilesInUse.contains(srcFileName)) {
				continue;
			}
			/* extract user name and remove user name from file name */
			String userName = srcFileName.substring(0,
					srcFileName.indexOf(AwsInit.AWS_FILE_NAME_DELIMITER));
			srcFileName = srcFileName.substring(srcFileName.indexOf(
					AwsInit.AWS_FILE_NAME_DELIMITER)+1, srcFileName.length());
			String destUserDir = destParentDir + "/" + userName;
			logger.info("Moving file: ["+srcFileName+"] from ["+sourceDir+"] to ["+
					destUserDir+"]");
			File destDir = new File(destUserDir);
			if (!destDir.exists()) {
				destDir.mkdirs();
				logger.info("creating user directory: ["+destUserDir+"]");
			}
			File destFile = new File(destUserDir, srcFileName);
			awsCopyFile(srcFileHandle, destFile);

			/* delete the original file */
			srcFileHandle.delete();
		}
	}
	
	synchronized private int getBucketId() 
	{
		awsBucketId = (awsBucketId + 1) % 100;
		return awsBucketId;
	}
	
	private void emptyDirSize(String dir){
		int dirNum = -1;
		
		
		switch(dir){
		case AwsInit.AWS_FILE_10_MIN_DIR:
			dirNum = AwsInit.awsDirType.AWS_10_MIN.ordinal();
			break;
		case AwsInit.AWS_FILE_30_MIN_DIR:
			dirNum = AwsInit.awsDirType.AWS_30_MIN.ordinal();
			break;
		case AwsInit.AWS_FILE_1_HOUR_DIR:
			dirNum = AwsInit.awsDirType.AWS_1_HOUR.ordinal();
			break;
		case AwsInit.AWS_FILE_2_HOUR_DIR:
			dirNum = AwsInit.awsDirType.AWS_2_HOUR.ordinal();
			break;
		case AwsInit.AWS_FILE_3_HOUR_DIR:
			dirNum = AwsInit.awsDirType.AWS_3_HOUR.ordinal();
			break;
		case AwsInit.AWS_FILE_4_HOUR_DIR:
			dirNum = AwsInit.awsDirType.AWS_4_HOUR.ordinal();
			break;
		case AwsInit.AWS_FILE_5_HOUR_DIR:
			dirNum = AwsInit.awsDirType.AWS_5_HOUR.ordinal();
			break;
		case AwsInit.AWS_FILE_EOD_DIR:
			dirNum = AwsInit.awsDirType.AWS_EOD.ordinal();
			break;
		default:
			System.err.println("Incorrect Directory Number to empty DIR");	
		}
		
		synchronized (AwsInit.awsCurrentDirSize) {
			AwsInit.awsCurrentDirSize[dirNum] = 0;
		}
	}
		
	private void awsMonitorUploadDirs()
	{
		
		while (true) {
			try {
			Thread.sleep(AWS_SCHEDULER_SLEEP_DURATION*100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.awsLogicalTimestamp += this.AWS_SCHEDULER_SLEEP_DURATION;
			/* get list of files */
			LinkedList<String> updDirs = awsGetDirToUpload();
			if (updDirs.isEmpty()) continue;
			logger.info("awsMonitorUploadDirs : Number of Directories to Upload : [" + updDirs.size() + "]");			

			/* create bucket for this thread */
			String bucketName = null;
			File bucketHandle = null;
			
			/*Move files to upload Directory*/
			bucketName = AwsInit.AWS_UPD_BUCKET_NAME + getBucketId();
			bucketHandle = new File(bucketName);
			bucketHandle.mkdirs();
			logger.info("Bucket created: ["+bucketName+"]");
			
			for (String dir: updDirs) {
				/* move files from dir to bucket */
				logger.info("Moving files from dir: ["+dir+"]");
				awsMoveFiles(dir, bucketName);

				/*Empty directory size*/
				this.emptyDirSize(dir);
			}
			
			/* spawn upload thread */
			logger.info("Got some data! lets kick ass of uploader thread");
			Thread t = new Thread(new AwsUploader(bucketHandle), "Uploader");
			t.start();
		}
	}
	
	@Override
	public void run() {
		System.err.println("Scheduler started");
		awsMonitorUploadDirs();
	}
}
