package com.android.migrationmanager;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class CustomAdapter extends ArrayAdapter<AppData> {
	private LayoutInflater layoutInflater_;

	public CustomAdapter(Context context, int textViewResourceId, List<AppData> objects) {
		super(context, textViewResourceId, objects);
		layoutInflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// 特定の行(position)のデータを得る
		AppData item = (AppData)getItem(position);

		// convertViewは使い回しされている可能性があるのでnullの時だけ新しく作る
		if (null == convertView) {
			convertView = layoutInflater_.inflate(R.layout.applist, null);
		}

		// AppDataのデータをViewの各Widgetにセットする
		ImageView imageView;
		imageView = (ImageView)convertView.findViewById(R.id.imageView);
		imageView.setImageDrawable(item.getImage());

		TextView textView;
		textView = (TextView)convertView.findViewById(R.id.textView);
		textView.setText(item.getName());

		return convertView;
	}
}
