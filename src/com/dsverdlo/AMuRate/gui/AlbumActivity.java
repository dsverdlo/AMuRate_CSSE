package com.dsverdlo.AMuRate.gui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dsverdlo.AMuRate.R;
import com.dsverdlo.AMuRate.objects.AMuRate;
import com.dsverdlo.AMuRate.objects.Album;
import com.dsverdlo.AMuRate.objects.Track;
import com.dsverdlo.AMuRate.services.DownloadImageTask;
import com.dsverdlo.AMuRate.services.DownloadLastFM;

import android.os.Bundle;
import android.content.Intent;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

/**
 * This activity shows the details of an album. 
 * 
 * @author David Sverdlov
 *
 */
public class AlbumActivity extends BlankActivity {
	private AMuRate amr;
	
	private AlbumActivity albumActivity;
	private TextView albumTitle, albumArtist;
	private TextView playcount, listeners;
	private ImageView albumImage;
	private Button albumImageClose;
	private LinearLayout tracksLayout;
	private TextView summary;
	
	private Album album;
	
	// When a user clicks a button, we will display a loading message
	// to let the user know it is being processed.
	// We also disable the ability to click another track meanwhile
	private boolean isDownloading;
	private CharSequence clickedText;
	private Button clickedButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_album);
		
		// Get the application
		amr = (AMuRate)getApplicationContext();
		
		// Store the itself
		albumActivity = this;
		
		// Initialize the views
		albumTitle = (TextView) findViewById(R.id.album_title);
		albumArtist = (TextView) findViewById(R.id.album_artist);
		albumImage = (ImageView) findViewById(R.id.album_image);
		albumImageClose = (Button) findViewById(R.id.album_image_close);
		playcount = (TextView) findViewById(R.id.album_playcount);
		listeners = (TextView) findViewById(R.id.album_listeners);
		tracksLayout = (LinearLayout) findViewById(R.id.album_tracks);
		summary = (TextView) findViewById(R.id.album_summary);	
		
		// Initialize members
		isDownloading = false;
		
		// Get the album to be displayed from the Extra's
		Intent intent = getIntent();
		if(intent.hasExtra("album")) {
			album = new Album(intent.getStringExtra("album"));
		} else if(intent.hasExtra("fromArtist")) {
			try {
				album = new Album(new JSONObject(intent.getStringExtra("fromArtist")).toString());
			} catch (JSONException e) {
				finish();
			}
		} else {
			// If we could not get it, we can't display this activity
			finish();
			Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_SHORT).show();
		}
		
		// Set the text of the album title and the artist of the album
		albumTitle.setText(album.getAlbumTitle());
		albumTitle.setTextSize(20);
		albumArtist.setText(album.getArtistName());
		albumArtist.setTextSize(17);
		
		// Set the text "X" on the close button
		albumImageClose.setText(R.string.album_x);
		// Define layout parameters (margins), and gravity to the right
		LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT);
		closeLp.setMargins(120, 10, 0, 0); // 120left, top, right, bottom
		albumImageClose.setGravity(Gravity.RIGHT);
		albumImageClose.setLayoutParams(closeLp);
		// When clicked on the close button, we clear the previous activities
		// from the stack and navigate to the main screen
		albumImageClose.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
				Intent next = new Intent(amr,MainActivity.class);
				next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(next);
			}
		});
		
		// Load the album image. If there is a link for the Large one, download it
		// Otherwise we display a standard picture.
		if(album.getImageL().length() > 0) {
			DownloadImageTask download = new DownloadImageTask(albumImage);
			download.execute(album.getImageL());
		} else albumImage.setImageResource(R.drawable.not_available);
		
		// Set the playcount and the listeners count
		playcount.setText(amr.getString(R.string.album_playcount) + album.getPlaycount());
		listeners.setText(amr.getString(R.string.album_listeners) + album.getListeners());
		
		// Set the summary of the album in the scrollview.
		// Use fromHtml to make the links work
		summary.setText(Html.fromHtml(album.getSummary()));
		// If there is no info, don't display it
		if(album.getSummary().equals("")) summary.setVisibility(View.INVISIBLE);

		// Set the tracks of the album in a scrollview
		try {
			JSONArray tracks = album.getTracks();
			
			// We keep track of the skipped tracks. We filter the tracks which
			// have no MBID and no duration associated with.
			// By keeping this counter, we kan correctly number the good tracks.
			int skipped = 0;
			
			for (int i = 0; i < tracks.length(); i++) {
				JSONObject oneTrack = tracks.getJSONObject(i);
				final String tit = oneTrack.getString("name");
				final String mbid = oneTrack.getString("mbid");
				
				// Parse the JSON to a Track object
				Track track = new Track();
				track.loadFromSearch(oneTrack);
				
				// If no duration and no mbid: filter away
				if(track.getDurationInt() <= 0 &&  mbid != null && mbid.equals("")) {
					skipped ++; 
					continue;
				}
				
				// Make a custom button for the track
				final Button bt = new Button(amr);
				bt.setBackgroundResource(R.layout.rounded_corners);

				// The layout parameters
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.FILL_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT);
				lp.setMargins(8, 10, 8, 4); // left, top, right, bottom
				bt.setLayoutParams(lp);
				
				// Set the button text like this:     n: Song title
				bt.setText("" + (i+1-skipped) + ":" + tit);
				
				// When the user clicks on it, we do a small check if there is a MBID
				// Before we start downloading info for that mbid.
				bt.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if(!isDownloading) {
							if(mbid.length() == 0) {
								Toast.makeText(amr, R.string.msg_no_mbid_track, Toast.LENGTH_SHORT).show();
							} else {
								isDownloading = true;
								clickedButton = bt;
								clickedText = bt.getText();
								bt.setText(R.string.loading);
								DownloadLastFM dl = new DownloadLastFM(albumActivity, DownloadLastFM.dl_album_track_info);
								dl.execute(tit, album.getArtistName());
							}
						} else {
							Toast.makeText(amr, R.string.msg_already_downloading, Toast.LENGTH_SHORT).show();
						}
					}
				});
				
				// When we are done, we add the button to the view
				tracksLayout.addView(bt);
				
			}
			
		} catch (JSONException je) {
			System.out.println("JSONException in AlbumActivity");
		}
		
	}


	/*
	 * When the info has been downloaden of the clicked track, we start a new activity.
	 * We don't finish() this one, as the user might want to return to this by pressing 
	 * the back button
	 */
	public void searchResultsTitleAndArtist(String extraString) {
		// Set the text back
		isDownloading = false;
		clickedButton.setText(clickedText);
		
		// Start the new activity
		Intent trackActivity = new Intent(amr, TrackActivity.class);
		trackActivity.putExtra("track", extraString);
		startActivity(trackActivity);
	}

}
