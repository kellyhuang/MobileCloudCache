package edu.uci.awsuploader;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;

	public class AwsLoginMsg extends AwsAppBrokerIntfMsg {
		
	public String 			username;
	public String 			password;
	private static Logger	logger = Logger.getLogger(AwsLoginMsg.class);

//	private final static int		AWS_LOGIN_MSG_BASE_ID= baseErrCode.AWS_LOGIN_MSG_BASE_ECODE.getValue();
//	
//	private enum loginMsgErrCodes {
//		AWS_LOGIN_MSG_SUCCESS_CODE(AWS_LOGIN_MSG_BASE_ID + 1),
//		AWS_LOGIN_MSG_ICORRECT_PWD_CODE(AWS_LOGIN_MSG_BASE_ID + 2),
//		AWS_LOGIN_UNREGISTERED_USER(AWS_LOGIN_MSG_BASE_ID + 3);
//		
//		
//		private final int loginErrCode;
//		
//		loginMsgErrCodes(int ecode){
//			this.loginErrCode = ecode;
//		}
//		public int getValue(){
//			return this.loginErrCode;
//		}
//	}
	
	public AwsLoginMsg() {
		this.username = "";
		this.password = "";
	}
	
	public String awsEncodeLoginMsg()
	{
		String regMsg = "";		
		return regMsg;
	}
	
//	private void AwsDecodeLoginMsg(InputStream in)
//	{
//		return;
//	}
	
	public void AwsDecodeLoginMsg(InputStream in)
	{
		ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
		 /*
		 * <length><username><length><password>
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
			System.out.println("Socket stream read error! " + e.getMessage());
		}
		
		String input = new String(inputBytes.toByteArray());

		int offset = 0;
		int tempLength = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
	 
		offset += Integer.SIZE/8;	 
		this.username = input.substring(offset, offset + tempLength);
		 
		offset += tempLength;
		tempLength = AwsConvertByteToInt(input.substring(offset, offset + 4).getBytes());
		 
		offset += Integer.SIZE/8;
		this.password = input.substring(offset, offset + tempLength);
	}


	public byte[] AwsLoginMsgHandler(InputStream in)
	{
		this.AwsDecodeLoginMsg(in);
		/*Call the function that checks the HashMap in containing Usernames and Pwds pf Registered users */
		
		int regCode = AwsRegisterMsg.AwsIsUserRegistered(this.username, this.password);
	
		if(regCode!= AwsRegisterMsg.regMsgErrCode.AWS_LOGIN_MSG_AUTH_SUCCESS.getValue()){
			logger.info("Failed Login attempt from Username: ["+this.username+"] with Pwd: ["+this.password+"]!!");
		}
		
		return AwsConvertIntToByte(regCode);

	}
	
}
