package com.android.migrationmanager;

import java.util.ArrayList;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;

public class DeviceListDialog extends Activity {

  static final String KEY = "DEVICELIST";
  static final String TAG = "MIGRATOR DIALOG";
  private TransactionModule tm;

  @Override
  protected void onCreate(Bundle savedBundle) {
    super.onCreate(savedBundle);
    //Log.w(TAG,"Dialog Activity");

    tm = new TransactionModule(this);

    Bundle list = new Bundle();
    list.putStringArrayList(KEY,
        (ArrayList<String>) tm.getMigratableDeviceList());
    FragmentManager manager = getFragmentManager();
    DeviceListDialogFragment dialog = DeviceListDialogFragment
      .newInstance(list);
    dialog.show(manager, "dialog");
  }

  protected void selectDevice(int which){
    //Log.w(TAG,"selected device id: " + String.valueOf(which));
    if(tm == null) {
      tm = new TransactionModule(this);
    }
    tm.execMigration(which);
  }
}
