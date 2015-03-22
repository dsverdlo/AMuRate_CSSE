package com.dsverdlo.AMuRate.gui;

import org.json.JSONException;
import org.json.JSONObject;

import com.dsverdlo.AMuRate.R;
import com.dsverdlo.AMuRate.objects.AMuRate;
import com.dsverdlo.AMuRate.objects.Track;
import com.dsverdlo.AMuRate.services.DownloadImageTask;
import com.dsverdlo.AMuRate.services.DownloadLastFM;
import com.dsverdlo.AMuRate.services.ServerConnect;
import com.dsverdlo.AMuRate.services.DatabaseSyncer;
import com.dsverdlo.AMuRate.services.InternalDatabaseHistoryAdapter;
import com.dsverdlo.AMuRate.services.InternalDatabaseRatingAdapter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Display a selected song/track on the screen. 
 * 
 * @author David Sverdlov
 *
 */

public class TrackActivity extends BlankActivity {
	private AMuRate amr;
	private TrackActivity trackActivity;
	
	private final Track track = new Track();
	private static String ratingBarInfoString;
	
	// This int keeps the state of the server connection: unchecked, no conn, conn
	private int isServerAvailable; 
	
	// This boolean keeps track if the ServerConnectionTYpe has switched yet
	private boolean haveSwitched;
	
	// The views
	private AnimationView loading_image;
	private Button back;
	private Button album;
	private Button artist;
	private ImageButton youtube;
	private ImageView image;
	private RatingBar ratingBar;
	private TextView ratingBarInfo2;
	private TextView title;
	private TextView ratingBarInfo;

	// The local database adapters
	private InternalDatabaseRatingAdapter ra;
	private InternalDatabaseHistoryAdapter ha;
	
