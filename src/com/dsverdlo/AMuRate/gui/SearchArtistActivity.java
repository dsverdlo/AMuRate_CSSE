package com.dsverdlo.AMuRate.gui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dsverdlo.AMuRate.R;
import com.dsverdlo.AMuRate.objects.AMuRate;
import com.dsverdlo.AMuRate.objects.Artist;
import com.dsverdlo.AMuRate.services.DownloadImageTask;
import com.dsverdlo.AMuRate.services.DownloadLastFM;

import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class is the screen for displaying the results of Artists
 * 
 * @author David Sverdlov
 *
 */
public class SearchArtistActivity extends BlankActivity {
	private AMuRate amr;
	private SearchArtistActivity thisActivity;
	
	// Tasks is a collection of image downloading. We suppress the warning
	// Because we have two types of AsyncTasks (DownloadLastFM and DownloadImageTask)
	@SuppressWarnings("rawtypes")
	private AsyncTask[] tasks;
	
	// Information about the data download
	private boolean isDownloading;
	private DownloadLastFM currentDownload;
	// The result we are downloading for. 
	// Store it here so we can change the background back
	private LinearLayout currentItem;
	
	/*
	 * Called when the activity is first created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_artist);

		// initialize the application and activity object
		thisActivity = this;
		amr = (AMuRate)getApplicationContext();
		
		// Since the results return 30 results at maximum, initialize array of 30
		tasks = new AsyncTask[30];
		isDownloading = false;
		
		// Grab the vertical layout so we can add objects to it
		LinearLayout ll = (LinearLayout) findViewById(R.id.searchArtistLayout);

		// Add small margin
		LinearLayout.LayoutParams horizontalLayoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		horizontalLayoutParams.setMargins(5, 20, 5, 0); // left, top, right, bottom

		try {
			// If there is no Extra, not much we can do.
			if(!getIntent().hasExtra("searchResults")) {
				Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}

			// Get the search results
			String getSearchResults = getIntent().getStringExtra("searchResults");
		
			// Parse it to a JSON Object
			JSONObject searchResults = new JSONObject(getSearchResults);
			
			JSONArray artists = searchResults.getJSONArray("artist");

			// For every match: create a button for the Artist
			for(int i = 0; i < artists.length(); i++ ) {
				
				// Get the current artist (in JSON)
				final JSONObject oneResult = artists.getJSONObject(i);
				// Convert it to an Artist object
				final Artist artist = new Artist();
				artist.loadFromSearch(oneResult.toString()); 
				
				// If no MBID was associated, skip the work
				if(artist.getMbid().equals("")) continue;
				
				// Create a layout for the artist
				final LinearLayout horizontalLayout = new LinearLayout(amr);
				horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
				horizontalLayout.setGravity(Gravity.CENTER_VERTICAL);
				// Add some padding and the background
				horizontalLayout.setPadding(0, 0, 0, 0);
				horizontalLayout.setBackgroundResource(R.drawable.track_background_wider_small);
				horizontalLayout.setPadding(5, 5, 5, 5);

				// Create an imageview for a picture of the artist (loaded later)
				ImageView picture = new ImageView(amr);

				// Get a layout for the title (which contains the artistname)
				LinearLayout titleLayout = new LinearLayout(amr);
				titleLayout.setOrientation(LinearLayout.VERTICAL); 
				titleLayout.setPadding(20, 0, 20, 0);

				// Set the artistname and add it to title layout
				TextView artistName = new TextView(amr);
				artistName.setTextColor(Color.BLACK);
				artistName.setText(artist.getArtistName());
				titleLayout.addView(artistName);
				
				// Create a textview with an arrow to the right
				// This will show the clickability
				TextView next = new TextView(amr);
				next.setGravity(Gravity.RIGHT);

				// if possible set image in imageview
				String imageUrl = artist.getImage("l");
				
				if(imageUrl.length() > 0) {
					// If there is an url for the Large image, download it and save the task
					DownloadImageTask download = new DownloadImageTask(picture);
					tasks[i] = download.execute(imageUrl);
				} else {
					// Otherwise show standard picture
					picture.setImageResource(R.drawable.not_available);
				}

				// Set the onClick function to change the background, stop other tasks
				// and start the download of information
				horizontalLayout.setOnClickListener( new OnClickListener() {
					public void onClick(View v) {
						// If we were downloading an other, don't do this
						if(isDownloading) return;
						
						// Change background
						horizontalLayout.setBackgroundResource(R.drawable.track_background_2);
		
						// First kill other loading images
						for(int idx = 0; idx < tasks.length; idx++) {
							// If not null and not cancelled --> cancel
							if(tasks[idx] != null && !tasks[idx].isCancelled())
								tasks[idx].cancel(true);
						}

						// Set the downloading information
						currentItem = horizontalLayout;
						isDownloading = true;
						currentDownload = new DownloadLastFM(thisActivity, DownloadLastFM.dl_search_artist_artist);
						currentDownload.execute(artist.getMbid());

					}
				});
				// Let the user be able to click the whole layout of this search result
				horizontalLayout.setClickable(true);

				// Add single items to horizontal layout
				horizontalLayout.addView(picture);
				horizontalLayout.addView(titleLayout);
				horizontalLayout.addView(next);

				// Add finished product to vertical linear layout
				ll.addView(horizontalLayout, horizontalLayoutParams);
			}


		} catch (Exception e) {
			System.out.println("Exception in SearchArtistActivity (onCreate):");
			e.printStackTrace();
		}
	}





/*
 * This method is called by DownloadLastFM when it's done
 */
	public void onRetrievedArtistInfo(String results) {
		// Nullify download information
		isDownloading = false;
		currentDownload = null;
		// Set original background
		currentItem.setBackgroundResource(R.drawable.track_background_wider_small);
		currentItem = null;
		
		try {
			JSONObject JSONartistInfo = new JSONObject(results);
			if(!JSONartistInfo.has("artist")) {
				// something went wrong.
				Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_LONG).show();
				return;
			}
			// If all is good, get the artist object and put in the Extra for the ArtistActivity
			JSONObject JSONartist = JSONartistInfo.getJSONObject("artist");

			Intent nextPage = new Intent(amr, ArtistActivity.class);
			nextPage.putExtra("artist", JSONartist.toString());
			startActivity(nextPage);
			
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
