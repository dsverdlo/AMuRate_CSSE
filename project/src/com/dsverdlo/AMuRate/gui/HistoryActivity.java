package com.dsverdlo.AMuRate.gui;

import java.text.SimpleDateFormat;
import java.util.Date;
import com.dsverdlo.AMuRate.R;
import com.dsverdlo.AMuRate.objects.AMuRate;
import com.dsverdlo.AMuRate.objects.History;
import com.dsverdlo.AMuRate.objects.Rating;
import com.dsverdlo.AMuRate.services.DownloadLastFM;
import com.dsverdlo.AMuRate.services.InternalDatabaseHistoryAdapter;
import com.dsverdlo.AMuRate.services.InternalDatabaseRatingAdapter;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends BlankActivity {
	private AMuRate amr;
	private HistoryActivity activity;
	
	private InternalDatabaseHistoryAdapter ha;
	private InternalDatabaseRatingAdapter ra;
	private String textBeforeLoading;
	private Button buttonLoading;

	private LinearLayout lv;
	private Button buttonBack;
	private TextView textTitle;
	private Button buttonDelete;
	private Button buttonOptionSearch;
	private Button buttonOptionTracks;
	private Button buttonOptionRating;

	// option switching system
	private int currentOption;
	private final static int optionSearch = 0;
	private final static int optionTracks = 1;
	private final static int optionRating = 2;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);

		// Initialize members
		activity = this;
		amr = (AMuRate)getApplicationContext();
		ha = new InternalDatabaseHistoryAdapter(amr);
		ra = new InternalDatabaseRatingAdapter(amr);

		// Load the layout 
		lv = (LinearLayout)findViewById(R.id.history_linlay);

		buttonBack = (Button)findViewById(R.id.history_back);
		textTitle = (TextView)findViewById(R.id.history_title);
		buttonDelete = (Button)findViewById(R.id.history_button_remove);
		buttonOptionRating = (Button)findViewById(R.id.history_button_ratings);
		buttonOptionSearch = (Button)findViewById(R.id.history_button_search);
		buttonOptionTracks = (Button) findViewById(R.id.history_button_track);

		// Delete history button deletes depending on current watched history
		buttonDelete.setText(R.string.history_delete);
		buttonDelete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				switch(currentOption) {
				
				// Delete search history
				case optionSearch:
					ha.deleteHistory(InternalDatabaseHistoryAdapter.SQL_DELETE_SEARCH);
					Toast.makeText(amr, R.string.history_deleted_search, Toast.LENGTH_SHORT).show();
					lv.removeAllViews();
					break;
				
				// Delete view tracks history
				case optionTracks:
					ha.deleteHistory(InternalDatabaseHistoryAdapter.SQL_DELETE_TRACK);
					Toast.makeText(amr, R.string.history_deleted_tracks, Toast.LENGTH_SHORT).show();
					lv.removeAllViews();
					break;
				
				// Delete (synced!) tracks
				case optionRating:
					ra.deleteRatings();
					Toast.makeText(amr, R.string.history_deleted_synced_ratings, Toast.LENGTH_SHORT).show();
					lv.removeAllViews();
					break;
				}
			}
		});

		// The backbutton finishes the application (so it goes back to the mainactivity)
		buttonBack.setText(R.string.back);
		buttonBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		textTitle.setText(R.string.history_history);

		// Load the options' onclicklisteners
		buttonOptionRating.setBackgroundResource(R.layout.rounded_corners);
		buttonOptionRating.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(currentOption != optionRating) {
					// if not current active option, set it so.
					// delete the previous views and add our own
					currentOption = optionRating;
					lv.removeAllViews();
					loadRatings();
				} else {
					// Message saying we are already showing requested history
					Toast.makeText(amr, R.string.history_showing_ratings, Toast.LENGTH_SHORT).show();
				} 
			}
		} );

		// Similar to previous onclicklistener
		buttonOptionTracks.setBackgroundResource(R.layout.rounded_corners);
		buttonOptionTracks.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(currentOption != optionTracks) {
					currentOption = optionTracks;
					lv.removeAllViews();
					loadTracks();
				}else {
					Toast.makeText(amr, R.string.history_showing_tracks, Toast.LENGTH_SHORT).show();
				}
			}
		});

		// similar to previous onclicklistener
		buttonOptionSearch.setBackgroundResource(R.layout.rounded_corners);
		buttonOptionSearch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(currentOption != optionSearch) {
					currentOption = optionSearch;
					lv.removeAllViews();
					loadSearch();
				} else {
					Toast.makeText(amr, R.string.history_showing_search, Toast.LENGTH_SHORT).show();
				}
			}
		});

		// Start loading all the search histories for default
		currentOption = optionSearch;
		loadSearch();

	}

	/*
	 * This function converts the date from unix epoch seconds
	 * to human readable string
	 */
	@SuppressLint("SimpleDateFormat")
	private String dateToString(int seconds) {
		Date d = new Date((long) seconds * 1000); // takes milliseconds
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/M/yyyy");
		return sdf.format(d);
	}

	/*
	 * When the download has finished for the clicked track, start TrackActivity
	 */
	public void onDoneLoadingTrackinfo(String trackInfo) {
		buttonLoading.setText(textBeforeLoading);

		Intent nextPage = new Intent(amr, TrackActivity.class);
		nextPage.putExtra("track", trackInfo);
		startActivity(nextPage);
	}

	/*
	 * Gets the ratings from the locale database
	 * If there are none, we say there are no yet
	 * Otherwise we loop and make a button for every one
	 */
	private void loadRatings() {
		Rating[] ratings = ra.getRatings(InternalDatabaseRatingAdapter.SQL_GET_ALL_RATINGS);
		// If there are none
		if(ratings == null) {
			TextView tv = new TextView(amr);
			tv.setText(R.string.history_no_ratings_yet);
			lv.addView(tv);
			return;
		}
		
		// For every rating we make a view in the listview
		for(int i = ratings.length; i > 0; i--) {
			Button b = new Button(amr);
			b.setBackgroundResource(R.layout.rounded_corners);

			// Layout of the button
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			lp.setMargins(8, 10, 8, 4); // left, top, right, bottom
			b.setLayoutParams(lp);

			// Create a Rating object
			Rating r = ratings[i-1];
			final String artist = r.getArtist();
			final String title = r.getTitle();
			final int date = r.getDate();
			float rating = r.getRating();

			// Set the text of the button
			String format = "%s - %s (%.1f " + amr.getString(R.string.stars) + ")";
			b.setText(String.format(format, artist, title, rating));

			// When clicked on the button, display the time of rating in a Toast
			b.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Toast.makeText(amr, amr.getString(R.string.history_on)+dateToString(date), Toast.LENGTH_SHORT).show();
				}
			});
			// When done, add the view to the list and continue
			lv.addView(b);
			continue;
		}
	}

	/*
	 * This method gets the search history
	 * If there is none, say there aren't any searches made
	 * If there are, make a button for every one
	 */
	private void loadSearch() {
		History[] histories = ha.getSearchHistory(InternalDatabaseHistoryAdapter.SQL_GET_ALL_SEARCH);

		// If there is no search history yet, display a textview saying that
		if(histories == null) {
			TextView tv = new TextView(amr);
			tv.setText(R.string.history_no_search_yet);
			lv.addView(tv);
			return;
		}

		// Otherwise, for every history we make a button in the listview
		for(int i = histories.length; i > 0; i--) {
			Button b = new Button(amr);
			b.setBackgroundResource(R.layout.rounded_corners);

			// Make the layout of the button
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			lp.setMargins(8, 10, 8, 4); // left, top, right, bottom
			b.setLayoutParams(lp);

			// Create the History object
			History h = histories[i-1];
			final String artist = h.getArtist();
			final String title = h.getTitle();
			final int date = h.getDate();

			// Set the text
			String format =  amr.getString(R.string.history_one_search) + " %s - %s";
			b.setText(String.format(format, artist, title));

			// If long clicked, we show a Toast with the date
			b.setOnLongClickListener(new OnLongClickListener() {
				public boolean onLongClick(View v) {
					Toast.makeText(amr, amr.getString(R.string.history_on)+dateToString(date), Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			
			// If short clicked, we finish this activity, and start a MainActivity,
			// where the fields are filled in with the arguments from the current button
			b.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					Intent next = new Intent(amr, MainActivity.class);
					next.putExtra("title", title);
					next.putExtra("artist", artist);
					finish();
					next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(next);
				}
			});
			// Add to the list and continue
			lv.addView(b);
			continue;
		}
	}

	/*
	 * This method gets the tracks viewed in detail
	 * If there are none, a message will be displayed
	 * Other wise a button for each history
	 */
	private void loadTracks() {
		History[] histories = ha.getSearchHistory(InternalDatabaseHistoryAdapter.SQL_GET_ALL_TRACKS);

		// If there is no track history yet, display a textview saying that
		if(histories == null) {
			TextView tv = new TextView(amr);
			tv.setText(R.string.history_no_tracks_yet);
			lv.addView(tv);
			return;
		}

		// Otherwise, for every history we make a button in the listview
		for(int i = histories.length; i > 0; i--) {
			final Button b = new Button(amr);
			b.setBackgroundResource(R.layout.rounded_corners);

			// The layout
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			lp.setMargins(8, 10, 8, 4); // left, top, right, bottom
			b.setLayoutParams(lp);

			// Create a history object
			History h = histories[i-1];
			final String artist = h.getArtist();
			final String title = h.getTitle();
			final String mbid = h.getMbid();
			final int date = h.getDate();

			// Set the title of the button
			String format = amr.getString(R.string.history_one_track) + " %s - %s";
			b.setText(String.format(format, artist, title));
			
			// On long click: display date
			b.setOnLongClickListener(new OnLongClickListener() {
				public boolean onLongClick(View v) {
					Toast.makeText(amr, amr.getString(R.string.history_on)+dateToString(date), Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			
			// On short click, download info of track and set text to loading
			b.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					textBeforeLoading = (String) b.getText();
					buttonLoading = b;
					b.setText(amr.getString(R.string.history_one_track) + amr.getString(R.string.loading));
					DownloadLastFM dl = new DownloadLastFM(activity, DownloadLastFM.dl_history_track_info);
					dl.execute(mbid);
				}
			});
			// Add to view and continue
			lv.addView(b);
			continue;
		}
	}
}
