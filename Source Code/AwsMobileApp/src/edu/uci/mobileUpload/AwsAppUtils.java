package edu.uci.mobileUpload;

import android.app.Application;


public class AwsAppUtils extends Application {
	
	private final static int				AWS_REG_MSG_BASE_ECODE = 100;
	private final static int				AWS_UPD_MSG_BASE_ECODE = 300;
	final static String						AWS_NAME_SERVER_HOSTNAME = "169.234.19.85";
	final static int						AWS_NAME_SERVER_PORT = 8888;
	static String							awsAppUserName;
	static String							awsAppUserPwd;
	static String							awsAppUserEmail;
	
	public static enum awsAppMsgErrCode {
		/*
		 * REGISTRATION ERR CODES
		 */
		AWS_REG_MSG_REG_SUCCESS(AWS_REG_MSG_BASE_ECODE + 1),
		AWS_REG_MSG_AUTH_SUCCESS(AWS_REG_MSG_BASE_ECODE + 2),
		AWS_REG_MSG_PASSD_MISMATCH(AWS_REG_MSG_BASE_ECODE + 3),
		AWS_REG_MSG_NOT_REGISTERED(AWS_REG_MSG_BASE_ECODE + 4),
		AWS_REG_MSG_USERNAME_REGISTERED(AWS_REG_MSG_BASE_ECODE + 5),
		AWS_UPD_MSG_SUCCESS(AWS_UPD_MSG_BASE_ECODE + 1);
	

		private int errCode;
		private awsAppMsgErrCode(int ecode) {
			this.errCode = ecode;
		}
		public int getValue() {
			return this.errCode;
		}
	}
	
	static byte[] AwsAppConvertIntToByte(int value){
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			int offset = (b.length - 1 - i) * 8;
			b[i] = (byte) ((value >>> offset) & 0xFF);
		}
		return b;	
	}
	
	
	static int AwsAppConvertByteToInt(byte[] bytes) {
		
		return (
				((bytes[0] & 0xFF) << 24) |
				((bytes[1] & 0xFF) << 16) |
				((bytes[2] & 0xFF) << 8) |
				(bytes[3] & 0xFF)
			);
	}
	
	static void AwsAppStoreUserInfo(String uname, String pwd) {
		/* read user name and email from sd card */
		awsAppUserName = uname;
		awsAppUserPwd = pwd;
		awsAppUserEmail = "";
	}
}
