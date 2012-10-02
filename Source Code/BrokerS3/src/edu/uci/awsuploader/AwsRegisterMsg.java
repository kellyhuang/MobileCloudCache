package edu.uci.awsuploader;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import org.apache.log4j.Logger;


public class AwsRegisterMsg extends AwsAppBrokerIntfMsg {
	public String 							username;
	public String 							password;
	public String 							email;
	private static Logger 					logger = Logger.getLogger(AwsRegisterMsg.class);
	private final static int 				AWS_REG_MSG_BASE_ECODE = 
												baseErrCode.AWS_REG_MSG_BASE_ECODE.getValue();
	private final static int 				AWS_LOGIN_MSG_BASE_ECODE = 
												baseErrCode.AWS_LOGIN_MSG_BASE_ECODE.getValue();
	private final static int 				AWS_UPD_MSG_BASE_ECODE = 
												baseErrCode.AWS_UPD_MSG_BASE_ECODE.getValue();
	
	public static enum regMsgErrCode {
		AWS_REG_MSG_REG_SUCCESS(AWS_REG_MSG_BASE_ECODE + 1),
		AWS_REG_MSG_AUTH_SUCCESS(AWS_REG_MSG_BASE_ECODE + 2),
		AWS_REG_MSG_PASSD_MISMATCH(AWS_REG_MSG_BASE_ECODE + 3),
		AWS_REG_MSG_NOT_REGISTERED(AWS_REG_MSG_BASE_ECODE + 4),
		AWS_REG_MSG_USERNAME_REGISTERED(AWS_REG_MSG_BASE_ECODE + 5),
		AWS_UPD_MSG_SUCCESS(AWS_UPD_MSG_BASE_ECODE + 1);
		
			
		private int errCode;
		private regMsgErrCode(int ecode) {
			this.errCode = ecode;
		}
		public int getValue() {
			return this.errCode;
		}
	}
	
	
	public AwsRegisterMsg() {
		this.username = "";
		this.password = "";
		this.email = "";
	}

	public static int AwsIsUserRegistered(String uname, String upwd)
	{
		String value = null;
		
		if ((AwsInit.awsRegisteredUserMap.containsKey(uname)) && 
				(null != (value = AwsInit.awsRegisteredUserMap.get(uname)))) {
			String passd = value.substring(0, value.indexOf(" "));
			if (passd.equals(upwd)) { 
				return regMsgErrCode.AWS_REG_MSG_AUTH_SUCCESS.getValue();
			}
			else {
				return regMsgErrCode.AWS_REG_MSG_PASSD_MISMATCH.getValue();
			}
		}
		else {
			return regMsgErrCode.AWS_REG_MSG_NOT_REGISTERED.getValue();
		}
	}
	
	public String AwsEncodeRegisterMsg()
	{
		String regMsg = "";
		
		return regMsg;
		
	}
	
	public void AwsDecodeRegisterMsg(InputStream in)
	{
		ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
		/*
		 * <length><username(8 characters)><length><password><length><email>
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
			logger.info("Socket stream read error! " + e.getMessage());
		}
		
		String input = new String(inputBytes.toByteArray());
		int offset = 0;
		byte[] tmpArr = input.substring(offset, offset + 4).getBytes();
		int tempLength = AwsConvertByteToInt(tmpArr);
		offset += Integer.SIZE/8;
		this.username = input.substring(offset, offset + tempLength);
		offset += tempLength;
		tempLength = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
		offset += Integer.SIZE/8;
		this.password = input.substring(offset, offset + tempLength);
		offset += tempLength;
		tempLength = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
		offset += Integer.SIZE/8;
		this.email = input.substring(offset, offset + tempLength);

	}
	
	public byte[] AwsRegisterMsgHandler(InputStream in)
	{
		/* decode the msg */
		this.AwsDecodeRegisterMsg(in);
		
		/* check new user; If new user add to file and in-memory db 
		 * otherwise throw error 
		 */
		if (AwsInit.awsRegisteredUserMap.containsKey(this.username)) {
			return AwsConvertIntToByte(
					regMsgErrCode.AWS_REG_MSG_USERNAME_REGISTERED.getValue());
		}
		else {
			/* add user info to hash map */
			AwsInit.awsRegisteredUserMap.put(this.username, (this.password + " " + this.email));
			/* add user info to db file */
			FileWriter fileWriteHandle = null;
			try {
				fileWriteHandle = new FileWriter(AwsInit.fileHandle);
			} catch (IOException e) {
				e.printStackTrace();
			}
			HashSet<String> keySet = new HashSet<String>(AwsInit.awsRegisteredUserMap.keySet());
			for (String uname : keySet) {
				String userCredentials = uname + " " + AwsInit.awsRegisteredUserMap.get(uname) + "\n";
				try {
					fileWriteHandle.write(userCredentials);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				fileWriteHandle.flush();
				fileWriteHandle.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			/* return err code */	
			return AwsConvertIntToByte(
					regMsgErrCode.AWS_REG_MSG_REG_SUCCESS.getValue());
		}
	}
}