	private boolean isDownloading;
	private DownloadLastFM currentDownloading;

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_track);

		// Get the application and the activity
		amr = (AMuRate) getApplicationContext();
		trackActivity = this;
		
		// Set the downloading information
		isDownloading = false;
		
		haveSwitched = false;
		
		// Load the track if there is one
		if(getIntent().hasExtra("track")) {
			track.loadFromInfo(getIntent().getStringExtra("track"));
		} else if(getIntent().hasExtra("jsontrack")) {
			JSONObject JSONtrack;
			try {
				JSONtrack = new JSONObject(getIntent().getStringExtra("jsontrack"));
				track.loadFromInfo(JSONtrack);
			} catch (JSONException e) {
				// No extras recognized, something must have been passed on wrong.
				Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
		}
				
		// Initialize the loading gif
		loading_image = (AnimationView) findViewById(R.id.track_loading_image);
		loading_image.setVisibility(View.VISIBLE);

		// Server communication state begins unchecked
		isServerAvailable = -1; 
		
		// Initialize adapters
		ra = new InternalDatabaseRatingAdapter(this);
		ha = new InternalDatabaseHistoryAdapter(this);
		
		// Save the viewing of this trackactivity in the local database
		ha.addHistoryTrack(track.getMBID(), track.getArtist(), track.getTitle());

		// initialize the views
		title = (TextView) findViewById(R.id.track_title);
		artist = (Button) findViewById(R.id.track_button_artist);	
		image = (ImageView) findViewById(R.id.track_albumimage);
		image.setVisibility(View.GONE);
		album = (Button) findViewById(R.id.track_butt_on_album);	

		// The ratingbar has 5 stars and steps of 0.5
		ratingBar = (RatingBar) findViewById(R.id.track_ratingBar);
		ratingBar.setNumStars(5); 
		ratingBar.setStepSize((float) 0.5);

		// There are 2 informational texts by the ratingbar.
		// One for server messages, and one for displaying own rating
		ratingBarInfo = (TextView) findViewById(R.id.track_ratingbarinfo); 		
		ratingBarInfo2 = (TextView) findViewById(R.id.track_ratingbarinfo2);
		ratingBarInfoString = amr.getString(R.string.track_ratingbarinfo);
		
		// Get the youtube button and add the click functionality
		youtube = (ImageButton) findViewById(R.id.track_youtube);
		youtube.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				PackageManager pm = amr.getPackageManager();
				try {
					// Semi check to see if youtube app is installed:
					// (semi meaning we will catch it, if it raised an exception)
					pm.getPackageInfo("com.google.android.youtube", PackageManager.GET_ACTIVITIES);

					Intent intent = new Intent(Intent.ACTION_SEARCH);
					intent.setPackage("com.google.android.youtube");
					intent.putExtra("query", track.getTitle() +" - "+track.getArtist());
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
				catch (Exception ex){
					// if not installed it will raise exception so we show a Toast
					Toast.makeText(amr, R.string.track_youtube_not_found , Toast.LENGTH_SHORT).show();
					// Then we will launch it in the browser
					String youtube = "http://www.youtube.com/results?search_query="
							+ track.getTitle() +" - "+track.getArtist();
					Uri uriUrl = Uri.parse(youtube);
					Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
					startActivity(launchBrowser);
				}
			}
		});

		// 'Backbutton' has text New Search
		back = (Button) findViewById(R.id.track_newsearch);
		back.setText(R.string.track_new_search);
		// And on click it clears the activity stack and starts the MainActivity
		back.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				finish();
				Intent mainIntent = new Intent(amr, MainActivity.class);
				mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(mainIntent);
			}
		});

		// Set the title as: Title (mm:ss)
		String titleString = track.getTitle();
		
		// If there is a duration, add it
		if(!track.getDuration().equals(":")) titleString = titleString + "  (" + track.getDuration() + ")";
		
		title.setText(titleString);

		// Set the artist in it's button and when the user clicks on it, 
		// it will start downloading artist information
		artist.setText(track.getArtist());
		artist.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				String artistMBID = track.getArtistMBID();
				if(artistMBID.length()>0) {
					if(isDownloading) {
						Toast.makeText(amr, R.string.msg_already_downloading, Toast.LENGTH_SHORT).show();
						return;
					}
					isDownloading = true;
					currentDownloading = new DownloadLastFM(trackActivity, DownloadLastFM.dl_track_artist_info);
					currentDownloading.execute(artistMBID);
				} else {
					Toast.makeText(amr, R.string.msg_no_mbid_artist, Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		// Check if an image can be displayed. If there is no large, try a medium.
		String imageUrl = track.getImage("l");
		if(!imageUrl.equals("")) {
			DownloadImageTask download = new DownloadImageTask(image, loading_image);
			download.execute(imageUrl);
		} else {
			// Else try a medium picture
			String OtherImageUrl = track.getImage("m");
			if(!OtherImageUrl.equals("")) {
				DownloadImageTask download = new DownloadImageTask(image, loading_image);
				download.execute(OtherImageUrl);
			} else {
				loading_image.setVisibility(View.GONE);
				image.setVisibility(View.VISIBLE);
				image.setImageResource(R.drawable.not_available);
			}
		}

		// Set the text From which album. If there is an album mbid, 
		// then an album activity can be created with a click.
		if(track.getAlbumMBID().length() > 0) {
			album.setText(track.getAlbumTitle());

			album.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					if(isDownloading) {
						Toast.makeText(amr, R.string.msg_already_downloading, Toast.LENGTH_SHORT).show();
						return;
					}
					isDownloading = true;
					currentDownloading = new DownloadLastFM(trackActivity, DownloadLastFM.dl_track_album_info);
					currentDownloading.execute(track.getAlbumMBID());
				}
			});
		} 

		// Set the default messages until more information will be available
		ratingBarInfo.setText(R.string.track_loading_ratings);
		ratingBarInfo2.setText(R.string.track_checking_rated);

		// If the ratingbar is changed
		ratingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
				// We will first switch on connection states
				switch(isServerAvailable) {
				case -1: 
					// Checking hasnt returned yet, stall some time with Toast until Asynctask is done
					Toast.makeText(amr, R.string.msg_testing_connection, Toast.LENGTH_SHORT).show();
					break;
				case 1: 
					// If we can connect
				Toast.makeText(amr, R.string.msg_sending_external, Toast.LENGTH_SHORT).show();
				sendToServer(); 
				ratingBar.setClickable(false);
				ratingBar.setEnabled(false);
				ratingBarInfo2.setText(amr.getString(R.string.track_you_rated) + rating);
				ratingBarInfo2.setVisibility(View.VISIBLE);
				break;
				case 0: 
					// If we can't connect 
				Toast.makeText(amr, R.string.msg_sending_internal, Toast.LENGTH_SHORT).show();
				sendToInternalDatabase();		
				ratingBar.setClickable(false);
				ratingBar.setEnabled(false);
				ratingBarInfo2.setText(amr.getString(R.string.track_you_rated) + rating);
				ratingBarInfo2.setVisibility(View.VISIBLE);
				ratingBarInfo.setVisibility(View.GONE);
				break;
				default: // Shouldnt get here
					System.out.println("Something went wrong! isAvailable?" + isServerAvailable);
				}
			}
		});
		
		title.setSelected(true);
		
		// Check if server is connectable. This will set the isServerAvailable int
		new ServerConnect(trackActivity, amr, ServerConnect.ISCONNECTED).execute();
		
		// Try syncing in the background as well
		new DatabaseSyncer(amr).execute();
	}


	
	/*
	 * Called when we get a new rating average to display.
	 * Since a rating is always less than 10, we can send
	 * the amount of ratings along in the number, 
	 * by first multiplying it with 10, then adding.
	 */
	public void onDoneGettingExternal(Double result) {
		// result is still two values in one, so we split first
		Double avg = result % 10;
		int amt = (int) ((result - avg) / 10);
				
		// Store listener, then nullify it because we are going to change
		// the value of the bar, but don't want it to go post it
		OnRatingBarChangeListener orbcl = ratingBar.getOnRatingBarChangeListener();
		ratingBar.setOnRatingBarChangeListener(null);
		// We do a little math 'trick' to get a number with step 0.5 
		ratingBar.setRating((float) (Math.ceil(avg * 2) / 2.0));
		ratingBar.setOnRatingBarChangeListener(orbcl);

		ratingBarInfo.setText(String.format(ratingBarInfoString, avg, amt));

	}
 
	/*
	 * This method is called when we are done sending the score 
	 * to the server
	 */
	public void onDoneSendingExternal(Double result) {
	
		if(result > 0) { // Sent succesfully
			Toast.makeText(amr, R.string.msg_sending_success, Toast.LENGTH_SHORT).show();
			// get new avg
			new ServerConnect(trackActivity, amr, ServerConnect.GETRATING).execute(track.getMBID());
			// When this returns, it will call onDoneGettingExternal
		} else {
			// Sending did not succeed, so send it unsynced to local database
			Toast.makeText(amr, R.string.msg_sending_failed, Toast.LENGTH_SHORT).show();
			
			sendToInternalDatabase(); 
			
			// Also retest connection
			Toast.makeText(amr, R.string.msg_connection_retest, Toast.LENGTH_SHORT).show();
			new ServerConnect(trackActivity, amr, ServerConnect.ISCONNECTED).execute();
			// And this will update isServerAvailable when it is done
		}
	}

	/*
	 * This method is called when a server Thread is done testing 
	 * if a connection could be made
	 */
	public void onDoneTestingExternalConnection(Double result) {
		// Set the state
		isServerAvailable = (result > 0) ? 1 : 0; 
		// and now we know this, we can get appropriate readings
		
		if(isServerAvailable > 0){
			// if server connection available, get the server average
			new ServerConnect(trackActivity, amr, ServerConnect.GETRATING).execute(track.getMBID());
			
			// and check if user has already rated this
			new ServerConnect(trackActivity, amr, ServerConnect.HASRATED).execute(track.getMBID(), amr.getUser());
			
		} else {
			// If we have not Switched yet, try other serverconnectiontype
			if(!haveSwitched) {
				amr.switchConnectionType();
				haveSwitched = true;
				new ServerConnect(trackActivity, amr, ServerConnect.ISCONNECTED).execute();
				
			} else {
				// We can't connect to the server, so we display it.
				// The only thing we can try to do is see if we've rated it already
				// and that score hasn't been deleted from the local database
				if(ra.hasRatedBefore(track.getMBID())){
					float rating = ra.readRating(track.getMBID());
					// If the user has rated it before and we have the rating
					// Say that there is no server connection, but we know what
					// he rated the track before
					ratingBarInfo.setText(R.string.msg_no_server_connection);
					ratingBarInfo2.setText(amr.getString(R.string.track_you_rated) + rating);

					// change listener swap trick (otherwise it will send the score again)
					OnRatingBarChangeListener orbcl = ratingBar.getOnRatingBarChangeListener();
					ratingBar.setOnRatingBarChangeListener(null);
					ratingBar.setRating(rating); 
					ratingBar.setOnRatingBarChangeListener(orbcl);

					// User already rated, so disable it
					ratingBar.setEnabled(false);
				} else {
					// If there is no connection we cannot make sure if the user already
					// rated this track. Give the possibility to be able to rate.
					// On sending it we will verify if the user had rated before or not.
					ratingBarInfo.setText(R.string.msg_can_send_later);
					ratingBarInfo2.setVisibility(View.GONE);
					ratingBar.setEnabled(true);
				}
			}
		}
	}

	/*
	 * This method will send start a server connection Thread
	 * to send a score 
	 */
	private void sendToServer() {
		// Prepare params for server connection Task
		int method = ServerConnect.SENDRATING;
		String mbid = track.getMBID();
		String artist = track.getArtist();
		String title = track.getTitle();
		float rating = ratingBar.getRating();
		int date = (int) (System.currentTimeMillis() / 1000L);
		String user = amr.getUser();
		
		// Also keep a copy in the local database (synced)
		ra.addRating(track.getMBID(), track.getArtist(), track.getTitle(), rating, true);
		
		// Finally send to ext db
		new ServerConnect(trackActivity, amr, method).execute(mbid, artist, title, ""+rating, ""+date, user);
		// This will call onDoneSendingExternal when it's done
	}

	/*
	 * This will save the rating in the local database
	 */
	private void sendToInternalDatabase() {

		float rating = ratingBar.getRating();
		long addResult = ra.addRating(track.getMBID(), track.getArtist(), track.getTitle(), rating, false);
		
		if(addResult < 0) {
			// The only reason this should fail is because the user had rated this before
			Toast.makeText(amr, R.string.msg_already_rated, Toast.LENGTH_SHORT).show();
		} else {
			// Succes. Update the ratingbar with our given score
			float avg = rating;
			int amt = 1;
			onDoneGettingExternal((double) avg + (amt * 10) );
			
			// User has rated this track: disable ratingbar
			ratingBar.setEnabled(false);
		}
	}



	/*
	 * 	This method is called after the DownloadLastFM task returns with artist info
	 */
	public void onDoneLoadingArtist(String artistInfo) {
		isDownloading = false;
		currentDownloading = null;
		
		try {
			JSONObject JSONartistInfo = new JSONObject(artistInfo);
			if(!JSONartistInfo.has("artist")) {
				// something went wrong.
				Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_LONG).show();
				return;
			}

			// Get the artist JSON 
			JSONObject JSONartist = JSONartistInfo.getJSONObject("artist");

			// Start ArtistActivity
			Intent artistIntent = new Intent(amr, ArtistActivity.class);
			artistIntent.putExtra("artist", JSONartist.toString());
			startActivity(artistIntent);

		} catch (JSONException jsone) {
			System.err.println("JSONException in TrackActivity: onDoneLoadingArtist:\n"+jsone);
			Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_SHORT).show();
		}
	}

	/*
	 * Called when DownloadLastFM returns
	 */
	public void onDoneLoadingAlbum(String album) {
		isDownloading = false;
		currentDownloading = null;
		
		Intent albumIntent = new Intent(amr, AlbumActivity.class);
		albumIntent.putExtra("album", album);
		startActivity(albumIntent);
	}

	/*
	 * This is called when the server answers with hasChecked
	 */
	public void onDoneCheckingHasRated(Double result) {
		if(result > 0) {
			// If user has rated
			ratingBarInfo2.setText(amr.getString(R.string.track_you_rated) + result);
			ratingBar.setEnabled(false);
		} else {
			// If user hasn't rated
			ratingBarInfo2.setText(R.string.track_not_rated);
		}
	}
}
