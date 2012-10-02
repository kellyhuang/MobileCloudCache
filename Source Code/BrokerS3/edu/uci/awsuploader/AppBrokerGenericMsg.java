package edu.uci.awsuploader;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;


public class AppBrokerGenericMsg {
	
	public enum awsUpdMsgType {
		AWS_UPD_MSG_INVALID,
		AWS_UPD_MSG_USER_REG_REQ,
		AWS_UPD_MSG_USER_REG_RSP,
		AWS_UPD_MSG_USER_LOGIN_REQ,
		AWS_UPD_MSG_USER_LOGIN_RSP,
		AWS_UPD_MSG_UPLOAD_REQ,
		AWS_UPD_MSG_UPLOAD_RSP,
		AWS_UPD_MSG_GET_OBJ_LIST_REQ,
		AWS_UPD_MSG_GET_OBJ_LIST_RSP
	}
	
	public enum awsUploadPriority {
		AWS_UPD_PRIORITY_HIGH,
		AWS_UPD_PRIORITY_MEDIUM,
		AWS_UPD_PRIORITY_LOW
	}

	public enum awsUploadSensitivity {
		AWS_UPD_SENS_NORMAL,
		AWS_UPD_SENS_LOW
	}

	public enum baseErrCode {
		AWS_REG_MSG_BASE_ECODE(100),
		AWS_LOGIN_MSG_BASE_ECODE(200),
		AWS_UPD_MSG_BASE_ECODE(300),
		AWS_GET_LIST_BASE_ECODE(400);
		
		private final int errCode;
		private baseErrCode(int ecode) {
			this.errCode = ecode;
		}
		public int getValue() {
			return errCode;
		}
	}
	
	
	public awsUpdMsgType		msgType;		
	public int 					seqNum;
	public int					payloadLen;
	public static final int 	APP_BROKER_MSG_HEADER_LEN = 12;
	private static Logger 		logger = Logger.getLogger(AppBrokerGenericMsg.class);	
	
	
	public AppBrokerGenericMsg() {
		this.msgType = awsUpdMsgType.AWS_UPD_MSG_INVALID;
		this.seqNum = 0;
		this.payloadLen = 0;
	}
	
	public byte[] AwsConvertIntToByte(int value){
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			int offset = (b.length - 1 - i) * 8;
			b[i] = (byte) ((value >>> offset) & 0xFF);
		}
		return b;	
	}
	
	
	public int AwsConvertByteToInt(byte[] bytes) {
		
		return (
				((bytes[0] & 0xFF) << 24) |
				((bytes[1] & 0xFF) << 16) |
				((bytes[2] & 0xFF) << 8) |
				(bytes[3] & 0xFF)
			);
	}
	
	public void AwsDecodeAppBrokerMsg(InputStream in) {
		
		byte[] msgHeader = new byte[APP_BROKER_MSG_HEADER_LEN];
		try {
			in.read(msgHeader, 0, 4);
			this.msgType = awsUpdMsgType.values()[AwsConvertByteToInt(msgHeader)];
			in.read(msgHeader, 4, 4);
			this.seqNum = AwsConvertByteToInt(msgHeader);
			in.read(msgHeader, 8, 4);
			this.payloadLen = AwsConvertByteToInt(msgHeader);
			logger.info("Msg Type: ["+this.msgType+"], seqNum: ["+this.seqNum+
					"], paylaodLen: ["+this.payloadLen+"]");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
