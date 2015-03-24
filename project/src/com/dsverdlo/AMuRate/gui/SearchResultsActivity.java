package com.dsverdlo.AMuRate.gui;

import org.json.JSONArray;
import org.json.JSONObject;

import com.dsverdlo.AMuRate.R;
import com.dsverdlo.AMuRate.objects.AMuRate;
import com.dsverdlo.AMuRate.objects.Track;
import com.dsverdlo.AMuRate.services.DownloadImageTask;
import com.dsverdlo.AMuRate.services.DownloadLastFM;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Display the search results on the screen.
 * 
 * @author David Sverdlov
 */
public class SearchResultsActivity extends BlankActivity {
	private AMuRate amr;
	private SearchResultsActivity thisActivity;
	
	private DownloadImageTask[] tasks;

	// Save the download information so we can alter it when the connection returns
	private boolean isDownloading;
	private DownloadLastFM currentDownload;
	private RelativeLayout clickedLayout;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_search_results);
		
		// Get the application and activity
		amr = (AMuRate)getApplicationContext();
		thisActivity = this;
		
		// Initialize the tasks for loading 30 image tasks
		tasks = new DownloadImageTask[30];
		
		// Grab the vertical layout so we can add objects to it
		LinearLayout ll = (LinearLayout) findViewById(R.id.searchResultsLayout);

		// Add small margin
		LinearLayout.LayoutParams horizontalLayoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		horizontalLayoutParams.setMargins(5, 20, 5, 0); // left, top, right, bottom


		try {
			if(!getIntent().hasExtra("searchResults")) {
				// We can do nothing then...
				Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			// Otherwise, get the results from Extra 
			String getSearchResults = getIntent().getStringExtra("searchResults");

			// Parse the string to JSONObject
			JSONObject searchResults = new JSONObject(getSearchResults);

			// Get the trackmatches
			JSONArray tracks = searchResults.getJSONArray("track");

			System.out.println(""+5+": "+System.currentTimeMillis());
			// For each, make a layout 
			for(int i = 0; i < tracks.length(); i++ ) {
				// Get the current JSON track
				final JSONObject oneResult = tracks.getJSONObject(i);
				// Create track object from it
				Track track = new Track();
				track.loadFromSearch(oneResult);

				// If the track does not come with a mbid, skip it
				if(track.getMBID().equals("")) continue;
				
				// Create a layout for the track
				final RelativeLayout horizontalLayout = new RelativeLayout(amr);
				horizontalLayout.setGravity(Gravity.LEFT); 
				// Add a small padding and a background
				horizontalLayout.setPadding(0, 0, 0, 0);
				horizontalLayout.setBackgroundResource(R.drawable.track_background_wider_small);
				horizontalLayout.setPadding(5, 5, 5, 5);

				// Prepare for a picture
				ImageView picture = new ImageView(amr);
				
				// Create a loading image
				AnimationView load_pic = new AnimationView(amr, null);
				load_pic.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				load_pic.setMinimumHeight(200);
				load_pic.setMinimumWidth(120);
				load_pic.setVisibility(View.GONE);
				load_pic.setId(100);
				picture.setVisibility(View.GONE); 
				picture.setId(105);
				
				// Titlelayout for the title and the artist
				LinearLayout titleLayout = new LinearLayout(amr);
				titleLayout.setOrientation(LinearLayout.VERTICAL); 
				titleLayout.setPadding(20, 0, 20, 0);

				// Text for the artist name 
				TextView artist = new TextView(amr);
				artist.setTextColor(Color.BLACK);
				artist.setText(track.getArtist());
				
				// Text for the title
				TextView title = new TextView(amr);
				title.setTextColor(Color.BLACK);
				title.setText(track.getTitle());
				
				titleLayout.addView(title);
				
				// Next with the arrow 
				TextView next = new TextView(amr);
				next.setGravity(Gravity.RIGHT);

				// if possible set image in pictureview and save the Task
				String imageUrl = track.getImage("l");
				if(imageUrl.length() != 0) { 
					tasks[i] = new DownloadImageTask(picture, load_pic);
					tasks[i].execute(imageUrl);
				} else {
					// Or set the not available picture
					picture.setImageResource(R.drawable.not_available);
				}

				// Set the onClick function
				final String mbid = track.getMBID(); 
				horizontalLayout.setOnClickListener( new OnClickListener() {
					public void onClick(View v) {
						if(isDownloading) {
							Toast.makeText(amr, R.string.msg_already_downloading, Toast.LENGTH_SHORT).show();
							return;
						}
						
						// Change background to let the user know we're busy 
						horizontalLayout.setBackgroundResource(R.drawable.track_background_2);
	
						// before we download the info, cancel the downloading of images
						for(int idx = 0; idx < tasks.length; idx++) {
							// if task is not null and not finished: cancel
							if(tasks[idx] != null && !tasks[idx].isCancelled()) {
								tasks[idx].cancel(true);
								tasks[idx] = null;
							}
						}
						
						// Update the downloading information
						clickedLayout = horizontalLayout;
						isDownloading = true;
						currentDownload = new DownloadLastFM(thisActivity, DownloadLastFM.dl_search_results_track);
						currentDownload.execute(mbid);
					}
				});
				// Make the whole thing clickable
				horizontalLayout.setClickable(true);
				
				// Layout parameters for the loading gif
				RelativeLayout.LayoutParams loadParams = 
		                new RelativeLayout.LayoutParams(
		                    RelativeLayout.LayoutParams.WRAP_CONTENT, 
		                    RelativeLayout.LayoutParams.WRAP_CONTENT);
				loadParams.addRule(RelativeLayout.ALIGN_LEFT);
				loadParams.addRule(RelativeLayout.LEFT_OF, 105);
				
				// Layout parameters for the actual image
				RelativeLayout.LayoutParams imageParams = 
		                new RelativeLayout.LayoutParams(
		                    RelativeLayout.LayoutParams.WRAP_CONTENT, 
		                    RelativeLayout.LayoutParams.WRAP_CONTENT);
				imageParams.addRule(RelativeLayout.ALIGN_LEFT);
				
				// Layout parameters for the title
				RelativeLayout.LayoutParams titleParams = 
		                new RelativeLayout.LayoutParams(
		                    RelativeLayout.LayoutParams.WRAP_CONTENT, 
		                    RelativeLayout.LayoutParams.WRAP_CONTENT);
		      titleParams.addRule(RelativeLayout.CENTER_VERTICAL);
		      titleParams.addRule(RelativeLayout.RIGHT_OF, 105);
				
				
				// Add single items to horizontal layout
				horizontalLayout.addView(load_pic,loadParams);
				horizontalLayout.addView(picture, imageParams);
				titleLayout.addView(artist);
				horizontalLayout.addView(titleLayout, titleParams);
				horizontalLayout.addView(next);

				// Add finished product to vertical linear layout
				ll.addView(horizontalLayout, horizontalLayoutParams);
				
			} // end for loop


		} catch (Exception e) {
			System.out.println("Exception in SearchResultsActivity (onCreate):");
			e.printStackTrace();
		}

	}

	/*
	 * When the DownloadLastFM task returns:
	 */
	public void onPostExecute(String results) {
		// Restore the download information
		isDownloading = false;
		currentDownload = null;
		clickedLayout.setBackgroundResource(R.drawable.track_background_wider_small);
		clickedLayout = null;
		
		// If something unexpected given
		if(results == null || results.equals("")) {
			Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_SHORT).show();
			return;
		}
		
		// If normal go to TrackActivity class
		Intent nextPage = new Intent(amr, TrackActivity.class);
		nextPage.putExtra("track", results);
		startActivity(nextPage);
	}

}
