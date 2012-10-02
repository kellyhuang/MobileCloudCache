package edu.uci.awsclient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

public class TestClient extends AwsAppBrokerIntfMsg {
	Socket 						client;
	String 						username;
	String 						password;
	String 						email;
	int 						SequenceNo;
	String[] 					files = {"mumbai.jpg", "yash2.txt", "mumbai.jpg", "yash2.txt", "yash.txt"};
	private AwsAppBrokerIntfMsg replyHeader;
	private static Logger 		logger = Logger.getLogger(AwsAppBrokerIntfMsg.class);	
	
	public TestClient(String username, String password, String email) {
		this.username = username;
		this.password = password;
		this.email 	  = email;
	}
	
	private void decodeGetListReply(String input){
		/* <length><String of all filenames separated by space> */ 

		int offset = 0;
		
		//Find Length of Username and extract Username
		int tempLength = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
		offset += Integer.SIZE/8;
		String fileString = input.substring(offset, offset + tempLength);
		
		String [] fileNames = fileString.split("\n");
		
		for(int i=0; i<fileNames.length; i++)
			System.out.println(fileNames[i]);
	}
	
	private void decodeOtherReply(String input){
		
		int num = AwsConvertByteToInt(input.getBytes());
		System.out.println("Reply Message = " + num);		
	}
	

	
	public void start()
	{
		OutputStream out = null;
		InputStream in = null;
			
		try
		{
			/*Testing for admission control*/
//			for(int j = 0; j<7; j++)			
			this.client = new Socket(InetAddress.getByName("127.0.0.1"), 8999);
			
			System.out.println("Client port: " + this.client.getPort());
			
			out = client.getOutputStream();
			in = client.getInputStream();
		}catch(IOException e){
			e.printStackTrace();
		}		
		/*
		 * register
		 */
		byte[] msg = PrepareMsg(awsUpdMsgType.AWS_UPD_MSG_UPLOAD_REQ);
		byte[] upload = PrepareMsg(awsUpdMsgType.AWS_UPD_MSG_USER_LOGIN_REQ);
//		byte[] buf = new byte[100];
		try
		{
			out.write(msg);
			replyHeader = new AwsAppBrokerIntfMsg();
			replyHeader.AwsDecodeAppBrokerMsg(in);	
			
			ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();		
			byte[] buf = new byte[replyHeader.payloadLen];

			int bytesRead = 0;
			int bytesToRead = replyHeader.payloadLen;
			int offset = 0;
			while (0 != bytesToRead) {
				bytesRead = in.read(buf); 
				inputBytes.write(buf, offset, bytesRead);
				bytesToRead -= bytesRead;
				offset += bytesRead;
			}
			
			String input = new String(inputBytes.toByteArray());
//			decodeGetListReply(input);
			decodeOtherReply(input);
			
		}catch (IOException e) {
			e.printStackTrace();
		}
		

		
		/*
		 * Login
		 */
		/*
		 * Upload
		 */
	}
	
