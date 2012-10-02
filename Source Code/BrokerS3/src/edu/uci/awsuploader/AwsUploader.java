package edu.uci.awsuploader;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferProgress;


public class AwsUploader implements Runnable, MultipleFileUpload {

	private AmazonS3 			s3Handle = null;
	private File 				dirHandle;
	private static Logger 		logger = Logger.getLogger(AwsUploader.class);	
	private String				AWS_UPD_RESP_EMAIL;				
	
	public AwsUploader(){	
	}
	
	public AwsUploader(File file){

		try{

			s3Handle = new AmazonS3Client(new PropertiesCredentials
					(AwsUploader.class.getResourceAsStream("AwsCredentials.properties")));
			
			if(!this.s3Handle.doesBucketExist(AwsInit.AWS_UPD_BUCKET_NAME))
				this.s3Handle.createBucket(AwsInit.AWS_UPD_BUCKET_NAME);

			s3Handle.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest
					(AwsInit.AWS_UPD_BUCKET_NAME, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.dirHandle = file;
	}
	
	private void AwsNotifyUser(File [] userFiles, String awsUserName){
		
		/*Getting list of files per user*/
		for(File file : userFiles)
			this.AWS_UPD_RESP_EMAIL += (file.getName() + "\n");

		
		
		final String username = "ysheth46@gmail.com";
		final String password = "password46";
 
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
 
		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		  }
		);
 
		try {
			String receiverEmail = null;
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("yashsheth46@gmail.com"));
			
			String val = AwsInit.awsRegisteredUserMap.get(awsUserName);
//			System.err.println("User email = " + val);
			receiverEmail = (val.split("\\s"))[1];
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(receiverEmail));
			message.setSubject("File Upload Confirmation!");
			message.setText(this.AWS_UPD_RESP_EMAIL + "\n\nPlease do not reply to this email!!\n***Thank You***");
 
			Transport.send(message);
 
			logger.info("Email sent to User: " + awsUserName);
 
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	}
	
	private void AwsUploadDirectory(){
		
    	TransferManager updTransferMgr = new TransferManager(s3Handle);
    	Transfer upd = updTransferMgr.uploadDirectory(AwsInit.AWS_UPD_BUCKET_NAME, AwsInit.AWS_UPD_KEY, this.dirHandle, true);
    	
    	/*Now that we have uploaded the directory,  we use the transfer object to track the download;  	 */ 	
    	this.AWS_UPD_RESP_EMAIL =	 "This is an Auto-Generated message from Save ur Stuff:\n\n";    	
    	
    	
		while(true){
			if(upd.getState() == TransferState.Completed){
				this.AWS_UPD_RESP_EMAIL += "Congratulations! The following file were successfully uploaded!!:\n";
				logger.info("AwsUploadDirectory: Upload successful!");
				break;
			}
			
			if(upd.getState() == TransferState.Failed || upd.getState() == TransferState.Canceled){
				upd = updTransferMgr.uploadDirectory(AwsInit.AWS_UPD_BUCKET_NAME, AwsInit.AWS_UPD_KEY, this.dirHandle, true);
				logger.info("AwsUploadDirectory: Upload failed or cancelled! Retrying..");	
			}
			
			try{
				Thread.sleep(1000);
			}catch (InterruptedException e) {
				logger.info("Problem in waiting for TransferState: "+e.getMessage());
			}
		}
		
		/*Now we need to inform the user that the file has been uploaded;*/
		for(File userDirectory : dirHandle.listFiles()){
			if(userDirectory.isDirectory())
				this.AwsNotifyUser(userDirectory.listFiles(), userDirectory.getName());
		}
		recursiveDelete(this.dirHandle);

	}
	
	public boolean recursiveDelete(File file)
	{
		if(file.isDirectory())
		{
			for(File eachFile : file.listFiles())
			{
				recursiveDelete(eachFile);
			}
			return file.delete();
		}
		else
			return file.delete();
	}
	
	public void run() {
		System.err.println("Uploader Invoked");
		AwsUploadDirectory();
	}

	@Override
	public boolean isDone() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void waitForCompletion() throws AmazonClientException,
			AmazonServiceException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AmazonClientException waitForException() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransferState getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addProgressListener(ProgressListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeProgressListener(ProgressListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TransferProgress getProgress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKeyPrefix() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBucketName() {
		// TODO Auto-generated method stub
		return null;
	}
}
