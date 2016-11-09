package com.android.migrationmanager;

import android.content.Context;
import android.os.IMigratorService;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class TransactionModule {
  private IMigratorService mMigrator;
  private Context context;

  public TransactionModule(Context context) {
    this.context = context;
  }

  void execMigration(int which){
      if(mMigrator == null){
        mMigrator = (IMigratorService) context.getSystemService("Migrator");
      }

      try {
        mMigrator.execMigrate(which);
      } catch (RemoteException e) {
        Log.w("MIGRATOR","failed exec");
        Log.w("MIGRATOR",e.toString());
      }
    }

  List<String> getMigratableDeviceList() {
      List<String> result = null;
      if(mMigrator == null){
        mMigrator = (IMigratorService) context.getSystemService("Migrator");
      }

      if(mMigrator == null) {
        List<String> tmp = new ArrayList<String>();
        tmp.add("null");
        return tmp;
      }

      try {
        result = mMigrator.getDeviceList();
      } catch (RemoteException e) {
        Log.w("MIGRATOR","migrater");
      }
      return result;
    }
}
