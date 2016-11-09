package com.android.migrationmanager;

import java.util.ArrayList;
import java.util.List;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.CheckBox;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/* 
 * This is a migration management App.
 * Set status of applications installed to devices whether migratable or not.
 */
public class ManagerActivity extends Activity {

  static final boolean DEBUG = true;
  static final String TAG = "MIGRATOR MANAGER";

  //List<String> deviceNameList;

  private AppData ad;
  //private Drawable icon = null;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_manager);

    //deviceNameList = getMigratableDeviceList();
    //ArrayList<String> appList = new ArrayList<String>();
    ArrayList<AppData> appList = new ArrayList<AppData>();

    PackageManager pm = getPackageManager();
    //Query
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);

    List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);

    for (ResolveInfo info : list) {
      ad = new AppData();

      ad.setImage(info.loadIcon(pm));
      ad.setName(info.loadLabel(pm).toString());
      ad.setPackage(info.activityInfo.packageName);
      //ad.setText(info.activityInfo.loadLabel(pm).toString());
      appList.add(ad);
    }

    //List<ApplicationInfo> appinfo = pm.getInstalledApplications(PackageManager.GET_META_DATA);
    //for (ApplicationInfo info : appinfo) {
    ////if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)
    ////continue;
    //if (info.packageName.equals(this.getPackageName()))
    //continue;
    //ad = new AppData();
    //ad.setText(pm.getApplicationLabel(info).toString());
    //try{
    //icon = pm.getApplicationIcon(info.packageName);
    //}catch(Exception e){
    //e.printStackTrace();
    //}
    //ad.setImage(icon);
    //
    //appList.add(ad);
    //}

    CustomAdapter adapter = new CustomAdapter(this,
        android.R.layout.simple_list_item_multiple_choice, appList);
    ListView listView = (ListView) findViewById(R.id.listView1);
    listView.setAdapter(adapter);

    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
        ListView list = (ListView) parent;
        CheckBox chk = (CheckBox) view.findViewById(R.id.checkBox);
        chk.setChecked(!chk.isChecked());
        SparseBooleanArray checkedItemPositions = list.getCheckedItemPositions();
        AppData data = (AppData) list.getItemAtPosition(position);
        String selected = data.getName();
        if (DEBUG)
          Log.w(TAG, "Selected Item: " + selected + " Check: " + String.valueOf(checkedItemPositions.get(position)));
        //startActivity(new Intent(ManagerActivity.this,DeviceListDialog.class));
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.manager, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
