package com.android.migrationmanager;

import android.graphics.drawable.Drawable;

public class AppData {
	private Drawable imageData;
	private String nameData;
	private String packageData;

	public void setImage(Drawable image){
		imageData = image;
	}

	public Drawable getImage(){
		return imageData;
	}

	public void setName(String text){
		nameData = text;
	}

	public String getName(){
		return nameData;
	}

	public void setPackage(String text){
		packageData = text;
	}

	public String getPackage(){
		return packageData;
	}
}
