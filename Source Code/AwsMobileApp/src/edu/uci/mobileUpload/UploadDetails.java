package edu.uci.mobileUpload;

import java.io.File;

import android.content.Context;
import android.os.Bundle;

public class UploadDetails {
	int 						awsFileUpdPriority;
	int							awsFileUpdSenstivity;
	int							awsFileUpdDeadline;
	File						awsFileUpdPath;
	int							awsFileUpdSize;
	String 						awsFileUpdName;
	Context 					context;
	
	public Bundle getBundle()
	{
		Bundle bundle = new Bundle();
		bundle.putInt("awsFileUpdPriority", this.awsFileUpdPriority);
		bundle.putInt("awsFileUpdSenstivity", this.awsFileUpdSenstivity);
		bundle.putInt("awsFileUpdDeadline", this.awsFileUpdDeadline);
		bundle.putString("awsFileUpdPath", this.awsFileUpdPath.toString());
		bundle.putInt("awsFileUpdSize", this.awsFileUpdSize);
		bundle.putString("awsFileUpdName", this.awsFileUpdName);
		return bundle;
	}
	
	public static UploadDetails unpackBundle(Bundle bundle)
	{
		UploadDetails updDetails = new UploadDetails();
		updDetails.awsFileUpdPriority   = bundle.getInt("awsFileUpdPriority");
		updDetails.awsFileUpdDeadline   = bundle.getInt("awsFileUpdDeadline");
		updDetails.awsFileUpdSenstivity = bundle.getInt("awsFileUpdSenstivity");

		updDetails.awsFileUpdName = bundle.getString("awsFileUpdName");
		updDetails.awsFileUpdSize = bundle.getInt("awsFileUpdSize");
		String filename = bundle.getString("awsFileUpdPath");
		updDetails.awsFileUpdPath = new File(filename);
		return updDetails;
	}
}
