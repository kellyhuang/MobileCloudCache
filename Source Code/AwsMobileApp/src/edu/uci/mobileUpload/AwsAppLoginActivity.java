package edu.uci.mobileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.uci.mobileUpload.AwsAppUtils.awsAppMsgErrCode;


public class AwsAppLoginActivity extends Activity implements OnClickListener {

	private static final String TAG = "LoginActivityInfo";
	private static final int	AWS_APP_PWD_MISMATCH_DAILOG = 1;
	private static final int	AWS_APP_USER_NOT_REGISTER_DAILOG = 2;
	private static final int	AWS_APP_NO_SERVER_DAILOG = 3;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        Button login = (Button) findViewById(R.id.loginBtn);
        login.setOnClickListener(this);
        
        TextView signup = (android.widget.TextView) findViewById(R.id.signupLink);
        signup.setOnClickListener(this);
    }

	public void onClick(View v) {
		
		if (v.getId() == R.id.loginBtn) {
			TextView username = (TextView) findViewById(R.id.username);
			String uname = username.getText().toString();
			EditText password= (EditText) findViewById(R.id.password);
			String pwd = password.getText().toString();
			System.err.println("uname: "+uname+" pwd: "+pwd);

			AwsAppMsgHandler handler = new AwsAppMsgHandler(uname, pwd, null);
			InputStream in = null;
			OutputStream out = null;
			if (-1 == handler.init(AwsAppUtils.AWS_NAME_SERVER_HOSTNAME,
					AwsAppUtils.AWS_NAME_SERVER_PORT)) {
				onCreateDialog(AWS_APP_NO_SERVER_DAILOG);
				return;
			}
			try {
				out = handler.clientSocket.getOutputStream();
				in 	= handler.clientSocket.getInputStream();
			} catch (IOException e) {
				Log.d(TAG, "Unable to get Stream from the Socket");
			}
			
			byte[] loginMsg = handler.PrepareMsg(AwsAppBrokerIntfMsg.awsUpdMsgType.
								AWS_UPD_MSG_USER_LOGIN_REQ, null, null);
			try {
				out.write(loginMsg);
			} catch (IOException e) {
				Log.d(TAG, "Unable to write to the Socket!");
				e.printStackTrace();
			}
			
			/* wait for reply from broker */
			int msgId = 0;
			int seqNum = 0;
			byte[] payload = null;
			payload = handler.AwsReadSocketStream(in, msgId, seqNum, payload);
			
			/* from payload decode response code and redirect accordingly */
			int ecode = AwsAppUtils.AwsAppConvertByteToInt(payload);
			if (ecode == awsAppMsgErrCode.AWS_REG_MSG_AUTH_SUCCESS.getValue()) {
				/* store user info */
				AwsAppUtils.AwsAppStoreUserInfo(uname, pwd);
				/* Switch to Upload view */
	            Intent i = new Intent(getApplicationContext(), AwsAppUploadActivity.class);
	            startActivity(i);
			}
			else if (ecode == awsAppMsgErrCode.AWS_REG_MSG_PASSD_MISMATCH.getValue()) {
				onCreateDialog(AWS_APP_PWD_MISMATCH_DAILOG);
				username.setText("");
				password.setText("");
			}
			else if (ecode == awsAppMsgErrCode.AWS_REG_MSG_NOT_REGISTERED.getValue()) {
				onCreateDialog(AWS_APP_USER_NOT_REGISTER_DAILOG);
				username.setText("");
				password.setText("");
			}
		}
		
		if (v.getId() == R.id.signupLink) {
			/* Switch to Register view */
            Intent i = new Intent(getApplicationContext(), AwsAppRegistrationActivity.class);
        	startActivity(i);
        }
	}
	
	public Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(this);
		
		switch (id) {
		case AWS_APP_PWD_MISMATCH_DAILOG:
			builder.setTitle("Login Error!");
			builder.setMessage("Invalid Password!")
		       .setCancelable(false)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		           }
		       });
			break;
		case AWS_APP_USER_NOT_REGISTER_DAILOG:
			builder.setTitle("Login Error!");
			builder.setMessage("Invalid User Name!")
		       .setCancelable(false)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		           }
		       });
			break;
		case AWS_APP_NO_SERVER_DAILOG:
			builder.setTitle("Server Error!");
			builder.setMessage("Unable to establish connection to server. Please try again!")
		       .setCancelable(false)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		           }
		       });
			break;
		}
		builder.show();
		dialog = builder.create();
		return dialog;
	}
	
	
}