package com.dsverdlo.AMuRate.gui;

import org.json.JSONException;
import org.json.JSONObject;

import com.dsverdlo.AMuRate.R;
import com.dsverdlo.AMuRate.objects.AMuRate;
import com.dsverdlo.AMuRate.services.DatabaseSyncer;
import com.dsverdlo.AMuRate.services.DownloadLastFM;
import com.dsverdlo.AMuRate.services.InternalDatabaseHistoryAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * MainActivity is the first screen users see when launching the app.
 * It provides search fields and a help button with instructions.
 * 
 * 
 * @author David Sverdlov
 *
 */
public class MainActivity extends BlankActivity implements OnClickListener {
	private AMuRate amr;
	
	private EditText searchArtist;
	private EditText searchTitle;
	private Button cancelButton;
	private Button sendGetReqButton;
	private Button historyButton;
	private TextView results;
	private TextView questionMark;
	private Button view;
	private AnimationView loading;
	
	private JSONObject searchResults;
	private InternalDatabaseHistoryAdapter ha;

	private int search_option = 0; // 
	private static final int SEARCH_ARTIST = 1;
	private static final int SEARCH_TITLE = 2;
	
	private boolean isDownloading;
	private DownloadLastFM currentDownload;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	

		// If called to exit; finish (called from settings menu)
		if(getIntent().hasExtra("EXIT")) finish();
		
		// Get global vars
		amr = (AMuRate)getApplicationContext();
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		amr.setSCREENHEIGHT(displaymetrics.heightPixels);
		amr.setSCREENWIDTH(displaymetrics.widthPixels);
		
		// Initialize adapter to be able to store new search history
		ha = new InternalDatabaseHistoryAdapter(this);
		isDownloading = false;
		
		// Initialize edit fields
		searchArtist = (EditText) findViewById(R.id.enterArtist);
		searchTitle = (EditText) findViewById(R.id.enterTitle);

		// If title and artist were passed in Extra Bundle, fill them in
		if(getIntent().hasExtra("title") && getIntent().hasExtra("artist")) {
			searchTitle.setText(getIntent().getStringExtra("title"));
			searchArtist.setText(getIntent().getStringExtra("artist"));
		}

		// Get the loading animation, but keep it GONE
		loading = (AnimationView) findViewById(R.id.gif_loading);
		loading.setVisibility(View.GONE);

		// The search button
		sendGetReqButton = (Button) findViewById(R.id.button_search);
		sendGetReqButton.setOnClickListener(this);
		sendGetReqButton.setTextColor(Color.WHITE);
		sendGetReqButton.setText(R.string.search);

		// The cancel button
		cancelButton = (Button) findViewById(R.id.button_cancel);
		cancelButton.setOnClickListener(this);
		cancelButton.setTextColor(Color.WHITE);
		cancelButton.setText(R.string.cancel);

		// The information button has text "?"
		questionMark = (Button) findViewById(R.id.questionmark);
		questionMark.setOnClickListener(this);
		questionMark.setTextColor(Color.LTGRAY);
		questionMark.setText(R.string.questionmark);
		
		// The view button is invisible until results return
		view = (Button) findViewById(R.id.view);
		view.setOnClickListener(this);
		view.setVisibility(View.INVISIBLE);
		view.setText(R.string.view);
		
		// Show the history button
		historyButton = (Button)findViewById(R.id.main_button_history);
		historyButton.setOnClickListener(this);
		historyButton.setTextColor(Color.WHITE);

		// The textview where we place information about the results
		results = (TextView) findViewById(R.id.results);

		// If long clicked on cancel button, quit application
		OnLongClickListener quitAction = new OnLongClickListener() {
			public boolean onLongClick(View v) {
				finish();
				return false;
			}
		};
		cancelButton.setOnLongClickListener(quitAction);

