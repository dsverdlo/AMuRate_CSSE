package com.dsverdlo.AMuRate.gui;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.dsverdlo.AMuRate.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * This class is a solution for Android's lack of GIF support.
 * AnimationView enables to display and animate GIF's in the 
 * user interface. We will use this for loading animations.
 * 
 * Code copied from {@link http://iamvijayakumar.blogspot.be/2012/06/android-animated-gif-example.html}
 * 
 * @author David Sverdlov
 *
 */
public class AnimationView extends View {
    private Movie mMovie;
    private long mMovieStart;
    private static final boolean DECODE_STREAM = true;
    private static byte[] streamToBytes(InputStream is) {
      ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
      byte[] buffer = new byte[1024];
      int len;
      try {
        while ((len = is.read(buffer)) >= 0) {
          os.write(buffer, 0, len);
        }
      } catch (java.io.IOException e) {
      }
      return os.toByteArray();
    }
    
     public AnimationView(Context context,AttributeSet attrs) {
      super(context,attrs);
      setFocusable(true);
      java.io.InputStream is;
      is = context.getResources().openRawResource(R.drawable.loading_black);
      if (DECODE_STREAM) {
        mMovie = Movie.decodeStream(is);
      } else {
        byte[] array = streamToBytes(is);
        mMovie = Movie.decodeByteArray(array, 0, array.length);
      }
    }
    @Override
    public void onDraw(Canvas canvas) {
     long now = android.os.SystemClock.uptimeMillis();
      if (mMovieStart == 0) { // first time
        mMovieStart = now;
      }
      if (mMovie != null) {
        int dur = mMovie.duration();
        if (dur == 0) {
          dur = 3000;
        }
        int relTime = (int) ((now - mMovieStart) % dur);
//       Log.d("", "real time :: " +relTime);
        mMovie.setTime(relTime);
        mMovie.draw(canvas, getWidth() - 200, getHeight()-200);
        invalidate();
      }
    }
  }