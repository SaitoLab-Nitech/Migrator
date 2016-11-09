package com.android.migrationmanager;

import java.util.ArrayList;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;

/*
 * For migration test without selecting devices.
 */
public class TestMode extends Activity {

  static final String KEY = "DEVICELIST";
  static final String TAG = "MIGRATOR DIALOG";
  private TransactionModule tm;

  @Override
  protected void onCreate(Bundle savedBundle) {
    super.onCreate(savedBundle);
    setContentView(R.layout.activity_manager);

    tm = new TransactionModule(this);
    //Log.w(TAG,"Dialog Activity");
    ArrayList<String> list = (ArrayList<String>) tm.getMigratableDeviceList();
    if(list != null) {
      tm.execMigration(0);
    }
    setResult(RESULT_OK);
    finish();
  }
}
