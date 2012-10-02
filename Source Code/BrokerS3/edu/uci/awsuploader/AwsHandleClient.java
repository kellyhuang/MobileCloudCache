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
	private AwsAppBrokerIntfMsg clientMsg; 
	private byte[] 				repMsgType = new byte[Integer.SIZE/8];
	private byte[] 				payloadLen = new byte[Integer.SIZE/8];

	
	public AwsHandleClient() {
		clientMsg = null;
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
			System.err.println("Return MsgType= "+ clientMsg.msgType + "  Seq No= " + clientMsg.seqNum
					+ "  Payload Size= "+ clientMsg.payloadLen);
		} catch(IOException e) {
			logger.error("Socket error! " + e.getMessage());
		}
	}

	public void AwsReadClientSocket()
	{
		
		InputStream in = null;
		try {
			clientMsg = new AwsAppBrokerIntfMsg();
			in = this.clientSocket.getInputStream();
			clientMsg.AwsDecodeAppBrokerMsg(in);
			
		} catch (IOException e) {
			logger.info("Scoket error! " + e.getMessage());
		}
		
		byte[] brokerToAppReplyMsg = null;
		byte[] payloadReply = null;
		
		switch (clientMsg.msgType) {
		
		case AWS_UPD_MSG_USER_REG_REQ:
			AwsRegisterMsg clientRegisterMsg = new AwsRegisterMsg();
			clientRegisterMsg.msgType = clientMsg.msgType;
			clientRegisterMsg.payloadLen = clientMsg.payloadLen;
			
			repMsgType = clientMsg.AwsConvertIntToByte(awsUpdMsgType.
							AWS_UPD_MSG_USER_REG_RSP.ordinal());
			payloadReply = clientRegisterMsg.AwsRegisterMsgHandler(in);
			break;
			
		case AWS_UPD_MSG_USER_LOGIN_REQ:
			AwsLoginMsg clientLoginMsg = new AwsLoginMsg();
			clientLoginMsg.msgType = clientMsg.msgType;
			clientLoginMsg.payloadLen = clientMsg.payloadLen;
			
			repMsgType = clientMsg.AwsConvertIntToByte(awsUpdMsgType.
							AWS_UPD_MSG_USER_LOGIN_RSP.ordinal());
			payloadReply = clientLoginMsg.AwsLoginMsgHandler(in);
			break;
			
		case AWS_UPD_MSG_UPLOAD_REQ:
			AwsUploadMsg clientUploadMsg = new AwsUploadMsg();
			clientUploadMsg.msgType = clientMsg.msgType;
			clientUploadMsg.payloadLen = clientMsg.payloadLen;

			repMsgType = clientMsg.AwsConvertIntToByte(awsUpdMsgType.
                    AWS_UPD_MSG_UPLOAD_RSP.ordinal());
			payloadReply = clientUploadMsg.AwsUploadMsgHandler(in);
			break;
			
		case AWS_UPD_MSG_GET_OBJ_LIST_REQ:
			AwsGetObjListMsg clientGetListMsg = new AwsGetObjListMsg();
			clientGetListMsg.msgType = clientMsg.msgType;
			clientGetListMsg.payloadLen = clientMsg.payloadLen;

			repMsgType = clientMsg.AwsConvertIntToByte(awsUpdMsgType.
                    AWS_UPD_MSG_GET_OBJ_LIST_RSP.ordinal());
			payloadReply = clientGetListMsg.AwsGetObjListMsgHandler(in);
			break;
			
		default:
			logger.error("Invalid msg type: ["+clientMsg.msgType+"]");
		}
		
		if(payloadReply!=null){
			brokerToAppReplyMsg = new byte[AwsAppBrokerIntfMsg.APP_BROKER_MSG_HEADER_LEN +
				                             payloadReply.length];
			System.arraycopy(payloadReply, 0, brokerToAppReplyMsg, 
					AwsAppBrokerIntfMsg.APP_BROKER_MSG_HEADER_LEN, payloadReply.length);
				payloadLen = clientMsg.AwsConvertIntToByte(payloadReply.length);
		}

		int offset = 0;
		if(brokerToAppReplyMsg != null)
		{
			System.arraycopy(repMsgType, 0, brokerToAppReplyMsg, offset, Integer.SIZE/8);
		offset += Integer.SIZE/8;
		System.arraycopy(clientMsg.AwsConvertIntToByte(clientMsg.seqNum), 0, brokerToAppReplyMsg,
				offset, Integer.SIZE/8);
		offset += Integer.SIZE/8;
		System.arraycopy(payloadLen, 0, brokerToAppReplyMsg,
				offset, Integer.SIZE/8);
		offset += Integer.SIZE/8;
//		System.arraycopy(payloadReply, 0, brokerToAppReplyMsg,
//				offset, payloadReply.length);		
		
		this.AwsSendMsg(brokerToAppReplyMsg);
		try {
			this.clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		}
	}
	
	@Override
	public void run() {
		System.err.println("Client handler spawned!!");
		AwsReadClientSocket();
		System.err.println("Client Finished!!");
		AwsNetwork.DECR_USER_COUNT();
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}