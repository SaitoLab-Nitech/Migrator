/* Migrator Service
 * frameworks/base/services/java/com/android/server/MigratorService.java */

/*
 * MIT License
 *
 * Copyright (c) 2016 SaitoLab-Nitech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * A Service on Android System for App migration
 * @author Kohei Sato at Saito Lab. in Nagoya Institute of Technology
 */

package com.android.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IMigratorService;
import android.os.Looper;
import android.os.Parcel;
import android.os.UserHandle;
import android.util.Base64;
import android.util.Log;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MigratorService extends IMigratorService.Stub {
  private static final UUID MIGRATE_UID = UUID.nameUUIDFromBytes("MIGRATOR BY SSN".getBytes());
  private static final String TMP_DIR = "/tmp/";
  private Context mContext;
  private BluetoothAdapter mAdapter;
  private MigrateWorkerThread mWorker;

  /* objects to send */
  private Bundle targetBundle;
  private ArrayList<String> targetFiles;
  private ArrayList<String> targetFileBody;
  /* list of bundles for stacked Activity */
  private ArrayList<Bundle> stackedArray;


  /* Bluetooth device is available or not. (true is supported) */
  private boolean supported = true;
  /* List of device name that can be migrated. */
  private ArrayList<String> devicelist;
  /* List of BluetoothDevice that can be migrated. */
  private ArrayList<BluetoothDevice> devices;

  //for debugging
  private static final boolean debug = false;
  private static final String TAG = "MIGRATOR SERVICE";

  /* list of Apps not to migrate */
  //TODO persistent blackList
  private static Map<String, Boolean> blackList;
  /* UID that calls this Service */
  private int callingUid;
  /* package name that calls this Service */
  private String mAppName;

  /**
   * This constructor is called by SystemServer
   * @hide
   */
  public MigratorService(Context context){
    super();
    mContext = context;
    mAdapter = BluetoothAdapter.getDefaultAdapter();
    if(mAdapter != null) {
      mWorker = new MigrateWorkerThread();
      mWorker.start();
      devices = new ArrayList<BluetoothDevice>();
      blackList = new HashMap<String, Boolean>();
      setDafaultList();
      makeDeviceList();
      if(debug) Log.w(TAG, "worker started!");
    } else {
      supported = false;
      Log.w(TAG, "Bluetooth is not supported!");
    }
  }

  /**
   * Call from Context that you want to migrate.
   * @hide
   * @param target execution states to transmit
   * @param pname a package name of migrating App
   * @param cname a Class name of migrating Activity
   * @param flag if true, an Activity calls next Activity
   * @return true if migration successes
   */
  public boolean migrate(Bundle target, String pname, String cname, boolean flag){
    if(debug) Log.w(TAG, "migrate start!");
    if(supported) {
      if(checkMigratableApp()) {
        if(target != null && pname != null && cname != null) {
          targetBundle = target;
          targetBundle.putCharSequence("MIGRATED PNAME", pname);
          targetBundle.putCharSequence("MIGRATED CNAME", cname);
          if(!flag) {
            if(stackedArray != null) {
              /* if stackedArray exists, this processing is called by second or later Activity */
              stackedArray.add(targetBundle);
            }
          } else {
            /* migrate stacked Activity */
            if(stackedArray == null)
              stackedArray = new ArrayList<Bundle>();
            stackedArray.add(targetBundle);
          }
          return true;
        } else {
          return false;
        }
      } else {
        Log.w(TAG, "This App is set status as false, uid : " + String.valueOf(callingUid));
        return false;
      }
    } else {
      Log.w(TAG, "Not Support or system App");
      return false;
    }
  }


  /**
   * This method executes migration for Activity using Bluetooth.
   * Called by the Activity which shows device list dialog.
   * @hide
   * @param which A number of destination device showed in the dialog (>0).
   */
  public void execMigrate(int which){
    if(debug) Log.w(TAG, "Exec Migration");
    long base = System.nanoTime();
    connect(devices.get(which));
  }


  /**
   * Wipe black list
   * @hide
   */
  public void wipeList(){
    if(!blackList.isEmpty()) {
      blackList.clear();
      setDafaultList();
    }
  }

  /**
   * Check an Application before migration by calling this method.
   * @return true if the App can migrate
   */
  public boolean checkMigratableApp(){
    /* migrate allowed Apps */
    mAppName = getPackagename();

    /* exclude system Apps */
    if(callingUid <= 1001)
      return false;

    if(blackList.get(mAppName) == null) /* not listed */
      return true;
    else
      return blackList.get(mAppName);
  }

  /**
   * Set an Application status to the black list.
   * This is called from the helper App.
   * @hide
   * @param appName FQDN package name
   * @param status whether the App migrate or not
   */
  public void setStatus(String appName, boolean status){
    blackList.put(appName, status);
  }

  /**
   * @return whether the device supports Bluetooth or not
   */
  public boolean isAvailable(){
    return supported;
  }

  /**
   * @hide
   * @return device list for Dialog
   */
  public List<String> getDeviceList(){
    makeDeviceList();
    return devicelist;
  }

  /**
   * Set File paths for migrate
   * @hide
   * @param paths absolute file paths for migration
   */
  public void setFileNames(String[] paths) {
    targetFiles = new ArrayList<String>();
    Collections.addAll(targetFiles, paths);
  }

  /**
   * set file bodies to ArrayList<byte[]>
   * @hide
   * @param bodies the file bodies
   */
  public void setFileBody(List bodies) {
    ArrayList<byte[]> tmp = (ArrayList<byte[]>)bodies;
    targetFileBody = new ArrayList<String>();
    for(byte[] array:tmp) {
      String data = Base64.encodeToString(array, Base64.DEFAULT);
      targetFileBody.add(data);
    }
  }

  private void makeDeviceList(){
    /*get list of devices which can be migrated*/
    if(supported) {
      /*get paired devices*/
      Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
      devicelist = new ArrayList<String>();
      if(pairedDevices.size() > 0) {
        /*if paired device is exist*/
        for(BluetoothDevice device : pairedDevices){
          /* choice phone or tablet*/
          if(debug) Log.w(TAG, "device name:" + device.getName());
          int code = device.getBluetoothClass().getMajorDeviceClass();
          if(code == BluetoothClass.Device.Major.PHONE) {
            devicelist.add(device.getName() + "\n" + device.getAddress());
            devices.add(device);
          }
          else if(code == BluetoothClass.Device.Major.COMPUTER) {
            devicelist.add(device.getName() + "\n" + device.getAddress());
            devices.add(device);
          }
        }
      }
    }
  }

  /* at sender */
  private void connect(BluetoothDevice device){
    BluetoothSocket clientSocket;

    try{
      clientSocket = device.createRfcommSocketToServiceRecord(MIGRATE_UID);
    }catch(IOException e){
      return;
    }

    if(clientSocket != null) {
      if(mAdapter.isDiscovering()) {
        mAdapter.cancelDiscovery();
      }
      try {
        clientSocket.connect();
        if(debug) Log.w(TAG, "connect");
      }catch(IOException e){
        try{
          clientSocket.close();
        }catch(IOException e1){
          return;
        }
      }
      long base = System.nanoTime();
      write(clientSocket);
    } else {
      if(debug) Log.w(TAG, "socket is null");
    }
  }

  /* called by connect(BluetoothDevice device) */
  private void write(BluetoothSocket socket){
    RWModule rwm = new RWModule(socket);
    rwm.write(targetBundle);
    rwm.cancel();
    targetBundle = null;
  }

  private String getPackagename(){
    PackageManager pm = mContext.getPackageManager();
    callingUid = Binder.getCallingUid();
    String[] names = pm.getPackagesForUid(callingUid);
    if(names != null)
      return names[0];
    else return "FAILED";
  }

  /* invalid Apps */
  private void setDafaultList(){
    blackList.put("com.android.launcher", false);
    blackList.put("com.android.keyguard", false);
    blackList.put("com.android.systemui", false);
    blackList.put("com.android.settings", false);
    blackList.put("com.android.migrationmanager", false);
  }

  /* Migrator Server class */
  private class MigrateWorkerThread extends Thread {
    private BluetoothServerSocket servSock = null;
    private BluetoothServerSocket tmpServerSock = null;
    private BluetoothSocket tmpSock = null;

    public MigrateWorkerThread(){
      if(debug) Log.w(TAG, "Work thread start");
      if (mAdapter != null) { /*Bluetooth device is available*/
        if(mAdapter.isEnabled()) { /*Bluetooth is on */
          try {
            tmpServerSock = mAdapter.listenUsingRfcommWithServiceRecord("MigratorService", MIGRATE_UID);
            if(debug) Log.w(TAG, "get adapter, UUID:" + MIGRATE_UID.toString());
          } catch (IOException e) {
            if(debug) Log.w(TAG, "can't get servSock: " + e.toString());
            e.printStackTrace();
          }
          servSock = tmpServerSock; /*get listen socket*/
        } else {
          Log.w(TAG, "Bluetooth is disable.");
        }
      } else {
        if(debug) Log.w(TAG, "No adapter!");
        supported = false;
      }
    }

    public void run(){
      if(debug) Log.w(TAG, "listen start");
      BluetoothSocket recvSock = null;

      if(servSock == null) {
        while(!mAdapter.isEnabled()){
          /*wait until Bluetooth turns on*/
        }
        try {
          tmpServerSock = mAdapter.listenUsingRfcommWithServiceRecord("MigratorService", MIGRATE_UID);
          if(debug) Log.w(TAG, "get adapter, UUID:" + MIGRATE_UID.toString());
        } catch (IOException e) {
          if(debug) Log.w(TAG, "can't get servSock");
          e.printStackTrace();
        }
        servSock = tmpServerSock;
      }

      if(servSock != null) {
        while(true){
          /* listen */
          try{
            tmpSock = servSock.accept();
            if(debug) Log.w(TAG, "get Socket");
          }catch (IOException e){
            continue;
          }
          recvSock = tmpSock;

          if(recvSock != null) {
            RWModule rwm = new RWModule(recvSock);
            rwm.start();
            recvSock = null;
          }
        }
      }
    }
  }

  /* Read/Write module */
  private class RWModule extends Thread{
    private BluetoothSocket mSocket;

    public RWModule(BluetoothSocket sock){
      mSocket = sock;
    }


    /* Read files from /tmp in sender device*/
    private ArrayList<String> readFromTmp(ArrayList<String> paths) {
      ByteArrayOutputStream ous = null;
      InputStream ios = null;
      /* A list of file bodies */
      ArrayList<String> result = new ArrayList<String>();
      File f;
      for(String p: paths) {
        f = new File(TMP_DIR + p.replaceAll("/", "_")); /* make tmp path */
        try {
          if(debug) Log.w(TAG, "read from tmp: " + f.getAbsolutePath());
          byte[] buf = new byte[4096];
          ous = new ByteArrayOutputStream();
          ios = new BufferedInputStream(new FileInputStream(f));
          int readsize = 0;
          while ((readsize = ios.read(buf)) != -1) {
            ous.write(buf, 0, readsize);
          }
        } catch (FileNotFoundException e) {
          continue;
        } catch (IOException e) {
          e.printStackTrace();
          continue;
        } finally {
          if(!f.delete() && debug) Log.w(TAG, "cannot remove tmp file");
          try {
            if(ous != null)
              ous.close();
          } catch (IOException e) {
          }
          try {
            if(ios != null)
              ios.close();
          } catch (IOException e) {
          }
        }
        String data = Base64.encodeToString(ous.toByteArray(), Base64.DEFAULT); /* encode to Base64 before sending */
        if(debug) Log.w(TAG, "read from tmp: " + data);
        result.add(data);
      }
      return result;
    }

    /* Write files to /tmp in receiver device */
    private void writeToTmp(ArrayList<String> paths, ArrayList<String> bodies) {
      OutputStream ous = null;
      for(int i = 0; i < paths.size(); i++) {
        File f = new File(TMP_DIR + paths.get(i).replaceAll("/", "_"));
        byte[] buf = Base64.decode(bodies.get(i), Base64.DEFAULT);
        try {
          if(debug) Log.w(TAG, "write to tmp: " + f.getAbsolutePath());
          f.createNewFile();
        } catch (IOException e) {
          e.printStackTrace();
          if(debug) Log.w(TAG, "cannot create new File to tmp");
        } catch (SecurityException e) {
          if(debug) Log.w(TAG, "security error");
        }
        f.setReadable(true, false);
        f.setWritable(true, false);
        try {
          ous = new BufferedOutputStream(new FileOutputStream(f));
          ous.write(buf, 0, buf.length);
        } catch (FileNotFoundException e) {
          if(debug) Log.w(TAG, "destination file is not found");
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          try {
            if(ous != null)
              ous.close();
          }catch (IOException e) {}
        }
      }
    }

    /* send execution states to another device */
    public void write(Bundle bundle){
      Parcel p = Parcel.obtain();
      byte[] datasize = null;
      byte[] senddata = null;
      OutputStream out = null;
      BufferedOutputStream bos = null;

      try {
        out = mSocket.getOutputStream();
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      if(out != null) {
        bos = new BufferedOutputStream(out);
      }

      long base = System.nanoTime();
      long prev = System.nanoTime();
      if(stackedArray != null) {
        /* if it needs to send some Activities */
        bundle = new Bundle(); /* Bundle to send already exists in stackedArray */
        bundle.putParcelableArrayList("MIGRATED STACK", stackedArray);
        stackedArray = null;
      }
      if(targetFiles != null) {
        ArrayList<String> tmp = new ArrayList<String>(targetFiles);
        bundle.putStringArrayList("TARGET_FILE_NAME", tmp);
        if(targetFileBody == null) {
          targetFileBody = readFromTmp(tmp);
        }
        bundle.putStringArrayList("TARGET_FILE_BODY", targetFileBody);
        if(debug) Log.w(TAG, "set files!");
        targetFiles = null;
        targetFileBody = null;
      }
      prev = System.nanoTime();

      p.writeBundle(bundle); /* write to Parcel */
      senddata = p.marshall(); /* object to byte */
      p.recycle();

      ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
      datasize = buffer.putInt(senddata.length).array();
      prev = System.nanoTime();

      try {
        /* send data size */
        bos.write(datasize, 0, datasize.length);
        bos.flush();
      } catch (IOException e) {
        if(debug) Log.w(TAG, "on sending size");
        if(debug) Log.w(TAG, e.toString());
        try {
          bos.close();
        } catch (IOException e1) { }
        return;
      }
      try {
        /*send data*/
        bos.write(senddata, 0, senddata.length);
        bos.flush();
        if(debug) Log.w(TAG, "print bundle, size: " + String.valueOf(senddata.length));
      } catch (IOException e){
        if(debug) Log.w(TAG, "on sending data");
        if(debug) Log.w(TAG, e.toString());
      } finally {
        try {
          bos.close();
        } catch(IOException e) { }
      }
      cancel();
    }

    public void run(){ /* read */
      Looper.prepare();
      byte[] buf = new byte[1024];
      byte[] data = null;
      int readsize = 0;
      /* total size of receiving data */
      int totalsize = 0;
      /* current size in receiving */
      int progress = 0;
      ByteArrayBuffer ba = null;
      Parcel p = Parcel.obtain();

      InputStream in = null;
      BufferedInputStream bis = null;

      try{
        in = mSocket.getInputStream();
      }catch (IOException e){
        e.printStackTrace();
      }
      if(in != null) {
        bis = new BufferedInputStream(in);
      }

      long base = System.nanoTime();
      long prev = base;

      try{
        ba = new ByteArrayBuffer(bis.available());
      }catch (IOException e){
        if(debug) Log.w(TAG, "BufferedInputStream is not available");
      }
      if(ba == null) {
        if(debug) Log.w(TAG, "ByteArray is null");
        return;
      }

      /*read data size*/
      try{
        readsize = bis.read(buf);
      }catch(IOException e){
        if(debug) Log.w(TAG, e.toString());
      }

      if(readsize == Integer.SIZE / Byte.SIZE) {
        totalsize = ByteBuffer.wrap(buf).getInt();
      }

      if(debug) Log.w(TAG, "total size:" + String.valueOf(totalsize));

      /*read data body*/
      prev = System.nanoTime();
      while(totalsize > progress){
        try{
          readsize = bis.read(buf, 0, buf.length);
          ba.append(buf, 0, readsize);
          if(debug) Log.w(TAG, "read data, size:" + String.valueOf(readsize));
          progress += readsize;
        }catch(IOException e) {
          e.printStackTrace();
          Log.w(TAG, "Receive Error!");
          cancel();
          return;
        }catch(IndexOutOfBoundsException e) {
          e.printStackTrace();
        }
      }

      try {
        bis.close();
      } catch(Exception e){
        e.printStackTrace();
      }


      /*start Activity*/
      if(ba.length() > 0) {
        data = ba.toByteArray();
        p.unmarshall(data, 0, data.length);
        p.setDataPosition(0);
        Bundle tmpBundle = p.readBundle();
        p.recycle();

        if(tmpBundle != null) {
          /* files */
          ArrayList<String> bodies = tmpBundle.getStringArrayList("TARGET_FILE_BODY");
          if(bodies != null) {
            tmpBundle.remove("TARGET_FILE_BODY"); /* reduce memory */
            ArrayList<String> paths = tmpBundle.getStringArrayList("TARGET_FILE_NAME");
            writeToTmp(paths, bodies);
          }

          /* stacked Activity */
          ArrayList<Bundle> stacked = tmpBundle.getParcelableArrayList("MIGRATED STACK");
          prev = System.nanoTime();
          if(stacked != null) {
            Bundle first = stacked.get(0);
            if(first != null) {
              tmpBundle = first;
            }
            stacked.remove(0);
          }


          /*launch target Activity*/
          Intent intent = new Intent();
          intent.setClassName(tmpBundle.getString("MIGRATED PNAME"), tmpBundle.getString("MIGRATED CNAME"));
          intent.putExtra("MIGRATED", tmpBundle);
          if(stacked != null) {
            intent.putParcelableArrayListExtra("MIGRATED STACK", stacked);
          }
          //					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

          /*launch target Activity in GUI thread*/
          new Handler(Looper.getMainLooper()).post(new Runner(intent));

          cancel();
        }
      }
      Looper.loop();
    }

    public void cancel(){
      try{
        mSocket.close();
      }catch(Exception e){
        if(debug) Log.w(TAG, "cannot close module");
      }
    }
  }

  private class Runner implements Runnable {
    private Intent intent;

    public Runner(Intent target){
      intent = target;
    }

    public void run() {
      if(debug) Log.w(TAG, "Runner StartActivity");
      mContext.startActivityAsUser(intent, UserHandle.CURRENT);
      return;
    }
  }
}
