package edu.uci.awsuploader;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.io.OutputStream;


import org.apache.log4j.Logger;

import edu.uci.awsuploader.AwsAppBrokerIntfMsg.awsUpdMsgType;


public class AwsHandleClient implements Runnable {

	private Socket 				clientSocket;
	private static Logger 		logger = Logger.getLogger(AwsHandleClient.class);	

	
	public AwsHandleClient() {
		this.clientSocket = null;
	}
	
	public AwsHandleClient(Socket socket) {
		this.clientSocket = socket;
	}
	
	private void AwsSendMsg(byte[] msg)
	{
		OutputStream pr = null;
		try {
			pr = this.clientSocket.getOutputStream();
			pr.write(msg);
			logger.info("sending reply to ["+ this.clientSocket+"]");
		} catch(IOException e) {
			logger.error("Socket error! " + e.getMessage());
		}
	}

	public void AwsReadClientSocket()
	{
		AwsAppBrokerIntfMsg clientMsg = null;
		InputStream in = null;
		try {
			clientMsg = new AwsAppBrokerIntfMsg();
			in = this.clientSocket.getInputStream();
			clientMsg.AwsDecodeAppBrokerMsg(in);
			
		} catch (IOException e) {
			logger.info("Scoket error! " + e.getMessage());
		}
		
		byte[] payloadLen = new byte[Integer.SIZE/8];
		byte[] brokerToAppReplyMsg = null;
		byte[] repMsgType = new byte[Integer.SIZE/8];
		byte[] payloadReply = null;
		
		switch (clientMsg.msgType) {
		
		case AWS_UPD_MSG_USER_REG_REQ:
			AwsRegisterMsg clientRegisterMsg = new AwsRegisterMsg();
			clientRegisterMsg.payloadLen = clientMsg.payloadLen;
			payloadReply = clientRegisterMsg.AwsRegisterMsgHandler(in);

			brokerToAppReplyMsg = new byte[AwsAppBrokerIntfMsg.APP_BROKER_MSG_HEADER_LEN +
			                                    payloadReply.length];

			repMsgType = clientMsg.AwsConvertIntToByte(awsUpdMsgType.
					AWS_UPD_MSG_USER_REG_RSP.ordinal());
			for(int i = 3*Integer.SIZE/8; i < payloadReply.length; i++)
				brokerToAppReplyMsg[i] = payloadReply[i - 3*Integer.SIZE];
			payloadLen = clientMsg.AwsConvertIntToByte(payloadReply.length);
			
			break;
			
		case AWS_UPD_MSG_USER_LOGIN_REQ:
			AwsLoginMsg clientLoginMsg = new AwsLoginMsg();
			clientLoginMsg.payloadLen = clientMsg.payloadLen;
			
			payloadReply = clientLoginMsg.AwsLoginMsgHandler(in);
			
			brokerToAppReplyMsg = new byte[AwsAppBrokerIntfMsg.APP_BROKER_MSG_HEADER_LEN +
			                             payloadReply.length];

			repMsgType = clientMsg.AwsConvertIntToByte(awsUpdMsgType.
					AWS_UPD_MSG_USER_LOGIN_RSP.ordinal());
			for(int i = 3*Integer.SIZE; i < payloadReply.length; i++)
				brokerToAppReplyMsg[i] = payloadReply[i - 3*Integer.SIZE];

			payloadLen = clientMsg.AwsConvertIntToByte(payloadReply.length);

			break;
			
		case AWS_UPD_MSG_UPLOAD_REQ:
			AwsUploadMsg clientUploadMsg = new AwsUploadMsg();
			payloadReply = clientUploadMsg.AwsUploadMsgHandler(in);

			brokerToAppReplyMsg = new byte[AwsAppBrokerIntfMsg.APP_BROKER_MSG_HEADER_LEN +
			                              payloadReply.length];

			repMsgType = clientMsg.AwsConvertIntToByte(awsUpdMsgType.
				                                            AWS_UPD_MSG_UPLOAD_RSP.ordinal());

			for(int i = 3*Integer.SIZE; i < payloadReply.length; i++)
				brokerToAppReplyMsg[i] = payloadReply[i - 3*Integer.SIZE];

			payloadLen = clientMsg.AwsConvertIntToByte(payloadReply.length);
			break;
			
		case AWS_UPD_MSG_GET_OBJ_LIST_REQ:
			AwsGetObjListMsg clientGetListMsg = new AwsGetObjListMsg();
			/* put reply code here*/
			clientGetListMsg.awsDecodeGetObjListMsg(in);
			break;
			
		default:
			logger.error("Invalid msg type: ["+clientMsg.msgType+"]");
		}

		int offset = 0;
		System.arraycopy(repMsgType, 0, brokerToAppReplyMsg, offset, Integer.SIZE/8);
		offset += Integer.SIZE/8;
		System.arraycopy(clientMsg.AwsConvertIntToByte(clientMsg.seqNum), 0, brokerToAppReplyMsg,
				offset, Integer.SIZE/8);
		offset += Integer.SIZE/8;
		System.arraycopy(payloadLen, 0, brokerToAppReplyMsg,
				offset, Integer.SIZE/8);
		offset += Integer.SIZE/8;
		System.arraycopy(payloadReply, 0, brokerToAppReplyMsg,
				offset, payloadReply.length);		
		
		this.AwsSendMsg(brokerToAppReplyMsg);
		
	}
	
	@Override
	public void run() {
		AwsReadClientSocket();
	}

}
