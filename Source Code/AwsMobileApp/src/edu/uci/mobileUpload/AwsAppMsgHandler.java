package edu.uci.mobileUpload;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

import android.util.Log;

public class AwsAppMsgHandler extends AwsAppBrokerIntfMsg {
	Socket 		clientSocket;
	String 		username;
	String 		password;
	String 		email;
	int 		SequenceNo;
	String 		hostName;
	int 		portNumber;
	static final String TAG = "AppMsgHandler";
	
	public AwsAppMsgHandler(String username, String password, String email) {
		this.username = username;
		this.password = password;
		this.email 	  = email;
		this.clientSocket = null;
	}
	
	public int init(String hostName, int portNumber)
	{
		try {
			this.hostName 	= hostName;
			this.portNumber = portNumber;
			Log.d("AppMsgHandler","Server addr: " + InetAddress.getByName(this.hostName));
			this.clientSocket = new Socket(InetAddress.getByName(this.hostName), this.portNumber);
		} catch (IOException e) {
			Log.d(TAG, "Error while creating a socket! "+ e.getMessage());
		}
		if (null == this.clientSocket) {
			return -1;
		}
		else return 0;
	}
	
	
	byte[] AwsReadSocketStream(InputStream in, int msgId, int seqNum, byte[] payload)
	{
		try {
			byte [] msgType = new byte[Integer.SIZE/8];
			in.read(msgType, 0, Integer.SIZE/8);
			msgId = AwsConvertByteToInt(msgType);
			
			byte[] seq = new byte[Integer.SIZE/8];
			in.read(seq, 0, Integer.SIZE/8);
			seqNum = AwsConvertByteToInt(seq);
			
			byte[] payloadBuf = new byte[Integer.SIZE/8];
			in.read(payloadBuf, 0, Integer.SIZE/8);
			int payloadLen = AwsConvertByteToInt(payloadBuf);
			Log.d(TAG, "msg: "+msgId+"seqNum"+seqNum+"payloadLen"+payloadLen);

			payload = new byte[payloadLen];
			int 	bytesRead = 0;
			int 	bytesToRead = payloadLen;
			int 	offset = 0;
			while (0 != bytesToRead) {
				bytesRead = in.read(payload, offset, bytesToRead); 
				bytesToRead -= bytesRead;
				offset += bytesRead;
			}
			
		} catch (IOException e) {
			Log.d(TAG, "Socket Read Error!" + e.getMessage());
			e.printStackTrace();
		}
		return payload;
	}
	
	public byte[] PrepareMsg(awsUpdMsgType msgType, FileInputStream fileStream, 
								UploadDetails updDetails)
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

				/*
				 * Construct the payload byte array
				 */
				payload = new byte[username.length() + updDetails.awsFileUpdName.length() 
				                   + updDetails.awsFileUpdSize + 6*Integer.SIZE/8];
				/* username and length */
				temp = AwsConvertIntToByte(username.length());
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;
				System.arraycopy(username.getBytes(), 0, payload, offsetPayload, username.getBytes().length);
				offsetPayload += username.getBytes().length;
				
				/* priority */ 
				temp = AwsConvertIntToByte(updDetails.awsFileUpdPriority);
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;
				
				/* Timing in minutes */
				temp = AwsConvertIntToByte(updDetails.awsFileUpdDeadline);
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;

				/* Sensitivity */
				temp = AwsConvertIntToByte(updDetails.awsFileUpdSenstivity);
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;

				/* File Parameters */
				/* File Name */
				temp = AwsConvertIntToByte(updDetails.awsFileUpdName.length());
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;
				System.arraycopy(updDetails.awsFileUpdName.getBytes(), 0, 
									payload, offsetPayload, updDetails.awsFileUpdName.getBytes().length);
				offsetPayload += updDetails.awsFileUpdName.getBytes().length;
				
				/* No of Bytes in File */
				temp = AwsConvertIntToByte(updDetails.awsFileUpdSize);
				System.arraycopy(temp, 0, payload, offsetPayload, 4);
				offsetPayload += Integer.SIZE/8;
				
				/* THE File Stream itself */
				try {
					int bytesRead = 0;
					int bytesToRead = updDetails.awsFileUpdSize;
					while (0 != bytesToRead) {
						bytesRead = fileStream.read(payload, offsetPayload, bytesToRead); 
						bytesToRead -= bytesRead;
						offsetPayload += bytesRead;
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
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
		for(int i = 0; i < temp.length ; i++)
			header[i + offsetHeader] = temp[i];
		offsetHeader += Integer.SIZE/8;
		msg = new byte[offsetHeader + offsetPayload];

		for(int i = 0; i < offsetHeader; i++)
			msg[i] = header[i];
		
		for(int i=0; i < offsetPayload; i++)
			msg[i+offsetHeader] = payload[i];
		
		return msg;
	}
}