		// Try syncing databases
		new DatabaseSyncer(amr).execute();

	}

	// The onclick function for the whole screen, switch on views
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_search : 

			// Get the values given in EditText fields and their lengths
			String givenArtist = searchArtist.getText().toString();
			String givenTitle = searchTitle.getText().toString();
			int nArtist = givenArtist.length();
			int nTitle = givenTitle.length();
			
			// If a download is already underway, do nothing 
			if(isDownloading) break;
			
			// If no artist and no title, Toast with a message
			if(nArtist == 0 && nTitle == 0 ) {
				Toast.makeText(amr, R.string.main_msg_empty, Toast.LENGTH_LONG).show();
			} else {
				// The following couple of lines hide the soft keyboard
				InputMethodManager inputManager = 
				        (InputMethodManager) amr.getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(
				        this.getCurrentFocus().getWindowToken(),
				        InputMethodManager.HIDE_NOT_ALWAYS);
				
				// By this point we know some information was filled in, so show the loading gif
				loading.setVisibility(View.VISIBLE);
				
				// keep these invisible until the downloading returns
				results.setVisibility(View.INVISIBLE);
				view.setVisibility(Button.INVISIBLE);

				// Save the search terms
				ha.addHistorySearch(givenArtist, givenTitle);

				// If only an artist was filled in
				if(nArtist > 0 && nTitle == 0) {
					currentDownload = new DownloadLastFM(this, DownloadLastFM.dl_main_search_artist);
					currentDownload.execute(givenArtist);
				}
				
				// If only a title was filled in
				if(nArtist == 0 && nTitle > 0) {
					currentDownload = new DownloadLastFM(this, DownloadLastFM.dl_main_search_title);
					currentDownload.execute(givenTitle);
				}
				
				// If both fields were filled in
				if(nArtist > 0 && nTitle > 0) {
					currentDownload = new DownloadLastFM(this, DownloadLastFM.dl_main_search_title_artist);
					currentDownload.execute(givenTitle, givenArtist);	
				}
			}
			break; // break search button

		case R.id.button_cancel :
			// Set result text empty, clear the loading image, hide the view button
			// and clear the search fields
			results.setText("");
			loading.setVisibility(View.GONE);
			view.setVisibility(Button.INVISIBLE);
			searchArtist.setText("");
			searchTitle.setText(""); 
			
			// Also cancel the downloading task if there is one
			if(isDownloading && currentDownload != null) {
				isDownloading = false;
				currentDownload.cancel(true);
				currentDownload = null;
			}

			break;

		case R.id.questionmark :
			// Show an alertDialog with instructions
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle(R.string.main_instructions);
			alertDialog.setMessage(amr.getString(R.string.main_instruction_text));
			// If pressed the OK Button, do nothing special but close the dialog
			alertDialog.setButton(amr.getString(R.string.OK), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// Write code here to execute after dialog closed
				}
			});
			alertDialog.show();
			break;

		case R.id.view : 
			// If view is displayed, there are search results
			
			// If only an artist was entered, start SearchArtistActivity
			if (search_option == SEARCH_ARTIST) {
				Intent newpage = new Intent(this, SearchArtistActivity.class);
				newpage.putExtra("searchResults", searchResults.toString());
				startActivity(newpage);				
			} else {
				// Otherwise SearchResultsActivity
				Intent newpage = new Intent(this, SearchResultsActivity.class);
				newpage.putExtra("searchResults", searchResults.toString());
				startActivity(newpage); 
			}
			break;

		case R.id.main_button_history :
			// If history button is pressed, start its activity
			Intent newpage = new Intent(this, HistoryActivity.class);
			startActivity(newpage);
		}
	}

	/*
	 * This method is called by DownloadLastFM when it returns with results
	 */
	public void searchResultsTitle(String resultString) {
		// Nullify download information
		isDownloading = false;
		currentDownload = null;
		
		// If resultString is completely null, a network error occured
		if(resultString == null) {
			Toast.makeText(amr, amr.getString(R.string.main_no_internet_conn), Toast.LENGTH_SHORT).show();
			loading.setVisibility(View.GONE);
			results.setText(amr.getString(R.string.results) + "...");
			results.setVisibility(View.VISIBLE);
		} else {
			try {
				// If there were results, check if it has a prefix error
				if(!resultString.substring(2, 7).equals("error")) {
					
					// Otherwise create a JSON object from the given string
					JSONObject JSONobject = new JSONObject(resultString);
					// Extract the results from the object
					JSONObject JSONresults = JSONobject.getJSONObject("results");
					// From the results, get the amount of hits found
					int nResults = JSONresults.getInt("opensearch:totalResults");
					if(nResults>0) {
						search_option = SEARCH_TITLE;
												
						// Store the result matches until the user clicks on View button
						searchResults = JSONresults.getJSONObject("trackmatches");
						loading.setVisibility(View.GONE);
						view.setVisibility(Button.VISIBLE);

						// Immediately start next intent
						Intent newpage = new Intent(this, SearchResultsActivity.class);
						newpage.putExtra("searchResults", searchResults.toString());
						startActivity(newpage);	
					}

				}
			} catch (JSONException je) {
				System.out.println("JSON Exception in MainAcitivty(searchResultsTitle):");
				je.printStackTrace();
			}
		}
	}

	/*
	 * This method is called from DownloadLastFM when it's done loading
	 */
	public void searchResultsArtist(String resultString) {
		// Nullify the download information
		isDownloading = false;
		currentDownload = null;
		
		// If nothing returned
		if(resultString == null) {
			Toast.makeText(amr, R.string.main_no_internet_conn, Toast.LENGTH_SHORT).show();
			loading.setVisibility(View.GONE);
			results.setText(amr.getString(R.string.results) + "...");
			results.setVisibility(View.VISIBLE);
		} else {
			try {
				// Otherwise check for error
				if(!resultString.substring(2, 7).equals("error")) {
					// Parse the string to a JSONObject
					JSONObject JSONobject = new JSONObject(resultString);
					// Get the results JSON from the object
					JSONObject JSONresults = JSONobject.getJSONObject("results");
					// Get the amount of results found
					int nResults = JSONresults.getInt("opensearch:totalResults");
					if(nResults>0) {
						// If there are any, set the option to artist
						search_option = SEARCH_ARTIST;
						
						// Save the matches for until the user clicks on the View button
						searchResults = JSONresults.getJSONObject("artistmatches");
						loading.setVisibility(View.GONE);
						view.setVisibility(Button.VISIBLE);
						
						// Immediately start intent
						Intent newpage = new Intent(this, SearchArtistActivity.class);
						newpage.putExtra("searchResults", searchResults.toString());

						startActivity(newpage);	
					}	
				}
			} catch (JSONException je) {
				System.out.println("JSON Exception in MainAcitivty(searchResultsArtist):");
				je.printStackTrace();
			}
		}
	}

	/*
	 * This method is called when DownloadLastFM returns 
	 */
	public void searchResultsTitleAndArtist(String resultString) {
		// Nullify the download information
		isDownloading = false;
		currentDownload = null;
		
		// If nothing returned...
		if(resultString == null || resultString.length() < 7) {
			Toast.makeText(amr, R.string.main_no_internet_conn, Toast.LENGTH_SHORT).show();
			loading.setVisibility(View.GONE);
			results.setText(amr.getString(R.string.results) + "...");
			results.setVisibility(View.VISIBLE);
		} else {
			try {
				// If something returned and it does not start with error
				if(!resultString.substring(2, 7).equals("error")) {
					// Parse the string to a JSONObject
					JSONObject JSONobject = new JSONObject(resultString);
					// Get the results from the object
					JSONObject JSONresults = JSONobject.getJSONObject("results");
					// Get the amount of hits
					int nResults = JSONresults.getInt("opensearch:totalResults");
					if(nResults>0) {
						// If there are any, set the option to title
						search_option = SEARCH_TITLE;

						// Store the results until the user clicks View
						searchResults = JSONresults.getJSONObject("trackmatches");
						loading.setVisibility(View.GONE);
						view.setVisibility(Button.VISIBLE);
						
						// Immediately start intent
						Intent newpage = new Intent(this, SearchResultsActivity.class);
						newpage.putExtra("searchResults", searchResults.toString());
						startActivity(newpage);	
					}
				}
			} catch (JSONException je) {
				System.out.println("JSON Exception in MainAcitivty(searchResultsTitleAndArtist):");
				je.printStackTrace();
			}
		}
	}
	
	
	

}