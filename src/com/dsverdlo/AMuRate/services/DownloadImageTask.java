package com.dsverdlo.AMuRate.services;

import java.net.URL;

import android.R;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

import com.dsverdlo.AMuRate.gui.AnimationView;

/**
 * This class downloads the pictures for the views.
 * The constructor takes the ImageView where the 
 * image has to be put in. When a loading gif is
 * supplied, we hide it when we are done downloading.
 * 
 * This runs in the background
 * 
 * @author David Sverdlov
 *
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
	private ImageView iv;
	private AnimationView load;

	/*
	 * Constructor with ImageView and a loading animation
	 */
	public DownloadImageTask(final ImageView iv, final AnimationView load) { 
		this.iv = iv;
		this.load = load;
	}

	/*
	 * Constructor with the ImageView where the image will be set in
	 */
	public DownloadImageTask(final ImageView iv) { 
		this.iv = iv;
	}

	/*
	 * Download in the background
	 */
	@Override
	protected Bitmap doInBackground(String... params) {
		try {
			URL url = new URL(params[0]);
			Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
			return bmp;
		} catch (Exception e) {
			System.out.println("Exception in MyConnection(loadImage):"+e);

			return null;
		}
	}

	/*
	 * When done downloading, hide the loading gif if there was one,
	 * Set the ImageView visible and set the bitmap
	 */
	@Override
	protected void onPostExecute(Bitmap bmp) {
		super.onPostExecute(bmp);
		if(load != null) load.setVisibility(View.GONE);
		if(bmp != null) {
			iv.setVisibility(View.VISIBLE);
			iv.setImageBitmap(bmp);
		}

	}			
}



