package edu.uci.mobileUpload;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AwsAppUploadActivity extends Activity implements OnClickListener, android.content.DialogInterface.OnClickListener{

	private static final String TAG = "UploadActivity";
	private final static int	AWS_APP_FILE_SELECT_DIALOG 		= 1;
	private final static int	AWS_APP_FILE_PRIOIRTY_DIALOG 	= 2;
	private final static int	AWS_APP_FILE_SENSTIVITY_DIALOG 	= 3;
	private final static int	AWS_APP_FILE_DEADLINE_DIALOG 	= 4;
	private final static int	AWS_APP_FINAL_UPD_DIALOG		= 5;
	private String[] 			mFileList;
	public File 				mPath;
	public String 				mChosenFile;
	UploadDetails updDetails = new UploadDetails();

	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload);

		mFileList = new String[100];
		
		TextView t= (TextView)findViewById(R.id.welcomeMsg);
		t.setText("Welcome "+ AwsAppUtils.awsAppUserName+"!");

		Button upload = (Button) findViewById(R.id.upload);
		upload.setOnClickListener(this);
		
		Button browse = (Button) findViewById(R.id.browse);
		browse.setOnClickListener(this);

	}

	public void onClick(View v) {
		if (v.getId() == R.id.browse) {
			boolean mExternalStorageAvailable = false;
			String state = Environment.getExternalStorageState();

			if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			    mExternalStorageAvailable = true;
			} else {
			    mExternalStorageAvailable = false;
			}
			
			if (mExternalStorageAvailable) {
				System.out.println(Environment.getExternalStorageDirectory().toString());
				Log.d(TAG, Environment.getExternalStorageDirectory().toString());
			
				mPath = new File(Environment.getExternalStorageDirectory().toString());			
				Log.d(TAG, mPath.toString());
			    mFileList = mPath.list();
				onCreateDialog(AWS_APP_FILE_SELECT_DIALOG); 
			}	
		}
		else if(v.getId() == R.id.upload) {
			/* Switch to File List view */
            Intent i = new Intent(getApplicationContext(), AwsAppGetFileListActivity.class);
            startActivity(i);
		}
	}
	
	public class DialogButtonClickHandler implements DialogInterface.OnClickListener
	{	
        public UploadDetails updDetails;
		public DialogButtonClickHandler(UploadDetails updDetails) {
			this.updDetails = updDetails;
		}
		
		public DialogButtonClickHandler() {}

		public void onClick(DialogInterface dialog, int clicked ) {
			switch (clicked) {
				case DialogInterface.BUTTON_POSITIVE:
                    Intent serviceIntent = new Intent(getBaseContext(), FileUploadService.class);
					Bundle bundle = updDetails.getBundle();
					serviceIntent.putExtra("UploadDetailsBundle", bundle);
					startService(serviceIntent);
					break;
					
				case DialogInterface.BUTTON_NEGATIVE:
					Toast.makeText(getApplicationContext(), "File '" + mChosenFile + "' upload cancelled",
							Toast.LENGTH_SHORT).show();
					break;
			}
		}
	}

	public Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		
		switch (id) {
		case AWS_APP_FILE_SELECT_DIALOG:
			AlertDialog.Builder builder = new Builder(this);
			builder.setTitle("Choose your file");
			builder.setItems(mFileList, this); 		
			builder.show();
			dialog = builder.create();
			break;
			
		case AWS_APP_FILE_PRIOIRTY_DIALOG:
			final CharSequence[] priority = {"High", "Low"};
			AlertDialog.Builder builder2 = new Builder(this);
			builder2.setTitle("File Upload Priority");
			builder2.setSingleChoiceItems(priority, -1, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        Toast.makeText(getApplicationContext(), "Priority: " + priority[item], Toast.LENGTH_SHORT).show();
			        updDetails.awsFileUpdPriority = item;
			        onCreateDialog(AWS_APP_FILE_DEADLINE_DIALOG);
			        dialog.dismiss();
			    }
			});
			builder2.show();
			dialog = builder2.create();
			break;
			
		case AWS_APP_FILE_SENSTIVITY_DIALOG:
			final CharSequence[] senstivity = {"Normal", "Low"};
			AlertDialog.Builder builder3 = new Builder(this);
			builder3.setTitle("File Upload Sensitivity");
			builder3.setSingleChoiceItems(senstivity, -1, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        Toast.makeText(getApplicationContext(), "Senstivity: " + senstivity[item], Toast.LENGTH_SHORT).show();
			        updDetails.awsFileUpdSenstivity = item;
			        onCreateDialog(AWS_APP_FILE_PRIOIRTY_DIALOG);
			        dialog.dismiss();
			    }
			});
			builder3.show();
			dialog = builder3.create();
			break;
			
		case AWS_APP_FILE_DEADLINE_DIALOG:
			AlertDialog.Builder builder4 = new Builder(this);
			builder4.setTitle("File Upload Deadline");
			final CharSequence[] highDeadline = {"10 Mins", "30 Mins", "1 Hour"};
			final int[]	highDeadlineInt =  {10, 30, 60};
			final CharSequence[] lowDeadline = {"2 Hours", "3 Hours", "5 Hours", "EoD"};
			final int[]	lowDeadlineInt = {120, 180, 300, 360};
			if (updDetails.awsFileUpdPriority == AwsAppBrokerIntfMsg.awsUploadPriority.AWS_UPD_PRIORITY_HIGH.ordinal()) {
				builder4.setSingleChoiceItems(highDeadline, -1, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				        Toast.makeText(getApplicationContext(), "Deadline: " + highDeadline[item], Toast.LENGTH_SHORT).show();
				        updDetails.awsFileUpdDeadline = highDeadlineInt[item];
				        onCreateDialog(AWS_APP_FINAL_UPD_DIALOG);
				        dialog.dismiss();
				    }
				});
			}
			else {
				builder4.setSingleChoiceItems(lowDeadline, -1, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				        Toast.makeText(getApplicationContext(), "Deadline: " + lowDeadline[item], Toast.LENGTH_SHORT).show();
				        updDetails.awsFileUpdDeadline = lowDeadlineInt[item];
				        onCreateDialog(AWS_APP_FINAL_UPD_DIALOG);
				        dialog.dismiss();
				    }
				});
			}
			
			builder4.show();
			dialog = builder4.create();
			break;
			
		case AWS_APP_FINAL_UPD_DIALOG:
			final CharSequence[] details = {"File Senstivity: " + ((updDetails.awsFileUpdSenstivity == 0) ? "Normal" : "Low"),
											"Upload Priority: " + ((updDetails.awsFileUpdPriority== 0) ? "High" : "Low"), 
											"Upload Deadline: " + ((updDetails.awsFileUpdDeadline/60 == 0) ? updDetails.awsFileUpdDeadline + "mins" : updDetails.awsFileUpdDeadline/60 + "Hr(s)")};
			AlertDialog.Builder builder5 = new Builder(this);
			builder5.setTitle("File: "+this.mChosenFile);
			builder5.setItems(details, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	/* do nothing */
			    }
			});
            /*
			 * Update the variables to be passed to the AsyncTask
			 */
			updDetails.awsFileUpdPath = this.mPath;
			updDetails.awsFileUpdSize = (int)this.mPath.length();
			updDetails.awsFileUpdName = this.mChosenFile;
			updDetails.context		  = getApplicationContext();
			builder5.setPositiveButton("Upload", new DialogButtonClickHandler(updDetails));
			builder5.setNegativeButton("Cancel", new DialogButtonClickHandler());
			builder5.show();
			dialog = builder5.create();
			break;
		}
		return dialog;
	}
	
	public void onClick(DialogInterface dialog, int which) {
		Toast.makeText(getApplicationContext(), mFileList[which], Toast.LENGTH_LONG).show();
		Log.d(TAG, Environment.getExternalStorageDirectory().toString() + mFileList[which].toString());
		try {
			mPath = new File(mPath.getCanonicalPath() + "//" + mFileList[which]);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (mPath.isDirectory()) {
			mFileList = mPath.list();
			if (mFileList != null && mFileList.length != 0) {
				onCreateDialog(AWS_APP_FILE_SELECT_DIALOG);
			}
		}
		else if (mPath.isFile())
		{
			Toast.makeText(getApplicationContext(), "Yay, I have finally managed to get a file", Toast.LENGTH_LONG);
			Log.d(TAG, "Yay, I have finally managed to get a file");
			this.mChosenFile = mPath.getName();
			onCreateDialog(AWS_APP_FILE_SENSTIVITY_DIALOG);
		}
	}
}
