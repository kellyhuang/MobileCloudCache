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

public class AwsAppRegistrationActivity extends Activity implements OnClickListener {
	
	private static final String TAG = "RegistrationActivity";
	private static final int	AWS_APP_REG_ERROR_DIALOG = 1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /* Set View to register.xml */
        setContentView(R.layout.registration);
 
        Button register = (Button) findViewById(R.id.registerBtn);
        register.setOnClickListener(this);
        
        TextView login = (TextView) findViewById(R.id.loginLink);
        login.setOnClickListener(this);
    }

	public void onClick(View v) {

		if (v.getId() == R.id.registerBtn) {
			TextView unameField = (TextView) findViewById(R.id.reg_username);
			String uname = unameField.getText().toString();
			EditText emailField = (EditText) findViewById(R.id.reg_email);
			String email = emailField.getText().toString();
			EditText pwdField = (EditText) findViewById(R.id.reg_password);
			String pwd = pwdField.getText().toString();
			Log.d(TAG, "uname: "+uname+" email: "+email+" pwd: "+pwd);

			AwsAppMsgHandler handler = new AwsAppMsgHandler(uname, pwd, email);
			InputStream in = null;
			OutputStream out = null;
			handler.init(AwsAppUtils.AWS_NAME_SERVER_HOSTNAME, AwsAppUtils.AWS_NAME_SERVER_PORT);
			try {
				out = handler.clientSocket.getOutputStream();
				in 	= handler.clientSocket.getInputStream();
			} catch (IOException e) {
				Log.d(TAG, "Unable to get Stream from the Socket");
			}
			
			byte[] regMsg = handler.PrepareMsg(AwsAppBrokerIntfMsg.awsUpdMsgType.
								AWS_UPD_MSG_USER_REG_REQ, null, null);
			try {
				out.write(regMsg);
			} catch (IOException e) {
				Log.d(TAG, "Unable to write to the Socket!");
				e.printStackTrace();
			}
			
			/* wait for reply from broker */
			int 		msgId = 0;
			int 		seqNum = 0;
			byte[] payload = null;
			payload = handler.AwsReadSocketStream(in, msgId, seqNum, payload);
			
			/* from payload decode response code and redirect accordingly */
			int ecode = AwsAppUtils.AwsAppConvertByteToInt(payload);
			if (ecode == awsAppMsgErrCode.AWS_REG_MSG_REG_SUCCESS.getValue()) {
				/* Switch to Login view */
	            Intent i = new Intent(getApplicationContext(), AwsAppLoginActivity.class);
	            startActivity(i);
			}
			else if (ecode == awsAppMsgErrCode.AWS_REG_MSG_USERNAME_REGISTERED.getValue()) {
				onCreateDialog(AWS_APP_REG_ERROR_DIALOG);
				unameField.setText("");
				emailField.setText("");
				pwdField.setText("");
			}
		}
		
		if (v.getId() == R.id.loginLink) {
			/* Switch to Login view */
            Intent i = new Intent(getApplicationContext(), AwsAppLoginActivity.class);
            startActivity(i);
		}
	}
	
	
	public Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		switch (id) {
		case AWS_APP_REG_ERROR_DIALOG:
			AlertDialog.Builder builder = new Builder(this);
			builder.setTitle("Registration Error!");
			builder.setMessage("User Name already taken! Select a different name.")
		       .setCancelable(false)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		           }
		       });
			builder.show();
			dialog = builder.create();
			break;
		}
		return dialog;
	}

}
