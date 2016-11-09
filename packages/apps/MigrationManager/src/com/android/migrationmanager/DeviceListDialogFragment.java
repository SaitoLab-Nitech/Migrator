package com.android.migrationmanager;

//import java.util.ArrayList;
import java.util.List;

//import com.android.migratermanager.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;

public class DeviceListDialogFragment extends DialogFragment {

	protected static final String TAG = "MIGRATOR DIALOG";
	protected static final String KEY = "DEVICELIST";
	private DeviceListDialog dld;

	protected static DeviceListDialogFragment newInstance(Bundle args) {
		DeviceListDialogFragment fragment = new DeviceListDialogFragment();
		fragment.setArguments(args);

		return fragment;
	}

	public void onAttach(Activity activity){
		super.onAttach(activity);

		if(activity instanceof DeviceListDialog == false){
			throw new UnsupportedOperationException("Unsupported Operation");
		}
		dld = (DeviceListDialog)activity;
	}

	public void onDetach(){
		super.onDetach();
		dld = null;
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {

		String titleString = getResources().getString(R.string.dialog_title);
		// List<String> deviceList = getMigratableDeviceList();
		List<String> deviceList = getArguments().getStringArrayList(KEY);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setTitle(titleString).setItems(
				deviceList.toArray(new CharSequence[deviceList.size()]),
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Log.w(TAG, "device was selected");
						dld.selectDevice(which);
					}
				});
		Dialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(true);
//		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//			@Override
//			public void onCancel(DialogInterface dialog) {
//				// TODO Auto-generated method stub
//				Log.w(TAG, "cancel");
//			}
//		});
//		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//			@Override
//			public void onDismiss(DialogInterface dialog) {
//				// TODO Auto-generated method stub
//				Log.w(TAG, "dismiss");
//			}
//		});

		return dialog;
	}

	public void onStop() {
		super.onStop();
		getActivity().finish();
	}
}
