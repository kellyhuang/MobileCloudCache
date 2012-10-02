package edu.uci.awsuploader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class AwsUploadMsg extends AwsAppBrokerIntfMsg {
	public String 					username;
	public awsUploadPriority		updPriority;
	public int						updDeadline;
	public awsUploadSensitivity		updSensitivity;
	public String 					fileName;
	public File 					fileHandle;
	public long 					BytesInFile;
	private static Logger 			logger = Logger.getLogger(AwsUploadMsg.class);	
	
	public AwsUploadMsg() {
		this.username = "";
		this.updPriority = awsUploadPriority.AWS_UPD_PRIORITY_LOW;
		this.updSensitivity = awsUploadSensitivity.AWS_UPD_SENS_NORMAL;
		this.fileName = "";
		this.fileHandle = null;
	}
	
	/*
	 * Gets the input stream for the payload of the Upload message 
	 */
	public String AwsDecodeUploadMsg(InputStream in)
	{
		ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
		/*
		 * <length><username><(int)priority><(int)timing><(int)sensitivity>
		 * <(int)File Name Length><File Name><(int)No of bytes in file><File Data>
		 */
		
		byte[] buf = new byte[this.payloadLen];
		try {
			int bytesRead = 0;
			int bytesToRead = this.payloadLen;
			int offset = 0;
			while (0 != bytesToRead) {
				bytesRead = in.read(buf, offset, bytesToRead); 
				inputBytes.write(buf, offset, bytesRead);
				bytesToRead -= bytesRead;
				offset += bytesRead;
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Socket stream read error! " + e.getMessage());
		}
		String input = new String(inputBytes.toByteArray());
		int offset = 0;
		
		//Find Length of Username and extract Username
		int tempLength = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
		offset += Integer.SIZE/8;
		this.username = input.substring(offset, offset + tempLength);
		offset += tempLength;
		
		//Priority
		this.updPriority = awsUploadPriority.values()[AwsConvertByteToInt(
				input.substring(offset, offset + 4).getBytes())];
		offset += Integer.SIZE/8;
		
		//Find Length of Date and extract Date
//		tempLength = AwsConvertByteToInt((input.substring(offset, offset + 4)).getBytes());
//		offset += Integer.SIZE/8;
//		DateFormat formatter = new SimpleDateFormat();
//		try {
//			this.updDeadline = (Date)formatter.parse((
//					input.substring(offset, offset + tempLength)));
//		} catch(ParseException e) {
//			e.printStackTrace();
//			logger.info("Upload time parse exception");
//		}
//		offset += tempLength;
		
		//Upload deadline
		int temp = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
		
//		if(temp > 300){
//			this.updDeadline = 300;
//		}else
//			this.updDeadline = temp;
		
		if(temp < 300){	
			if(temp < 30)
				this.updDeadline = 10;
			else if(temp < 60)
				this.updDeadline = 30;
			else this.updDeadline = (temp/60)*60;
		}
		offset += Integer.SIZE/8;		
		
		//Sensitivity
		this.updSensitivity = awsUploadSensitivity.values()[AwsConvertByteToInt(
				input.substring(offset,offset + 4).getBytes())];
		offset += Integer.SIZE/8;
		
		//Find Length of Filename and extract Filename
		tempLength = AwsConvertByteToInt((input.substring(offset, offset + 4).getBytes()));
		offset += Integer.SIZE/8;
		this.fileName = input.substring(offset, offset + tempLength);

//		System.out.println("the name of file " + this.fileName + " Length : " + tempLength);
		offset += tempLength;
		
		
		//No. of Bytes
		this.BytesInFile = AwsConvertByteToInt((input.substring(offset, offset + 4).getBytes()));
		offset += Integer.SIZE/8;
		
		/*Now that we have the fileName, we create a new File object,
		 * write the byte array of remaining payload into File by using FileOutputStream and close*/
		
		return input.substring(offset);
	}
	
	public byte[] AwsUploadMsgHandler(InputStream in){
		
		String fileData = AwsDecodeUploadMsg(in);	
		
		String fileName = this.username + 
				AwsInit.AWS_FILE_NAME_DELIMITER + this.fileName;
		
		
		File file = null;
		int  dirSizeIndex = 0;
		FileOutputStream fileStream = null;
		/*
		 * Assuming the directories are already created
		 */
		try{
		switch(this.updPriority)
		{
		case AWS_UPD_PRIORITY_HIGH:
			switch(this.updDeadline)
			{
				case 10:	
						fileName = AwsInit.AWS_FILE_10_MIN_DIR + fileName;
						file = new File(fileName);
						dirSizeIndex = AwsInit.awsDirType.AWS_10_MIN.ordinal();
					break;
				case 30:
						fileName = AwsInit.AWS_FILE_30_MIN_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_30_MIN.ordinal();
					break;
				case 60:
						fileName = AwsInit.AWS_FILE_1_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_1_HOUR.ordinal();
					break;
				case 120:
					fileName = AwsInit.AWS_FILE_2_HOUR_DIR + fileName;
					file = new File(fileName); 
					dirSizeIndex = AwsInit.awsDirType.AWS_2_HOUR.ordinal();
				break;
				case 180:
						fileName = AwsInit.AWS_FILE_3_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_3_HOUR.ordinal();
					break;
				case 300:
						fileName = AwsInit.AWS_FILE_5_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_5_HOUR.ordinal();
					break;
				default:
						fileName = AwsInit.AWS_FILE_5_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_EOD.ordinal();
					break;
			}
			break;
		case AWS_UPD_PRIORITY_LOW:
			switch(this.updDeadline)
			{
				case 10:	
					fileName = AwsInit.AWS_FILE_10_MIN_DIR + fileName;
					file = new File(fileName);
					dirSizeIndex = AwsInit.awsDirType.AWS_10_MIN.ordinal();
				break;
				case 30:
						fileName = AwsInit.AWS_FILE_30_MIN_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_30_MIN.ordinal();
					break;
				case 60:
						fileName = AwsInit.AWS_FILE_1_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_1_HOUR.ordinal();
					break;
				case 120:
						fileName = AwsInit.AWS_FILE_2_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_2_HOUR.ordinal();
					break;
				case 180:
						fileName = AwsInit.AWS_FILE_3_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_3_HOUR.ordinal();
					break;
				case 300:
						fileName = AwsInit.AWS_FILE_5_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_5_HOUR.ordinal();
					break;
				default:
						fileName = AwsInit.AWS_FILE_5_HOUR_DIR + fileName;
						file = new File(fileName); 
						dirSizeIndex = AwsInit.awsDirType.AWS_EOD.ordinal();
					break;
			}
				break;
			default:
				file = null;
		  }
		}catch(NullPointerException e){
			logger.info("AwsUploadMsg - AwsUploadMsgHandler : Null pointer exception for file name");
			e.printStackTrace();
		}
		
		/*If the file by the same user exists, overwrite it	 */
		if(file.exists()){
			file.delete();
		}
		
		try {
			/* Put the file name currently being written to the Hash Set*/
			 
			synchronized (AwsInit.awsCurrentFilesInUse) {
				AwsInit.awsCurrentFilesInUse.add(fileName);
			}
			file.createNewFile();

		} catch (IOException e) {
			synchronized (AwsInit.awsCurrentFilesInUse) {
				AwsInit.awsCurrentFilesInUse.remove(fileName);
			}						
			logger.info("AwsUploadMsg - AwsUploadMsgHandler : Unable to create file for upload");
			e.printStackTrace();
		}

		
		/* Open a Stream for writing to the file */
		try {
			fileStream = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			logger.info("AwsUploadMsg - AwsUploadMsgHandler: File Not Found");
			e.printStackTrace();
		}						

		/* Write to the file */
		try {
			fileStream.write(fileData.getBytes());
			fileStream.close();
			synchronized (AwsInit.awsCurrentDirSize){
				AwsInit.awsCurrentDirSize[dirSizeIndex] += this.BytesInFile;
			}				
			System.out.println("New File Size= "+this.BytesInFile+"  Updated bytes in directory : " + AwsInit.awsCurrentDirSize[dirSizeIndex]);
			synchronized (AwsInit.awsCurrentFilesInUse) {
				AwsInit.awsCurrentFilesInUse.remove(fileName);
			}						
		} catch (IOException e) {
			logger.info("AwsUploadMsg - AwsUploadMsgHandler: Error while writing to file in directory: " + fileName);
			e.printStackTrace();
		}
		
		return AwsConvertIntToByte(AwsRegisterMsg.regMsgErrCode.AWS_UPD_MSG_SUCCESS.getValue());
	}
}
