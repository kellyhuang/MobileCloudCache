package edu.uci.mobileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.ExpandableListActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

public class AwsAppGetFileListActivity  extends ExpandableListActivity implements OnClickListener {

	private static final String TAG = "GetFileListActivityInfo";
    
	ExpandableListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up our adapter
        mAdapter = new MyExpandableListAdapter();
        AwsAppGetFileList();
        setListAdapter(mAdapter);
        registerForContextMenu(getExpandableListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("List of Files");
        menu.add(0, 0, 0, "expandable_list_sample_action");
    }
	
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();

        String title = ((TextView)info.targetView).getText().toString();

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 
            int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition); 
            Toast.makeText(this, title + ": Child " + childPos + " clicked in group " + groupPos,
                    Toast.LENGTH_SHORT).show();
            return true;
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 
            Toast.makeText(this, title + ": Group " + groupPos + " clicked", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }
    
    /**
     * A simple adapter which maintains an ArrayList of photo resource Ids. 
     * Each photo is displayed as an image. This adapter supports clearing the
     * list of photos and adding a new photo.
     *
     */
    public class MyExpandableListAdapter extends BaseExpandableListAdapter {
        // Sample data set.  children[i] contains the children (String[]) for groups[i].
        String[] awsAppFileGroups = { "Pictures", "Audio Files", "Video Files", "Data Files" };
        String[][] awsAppFileGroupEntry = {{}, {}, {}, {}};
        
        public Object getChild(int groupPosition, int childPosition) {
            return awsAppFileGroupEntry[groupPosition][childPosition];
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            return awsAppFileGroupEntry[groupPosition].length;
        }

        public TextView getGenericView() {
            // Layout parameters for the ExpandableListView
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 32);
            

            TextView textView = new TextView(AwsAppGetFileListActivity.this);
            textView.setLayoutParams(lp);
            textView.setBackgroundColor(Color.WHITE);
            textView.setTextColor(Color.BLACK);
            // Center the text vertically
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            // Set the text starting position
            textView.setPadding(36, 0, 0, 0);
            return textView;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            TextView textView = getGenericView();
            textView.setBackgroundColor(Color.LTGRAY);
            textView.setText(getChild(groupPosition, childPosition).toString());
            return textView;
        }

        public Object getGroup(int groupPosition) {
            return awsAppFileGroups[groupPosition];
        }

        public int getGroupCount() {
            return awsAppFileGroups.length;
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            TextView textView = getGenericView();
            textView.setText(getGroup(groupPosition).toString());
            return textView;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }

    }
    
    void decodeGetListReply(String input){
		/* <length> <image count> <audio count> <video count> <data count>
		 *  <String of all filenames separated by space> 
		 *  */ 
		int offset = 0;
		/* Find count for each file type */
		int imageCount = AwsAppUtils.AwsAppConvertByteToInt(input.substring(offset,
				offset + 4).getBytes());
		offset += Integer.SIZE/8;
		int audioCount = AwsAppUtils.AwsAppConvertByteToInt(input.substring(offset,
				offset + 4).getBytes());
		offset += Integer.SIZE/8;
		int videoCount = AwsAppUtils.AwsAppConvertByteToInt(input.substring(offset,
				offset + 4).getBytes());
		offset += Integer.SIZE/8;
		int dataCount = AwsAppUtils.AwsAppConvertByteToInt(input.substring(offset,
				offset + 4).getBytes());
		offset += Integer.SIZE/8;

		/* find length of filenames and extract filename */
		int tempLength = AwsAppUtils.AwsAppConvertByteToInt(input.substring(offset, offset + 4).getBytes());
		offset += Integer.SIZE/8;
		String fileString = input.substring(offset, offset + tempLength);
		String [] fileNames = fileString.split("\n");

		/* traverse array and put file in their group */
		MyExpandableListAdapter listAdapter = (MyExpandableListAdapter)mAdapter;
		listAdapter.awsAppFileGroupEntry[AwsAppBrokerIntfMsg.GET_LIST_IMAGE_CODE] = new String[imageCount];
		listAdapter.awsAppFileGroupEntry[AwsAppBrokerIntfMsg.GET_LIST_AUDIO_CODE] = new String[audioCount];
		listAdapter.awsAppFileGroupEntry[AwsAppBrokerIntfMsg.GET_LIST_VIDEO_CODE] = new String[videoCount];
		listAdapter.awsAppFileGroupEntry[AwsAppBrokerIntfMsg.GET_LIST_DATA_FILE_CODE] = new String[dataCount];
		
		imageCount = audioCount = videoCount = dataCount = 0;
		for(int i=0; i<fileNames.length; i++) {
			int fileType = Integer.parseInt(fileNames[i].substring(0, 
					fileNames[i].indexOf("#")));
			String filename = fileNames[i].substring(fileNames[i].indexOf("#")+1);
			switch (fileType) {
			case AwsAppBrokerIntfMsg.GET_LIST_IMAGE_CODE:
				listAdapter.awsAppFileGroupEntry[
				    AwsAppBrokerIntfMsg.GET_LIST_IMAGE_CODE][imageCount] = filename;
				imageCount += 1;
				break;
			case AwsAppBrokerIntfMsg.GET_LIST_AUDIO_CODE:
				listAdapter.awsAppFileGroupEntry[
				    AwsAppBrokerIntfMsg.GET_LIST_AUDIO_CODE][audioCount] = filename;
				audioCount += 1;
				break;
			case AwsAppBrokerIntfMsg.GET_LIST_VIDEO_CODE:
				listAdapter.awsAppFileGroupEntry[
				    AwsAppBrokerIntfMsg.GET_LIST_VIDEO_CODE][videoCount] = filename;
				videoCount += 1;
				break;
			case AwsAppBrokerIntfMsg.GET_LIST_DATA_FILE_CODE:
				listAdapter.awsAppFileGroupEntry[
				    AwsAppBrokerIntfMsg.GET_LIST_DATA_FILE_CODE][dataCount] = filename;
				dataCount += 1;
				break;
			}
		}
	}
    
    private void AwsAppGetFileList()
    {
    	AwsAppMsgHandler handler = new AwsAppMsgHandler(AwsAppUtils.awsAppUserName,
    									AwsAppUtils.awsAppUserPwd, AwsAppUtils.awsAppUserEmail);
    	
		InputStream in = null;
		OutputStream out = null;
		handler.init(AwsAppUtils.AWS_NAME_SERVER_HOSTNAME, AwsAppUtils.AWS_NAME_SERVER_PORT);
		if (handler.clientSocket != null && handler.clientSocket.isConnected()) {
			try {
				out = handler.clientSocket.getOutputStream();
				in 	= handler.clientSocket.getInputStream();
			} catch (IOException e) {
				Log.d(TAG, "Socket Stream error" + e.getMessage());
			}
			
			byte[] uploadMsg = handler.PrepareMsg(AwsAppBrokerIntfMsg.awsUpdMsgType.
								AWS_UPD_MSG_GET_OBJ_LIST_REQ, null, null);
			try {
				out.write(uploadMsg);
			} catch (IOException e) {
				Log.d(TAG, "Socket write error!" + e.getMessage());
			}
		}
		
		/* wait for reply from broker */
		int 		msgId = 0;
		int 		seqNum = 0;
		byte[] 		payload = null;
		payload = handler.AwsReadSocketStream(in, msgId, seqNum, payload);
		decodeGetListReply(new String(payload));
		
		try {
			handler.clientSocket.close();
		} catch (IOException e) {
			Log.d(TAG, "Socket IO Exception!" + e.getMessage());
			e.printStackTrace();
		}
    }
	public void onClick(View v) {
		
	}
}
    
