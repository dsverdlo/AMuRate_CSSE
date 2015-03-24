package com.dsverdlo.AMuRate.gui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dsverdlo.AMuRate.R;
import com.dsverdlo.AMuRate.objects.AMuRate;
import com.dsverdlo.AMuRate.objects.Album;
import com.dsverdlo.AMuRate.objects.Artist;
import com.dsverdlo.AMuRate.objects.Track;
import com.dsverdlo.AMuRate.services.DownloadImageTask;
import com.dsverdlo.AMuRate.services.DownloadLastFM;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the screen of one Artist.
 * It displays a picture, bio information, top tracks, top albums
 * 
 * @author David Sverdlov
 *
 */
public class ArtistActivity extends BlankActivity {
	private AMuRate amr;
	private ArtistActivity thisActivity;

	private LinearLayout mainLayout;
	private LinearLayout artistStats;
	private ImageView artistBigPic;
	private TextView longScroll;
	private TextView tracksTitle;
	private LinearLayout tracksScroll;
	private TextView albumsTitle;
	private LinearLayout albumsScroll;

	private TextView tracksMessaging;
	private TextView albumsMessaging;

	private boolean isDownloading;
	private Button clickedButton;
	private CharSequence clickedText;

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.artist_activity);

		// Set the application and the activity
		this.amr = (AMuRate) getApplicationContext();
		thisActivity = this;

		// Set the members
		isDownloading = false;

		// Initialize the views
		mainLayout = (LinearLayout)findViewById(R.id.artistMainLayout);
		mainLayout.setBackgroundResource(R.drawable.new62);

		artistStats = (LinearLayout)findViewById(R.id.artistStats);
		artistBigPic = (ImageView)findViewById(R.id.artistBigPic);
		longScroll = (TextView)findViewById(R.id.long_scroll);
		tracksTitle = (TextView)findViewById(R.id.row1col1);
		tracksScroll = (LinearLayout)findViewById(R.id.row2col1);
		albumsTitle = (TextView)findViewById(R.id.row1col2);
		albumsScroll = (LinearLayout)findViewById(R.id.row2col2);

		// These will be used to display the loading text by the toptracks and topalbums
		tracksMessaging = new TextView(amr);
		albumsMessaging = new TextView(amr);

		// Set the names of the two lists
		tracksTitle.setText(R.string.artist_TRACKS);
		albumsTitle.setText(R.string.artist_ALBUMS);

		// Set the loading text and add it in the scroll lists
		tracksMessaging.setText(R.string.artist_loading_tracks);
		tracksScroll.addView(tracksMessaging);

		albumsMessaging.setText(R.string.artist_loading_albums);
		albumsScroll.addView(albumsMessaging);

		// Load the artist  from the extra
		final Artist artist = new Artist();
		if(getIntent().hasExtra("artist")) {
			artist.loadFromInfo(getIntent().getStringExtra("artist"));
		} else {
			// We cannot make the view otherwise, so finish it.
			finish();
			Toast.makeText(amr, R.string.msg_couldnt_obtain, Toast.LENGTH_SHORT).show();
		}

		// Load the image of the artist. If there is an XL one, download it
		if(artist.getImage("xl").length() > 0) {
			DownloadImageTask download = new DownloadImageTask(artistBigPic);
			download.execute(artist.getImage("xl"));
		} else {
			// Otherwise set a standard picture
			artistBigPic.setImageResource(R.drawable.not_available);
		}

		// Set the summary of the artist in the scrollbox. 
		// Use FromHtml so the links are in a different font
		longScroll.setText(Html.fromHtml(artist.getSummary()));
		// This line makes the links clickable and active
		longScroll.setMovementMethod(LinkMovementMethod.getInstance());
		longScroll.setMaxHeight(160);

		// The back button with rounded corners has text X
		Button buttonBack = new Button(amr);
		buttonBack.setBackgroundResource(R.layout.rounded_corners);
		buttonBack.setText(R.string.album_x);

		// If clicked on X button, finish the activity, clear the stack and 
		// go to the main acitivty.
		buttonBack.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				finish();
				Intent next = new Intent(amr,MainActivity.class);
				next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(next);
			}
		});

		// The layout of the backbutton is small and right aligned
		RelativeLayout backLayout = new RelativeLayout(amr);
		RelativeLayout.LayoutParams backParams = 
				new RelativeLayout.LayoutParams(60, 50);
		backParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		buttonBack.setLayoutParams(backParams);
		backLayout.addView(buttonBack);
		buttonBack.setTextSize(13);

		// The artistname is displayed in a big size
		TextView textViewArtistName = new TextView(this);
		textViewArtistName.setTextSize(25);
		textViewArtistName.setText(artist.getArtistName());

		// The url to the artist's wiki page is also clickable 
		TextView textViewUrlOut = new TextView(this);
		textViewUrlOut.setText(Html.fromHtml(artist.getUrlOut()));
		textViewUrlOut.setMovementMethod(LinkMovementMethod.getInstance());

		// add these items to the designed layout for it
		artistStats.addView(backLayout);
		artistStats.addView(textViewArtistName);
		artistStats.addView(textViewUrlOut);

		DownloadLastFM dl_tracks = new DownloadLastFM(thisActivity, DownloadLastFM.dl_artist_load_tracks);
		dl_tracks.execute(artist.getMbid());
		DownloadLastFM dl_albums = new DownloadLastFM(thisActivity, DownloadLastFM.dl_artist_load_albums);
		dl_albums.execute(artist.getMbid());
	}


	/*
	 * This function is called when the DownloadLastFM has downloaded the top tracks
	 * If the result is not what we expect, we message it failed.
	 * If there are results, we will iterate over them to make a button foreach track
	 */
	public void loadTracks(String loadedTracks) {
		try {
			JSONObject JSONloaded = new JSONObject(loadedTracks);
			if(!JSONloaded.has("toptracks")) {
				// Display the failed message
				tracksMessaging.setText(R.string.artist_loading_tracks_failed); 
				return;
			}
			// Delete the loading message so there is room for the tracks
			tracksScroll.removeAllViews();

			JSONArray JSONtracks = JSONloaded.getJSONObject("toptracks").getJSONArray("track");

			for(int i = 0; i < JSONtracks.length(); i++ ) {
				final JSONObject JSONtrack = JSONtracks.getJSONObject(i);

				// Create a track object for a loaded JSON track
				final Track track = new Track();
				track.loadFromInfo(JSONtrack);

				// If track does not have MBID, we skip it
				if(track.getMBID().equals("")) continue;

				// Make a button for it with rounded corners and the text is the title
				final Button buttonTrack = new Button(amr);
				buttonTrack.setText(track.getTitle());
				buttonTrack.setBackgroundResource(R.layout.rounded_corners);
				buttonTrack.setTextSize(13);

				// The layout has margins
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.FILL_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT);
				lp.setMargins(0, 8, 3, 4); // left, top, right, bottom
				buttonTrack.setLayoutParams(lp);

				// If someone clicks on a track, we display the text loading in it
				buttonTrack.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if(!isDownloading) {
							// to reset the original text of the button, we store it
							// and mark the application as busy
							isDownloading = true;
							clickedButton = buttonTrack;
							clickedText = buttonTrack.getText();

							buttonTrack.setText(R.string.loading);
							DownloadLastFM dl_track = new DownloadLastFM(thisActivity, DownloadLastFM.dl_artist_track_info);
							dl_track.execute(track.getMBID());
						} else {
							Toast.makeText(amr, R.string.msg_already_downloading, Toast.LENGTH_SHORT).show();
						}
					}
				});

				// Finally add it to the scroll list
				tracksScroll.addView(buttonTrack);

			} // end for loop

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/*
	 * This method is called when the downloading of the topalbums has finished.
	 * We add every album to the list (provided they have mbids)
	 */
	public void loadAlbums(String loadedAlbums) {
		try {
			JSONObject JSONloaded = new JSONObject(loadedAlbums);
			if(!JSONloaded.has("topalbums")) {
				albumsMessaging.setText(R.string.artist_loading_albums_failed); 
				return;
			}

			// Delete the message field so there is room for album buttons
			albumsScroll.removeAllViews();

			JSONArray JSONalbums = JSONloaded.getJSONObject("topalbums").getJSONArray("album");

			for(int i = 0; i < JSONalbums.length(); i++ ) {
				// Set a JSON album to a Album object
				final JSONObject JSONalbum = JSONalbums.getJSONObject(i);
				final Album album = new Album(JSONalbum);

				// If no mbid, we skip the work
				if(album.getMbid().equals("")) continue;

				// Create the album button with text: title of the album
				final Button buttonAlbum = new Button(amr);
				buttonAlbum.setText(album.getAlbumTitle());
				buttonAlbum.setBackgroundResource(R.layout.rounded_corners);
				buttonAlbum.setTextSize(13);

				// The layout parameters
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.FILL_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT);
				lp.setMargins(3, 8, 0, 4); // left, top, right, bottom
				buttonAlbum.setLayoutParams(lp);

				// If we click on the album, the loading text informs the user 
				// that the app is downloading it. It will be impossible to click anything else
				buttonAlbum.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if(!isDownloading) {
							// Save the button and text
							isDownloading = true;
							clickedButton = buttonAlbum;
							clickedText = buttonAlbum.getText();

							// Download it
							buttonAlbum.setText(R.string.loading);
							DownloadLastFM dl_album = new DownloadLastFM(thisActivity, DownloadLastFM.dl_artist_album_info);
							dl_album.execute(album.getMbid());
						} else {
							Toast.makeText(amr, R.string.msg_already_downloading, Toast.LENGTH_SHORT).show();
						}
					}
				});

				// Only if album has an MBID we show it
				if(album.getMbid().length() > 0) albumsScroll.addView(buttonAlbum);

			} // end for loop

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/*
	 * When the track returns, restore the button and go to the new activity
	 */
	public void onTrackLoaded(String track) {
		isDownloading = false;
		clickedButton.setText(clickedText);

		Intent trackIntent = new Intent(amr, TrackActivity.class);
		trackIntent.putExtra("track", track);
		System.out.println("Starting TrackActivity intent");
		startActivity(trackIntent);
	}

	/*
	 * When the album returns, restore the button and go to the new activity
	 */
	public void onAlbumLoaded(String album) {
		isDownloading = false;
		clickedButton.setText(clickedText);

		Intent albumIntent = new Intent(amr, AlbumActivity.class);
		albumIntent.putExtra("fromArtist", album);
		System.out.println("Starting AlbumActivity intent");
		startActivity(albumIntent);
	}

}




