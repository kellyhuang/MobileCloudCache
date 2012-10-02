package edu.uci.mobileUpload;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class FileUploadService extends IntentService{
	NotificationManager notificationManager;

	private final String TAG = "FileUploadService";

	public FileUploadService() {
		super("FileUploadService");
	}


	protected void onHandleIntent(Intent intent) {
		
		UploadDetails updDetails = UploadDetails.unpackBundle(intent.getBundleExtra("UploadDetailsBundle"));
		FileInputStream fileIn = null;

		Log.d("In service ", "Inside service");
		
		/* configure the intent */
		Intent serviceIntent = new Intent(this, FileUploadService.class);
		PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, serviceIntent, 0);
		
		Notification notification = new Notification(R.drawable.icon, "notify", System.currentTimeMillis());		
	
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progress);
		notification.contentIntent = pending;
		notification.contentView.setTextViewText(R.id.fileprogress, updDetails.awsFileUpdName);


		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);

		
		try{
			
			fileIn = new FileInputStream(updDetails.awsFileUpdPath);
		}catch(IOException e){
			Log.d(TAG, "IOException while opening the file " + updDetails.awsFileUpdPath.toString());
		}
		
		AwsAppMsgHandler handler = new AwsAppMsgHandler(AwsAppUtils.awsAppUserName,AwsAppUtils.awsAppUserPwd,null);
		InputStream in 		= null;
		OutputStream out	= null;
		handler.init(AwsAppUtils.AWS_NAME_SERVER_HOSTNAME, AwsAppUtils.AWS_NAME_SERVER_PORT);
		if (handler.clientSocket != null && handler.clientSocket.isConnected()) {
			try {
				out = handler.clientSocket.getOutputStream();
				in 	= handler.clientSocket.getInputStream();
			} catch (IOException e) {
				Log.d(TAG, "Socket Stream error" + e.getMessage());
			}
						
			byte[] uploadMsg = handler.PrepareMsg(AwsAppBrokerIntfMsg.awsUpdMsgType.
								AWS_UPD_MSG_UPLOAD_REQ, fileIn, updDetails);

			
			notification.contentView.setProgressBar(R.id.progress, 100 , 25, false);
	        notificationManager.notify(42, notification);

			try {
					out.write(uploadMsg);//, bytesWritten, 100);
			} catch (IOException e) {
				Log.d(TAG, "Socket write error!" + e.getMessage());
				e.printStackTrace();
			}
			
			notification.contentView.setProgressBar(R.id.progress, 100 , 50, false);
	        notificationManager.notify(42, notification);

			/* Waiting for reply */
			AwsAppBrokerIntfMsg replyHeader = new AwsAppBrokerIntfMsg();
			replyHeader.AwsDecodeAppBrokerMsg(in);	
			
			ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();		
			byte[] buf = new byte[replyHeader.payloadLen];

			int bytesRead = 0;
			int bytesToRead = replyHeader.payloadLen;
			int offset = 0;

			while (0 != bytesToRead) {
				try {
					
					bytesRead = in.read(buf);
				} catch (IOException e) {
					e.printStackTrace();
				} 
				inputBytes.write(buf, offset, bytesRead);
				bytesToRead -= bytesRead;
				offset += bytesRead;
				notification.contentView.setProgressBar(R.id.progress, 100 , 75, false);
		        notificationManager.notify(42, notification);
			}
			
			int num = replyHeader.AwsConvertByteToInt(inputBytes.toByteArray());

			if(num == AwsAppUtils.awsAppMsgErrCode.AWS_UPD_MSG_SUCCESS.ordinal())
			{
				notification.contentView.setProgressBar(R.id.progress, 100 , 100, false);
		        notificationManager.notify(42, notification);
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
			}
			
			try {
				handler.clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