	public byte[] PrepareMsg(awsUpdMsgType msgType)
	{
		int offsetHeader = 0;
		int offsetPayload = 0;
		byte[] header = new byte[3*Integer.SIZE/8];
		byte[] payload = null; 
		byte[] msg;
		byte[] temp = AwsConvertIntToByte(msgType.ordinal());
		/* Write the message type to the header */
		for(int i = 0; i < temp.length ; i++)
			header[i + offsetHeader] = temp[i];
		offsetHeader += Integer.SIZE/8;
		temp = AwsConvertIntToByte(this.SequenceNo);
		/* Write the sequence number to the header */
		for(int i = 0; i < temp.length ; i++)
			header[i + offsetHeader] = temp[i];
		offsetHeader += Integer.SIZE/8;
		switch(msgType)
		{
			case AWS_UPD_MSG_USER_REG_REQ:
				payload = new byte[username.length() + password.length() + email.length() + 3*Integer.SIZE/8];
				temp = AwsConvertIntToByte(username.length());
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += Integer.SIZE/8;
				temp = username.getBytes();
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += temp.length;

				temp = AwsConvertIntToByte(password.length());
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += Integer.SIZE/8;
				temp = password.getBytes();
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += temp.length;

				temp = AwsConvertIntToByte(email.length());
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += Integer.SIZE/8;
				temp = email.getBytes();
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += temp.length;
				break;
			
			case AWS_UPD_MSG_USER_LOGIN_REQ:
				payload = new byte[username.length() + password.length() + 2*Integer.SIZE/8];
				
				//Append username length and username
				temp = AwsConvertIntToByte(username.length());
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += Integer.SIZE/8;
				temp = username.getBytes();
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += temp.length;
				
				//Append pwd length and pwd
				temp = AwsConvertIntToByte(password.length());
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += Integer.SIZE/8;
				temp = password.getBytes();
				for(int i = 0; i < temp.length; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += temp.length;

				break;
				
			case AWS_UPD_MSG_UPLOAD_REQ:
				/*
				 * <length><username><(int)priority><(int)timing><(int)sensitivity>
				 * <(int)File Name Length><File Name><(int)No of bytes in file><File Data>
				 */
				FileInputStream fileStream = null;
				String fileName = this.files[0];
				File file = new File(fileName);
				if(file.exists())
				{					
					try {
						fileStream = new FileInputStream(file);
					}catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}				

				/*
				 * Construct the payload byte array
				 */
				int lent = username.length() + fileName.length() + (int)file.length() + 6*Integer.SIZE/8;
				System.out.println("***************** len " + lent);
				payload = new byte[username.length() + fileName.length() + (int)file.length() + 6*Integer.SIZE/8];
				/* username and length */
				temp = AwsConvertIntToByte(username.length());

				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;
				System.arraycopy(username.getBytes(), 0, payload, offsetPayload, username.getBytes().length);
				offsetPayload += username.getBytes().length;
				
				/* priority */ 
				temp = AwsConvertIntToByte(AwsAppBrokerIntfMsg.awsUploadPriority.AWS_UPD_PRIORITY_HIGH.ordinal());
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;
				
				/* Timing in minutes */
				temp = AwsConvertIntToByte(120);
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;

				/* Sensitivity */
				temp = AwsConvertIntToByte(AwsAppBrokerIntfMsg.awsUploadSensitivity.AWS_UPD_SENS_NORMAL.ordinal());
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;

				/* File Parameters */
				/* File Name */
				temp = AwsConvertIntToByte(fileName.length());
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;
				System.arraycopy(fileName.getBytes(), 0, payload, offsetPayload, fileName.getBytes().length);
				offsetPayload += fileName.length();
				
				/* No of Bytes in File */
				temp = AwsConvertIntToByte((int)file.length());
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;
				System.out.println("No of bytes in file : " + file.length());
				/* THE File Stream itself */
				byte[] temp1 = new byte[(int)file.length()];
			try {
				int len = 0;
				int offset = 0;

				while(offset < file.length()){
					len = fileStream.read(temp1, offset, (int)file.length() - offset);
					offset += len;
				}				
				System.out.println("length read : " + len);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.arraycopy(temp1, 0, payload, offsetPayload, (int)file.length());
			offsetPayload += temp1.length;
				break;				
			case AWS_UPD_MSG_GET_OBJ_LIST_REQ:
				payload = new byte[username.length() + Integer.SIZE/8];
				temp = AwsConvertIntToByte(username.length());
				for(int i = 0; i < Integer.SIZE/8; i++)
					payload[i+offsetPayload] = temp[i];
				offsetPayload += Integer.SIZE/8;
				byte [] usrname = new byte[username.getBytes().length];
				usrname = username.getBytes();
				for(int i = 0; i < usrname.length; i++)
					payload[i+offsetPayload] = usrname[i];
				offsetPayload += usrname.length;
				break;
				
		}
		
		temp = AwsConvertIntToByte(offsetPayload);
		System.out.println("Payload length  : " + offsetPayload);
		for(int i = 0; i < temp.length ; i++)
			header[i + offsetHeader] = temp[i];
		offsetHeader += Integer.SIZE/8;
		msg = new byte[offsetHeader + offsetPayload];
		int l = offsetHeader + offsetPayload;
		System.out.println(" size : " + l);
		for(int i = 0; i < offsetHeader; i++)
			msg[i] = header[i];
		
		for(int i=0; i < offsetPayload; i++)
			msg[i+offsetHeader] = payload[i];
		
//		for(int i=0; i<msg.length; i++)
//			System.out.print(msg[i] + " ");
		return msg;
	}
	
	public static void main(String args[])
	{
		new TestClient("swapnil","tiwari","swapnil.sdt@gmail.com").start();
	}

}
