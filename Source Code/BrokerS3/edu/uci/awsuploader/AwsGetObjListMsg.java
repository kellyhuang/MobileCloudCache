package edu.uci.awsuploader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;


public class AwsGetObjListMsg extends AwsAppBrokerIntfMsg {

	public String				username;
//	public String				path;
	public String[]				AWS_OBJ_LIST;
	private static Logger 		logger = Logger.getLogger(AwsUploader.class);	
	private AmazonS3			s3HandleForList;				

	
	public AwsGetObjListMsg() {
		this.username = "";
//		this.path = "";
		
		try{
			File file1 = new File("C:\\Users\\Yash\\workspace\\AWSUploader\\src\\AwsCredentials.properties");

			s3HandleForList = new AmazonS3Client(new PropertiesCredentials(file1));

//			s3Handle = new AmazonS3Client(new PropertiesCredentials
//					(AwsUploader.class.getResourceAsStream("AwsCredentials.properties")));
			
			if(!this.s3HandleForList.doesBucketExist(AwsInit.AWS_UPD_BUCKET_NAME))
				this.s3HandleForList.createBucket(AwsInit.AWS_UPD_BUCKET_NAME);

			s3HandleForList.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest
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
			System.err.println("IOException in creating S3 handle for Listing");
		}

	}
	
	public void AwsDecodeGetListMsg(InputStream in)
	{
		ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
		
		/*
		 * <length><username><length><path>
		 */ 
		byte[] buf = new byte[this.payloadLen];
		try {
			int bytesRead = 0;
			int bytesToRead = this.payloadLen;
			int offset = 0;
			while (0 != bytesToRead) {
				bytesRead = in.read(buf); 
				inputBytes.write(buf, offset, bytesRead);
				bytesToRead -= bytesRead;
				offset += bytesRead;
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Socket stream read error! [" + e.getMessage() + "]");
		}
		
		String input = new String(inputBytes.toByteArray());
		int offset = 0;
		
		//Find Length of Username and extract Username
		int tempLength = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
		offset += Integer.SIZE/8;
		this.username = input.substring(offset, offset + tempLength);
		offset += tempLength;
		
//		//Find Length of path and extract path
//		tempLength = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
//		offset += Integer.SIZE/8;
//		this.path = input.substring(offset, offset + tempLength);
//		offset += tempLength;
		
	}

	public byte[] AwsGetObjListMsgHandler(InputStream in)
	{
		/* decode the msg */
		this.AwsDecodeGetListMsg(in);
		String bucket = AwsInit.AWS_UPD_BUCKET_NAME;
		String key = AwsInit.AWS_UPD_KEY + "/"+this.username;
		
    	System.out.println("Listing objects for : " + bucket);
        ObjectListing objectListing = s3HandleForList.listObjects(new ListObjectsRequest()
                .withBucketName(bucket).withPrefix(key));
        
        /*Get key of every object under the user folder, and take the last substring to get only
         * the file name from the key string.
         * Then, we create the reply message in bytes to return;
         */
        
        String repPayload = "";
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
        	repPayload += objectSummary.getKey().substring(key.length()+1) + "\n";
        	System.out.println("\t- " + objectSummary.getKey().substring(key.length()+1) + "  " +
                               "(size = " + objectSummary.getSize() + ")");
        }
        byte[] reply = new byte[repPayload.length() + Integer.SIZE/8];
        byte[] temp = new byte[repPayload.length()];
        temp = AwsConvertIntToByte(repPayload.length());
        System.arraycopy(temp, 0, reply, 0, Integer.SIZE/8);

        temp = repPayload.getBytes();
        System.arraycopy(temp, 0, reply, Integer.SIZE/8, repPayload.length());
        
		return reply;
	}

}
