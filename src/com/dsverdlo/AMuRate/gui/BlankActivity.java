package com.dsverdlo.AMuRate.gui;

import java.util.Locale;

import org.apache.http.conn.util.InetAddressUtils;

import com.dsverdlo.AMuRate.R;
import com.dsverdlo.AMuRate.objects.AMuRate;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * This BlankActivity is an extension of the Activity class. Every activity
 * in the AMuRate GUI will extend the BlankActivity. The settings menu is
 * defined in this activity and should be available in every activity.
 * 
 * @author David Sverdlov
 *
 */
public class BlankActivity extends Activity {
	private AMuRate amr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		amr = (AMuRate) getApplicationContext();
	}

	/**
	 * Create the options menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// First item: refresh: reloads the currect screen
		menu.addSubMenu(Menu.NONE, 199, 0, amr.getString(R.string.blank_menu_refresh)).setIcon(android.R.drawable.ic_menu_rotate);

		// Second item: change language. To add languages, extend the menu here
		SubMenu langMenu = menu.addSubMenu(0, 200, 1, R.string.language).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
		langMenu.add(1, 201, 0, amr.getString(R.string.dutch));
		langMenu.add(1, 202, 0, amr.getString(R.string.french));
		langMenu.add(1, 203, 0, amr.getString(R.string.english));

		// Item: set IP/port number
		menu.addSubMenu(Menu.NONE, 197, 3, amr.getString(R.string.blank_menu_set_ip_port)).setIcon(android.R.drawable.ic_menu_manage);

		// Item: quit application
		menu.addSubMenu(Menu.NONE, 198, 4, amr.getString(R.string.blank_menu_quit)).setIcon(android.R.drawable.ic_lock_power_off);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * This method defines the actions to be taken when a menu item is pressed.
	 * To add a language (or other menu action), add it in this switch
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){

		case 197: actionSetIp(); break;

		case 198: actionQuit();	break;

		case 199: actionRefresh(); break;

		case 201: 
			actionSetLanguage("nl", amr.getString(R.string.blank_switched_dutch));
			// Then reload the current activity to update the strings
			actionRefresh();
			break;

		case 202:
			actionSetLanguage("fr", amr.getString(R.string.blank_switched_french));
			actionRefresh();
			break;  

		case 203:
			actionSetLanguage("en", amr.getString(R.string.blank_switched_english));
			actionRefresh();
			break;  
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * This method quits the application by clearing the acitivty stack
	 * and starting the mainActivity with Extra "exit"
	 */
	private void actionQuit() {
		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("EXIT", true);
		startActivity(intent);
	}

	/*
	 * The refresh option reloads the current activity. This will load
	 * a new language or set the new port/ip fields.
	 */
	private void actionRefresh() {
		finish();
		Intent refresh = new Intent(amr, getClass());
		if(getIntent().getExtras() != null) refresh.putExtras(getIntent().getExtras());
		startActivity(refresh);
	}

	/*
	 * This method sets a given language and displays a toast afterwards
	 */
	private void actionSetLanguage(String lang, String toast) {
		Locale locale = new Locale(lang); 
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
		Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
	}

	/*
	 * This method displays a pop up with two fields where the user can enter text.
	 * The first field is for the ip address and the second for the port number
	 * The current settings are filled in by default
	 */
	private void actionSetIp() {		
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		// Set the title of the dialog
		alert.setTitle(R.string.blank_set_port_ip);

		// Create a vertical layout for all the Views
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		// Set an TextView and EditText to get user input on IP 
		final TextView ipText = new TextView(this);
		ipText.setText(R.string.blank_enter_ip);
		ll.addView(ipText);

		final EditText inputIp = new EditText(this);
		inputIp.setText(amr.getIp());
		ll.addView(inputIp);

		// Set an TextView and EditText to get user input on port
		final TextView portText = new TextView(this);
		portText.setText(R.string.blank_enter_port);
		ll.addView(portText);

		final EditText inputPort = new EditText(this);
		inputPort.setText(""+ amr.getPort());
		ll.addView(inputPort);

		// Create our own buttons so we control when the dialog may close
		Button ok = new Button(this);
		ok.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f));
		ok.setText(amr.getString(R.string.blank_validate));
		// We will add the OnClickListener later so we can use the dialog we are building

		// Custom cancel button
		Button cancel = new Button(this);
		cancel.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f));
		
		cancel.setText(amr.getString(R.string.blank_cancel));
		// Adding the listener later

		// Create a horizontal layout for the two buttons
		LinearLayout buttons = new LinearLayout(this);
		buttons.setGravity(Gravity.CENTER_HORIZONTAL);
		buttons.setPadding(10, 5, 10, 5);
		LinearLayout.LayoutParams buttonsLayoutParams = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		buttonsLayoutParams.setMargins(0, 20, 0, 5);
		
		buttons.setLayoutParams(buttonsLayoutParams);
		buttons.setBackgroundColor(Color.DKGRAY);
		buttons.addView(ok);
		buttons.addView(cancel);

		// Add the button layout to the biggest layout
		ll.addView(buttons);

		// Set the biggest layout as the View of the dialog
		alert.setView(ll);

		// Create the dialog, but before we Show() it, we implement the OnClickListeners
		final AlertDialog alertdialog = alert.create();

		// Cancel button dismisses the dialog
		// We made our own custom button for this so it can be next to the Validate button
		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				alertdialog.dismiss();
			}
		});

		// Validate button will validate both fields and give them a color
		// according to their validation output. Green=good, red=bad
		ok.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				// This boolean will tell us if we can dismiss the dialog
				boolean mayDismiss = true;

				// Validate the port number
				int portNo = amr.getPort();
				try { 
					// Check that it is an integer
					portNo = Integer.parseInt(inputPort.getText().toString());
					// Then, according to this website:
					// http://www.tcpipguide.com/free/t_TCPIPApplicationAssignmentsandServerPortNumberRang-2.htm
					// A good port range is between 1024 and 49151
					if(portNo < 1024 || portNo > 49151)
						throw new Exception();
					// If no exceptions thrown, we are good
					portText.setTextColor(Color.GREEN);
				} catch (Exception e) {
					// Something went wrong, don't close after validating IP address
					portText.setTextColor(Color.RED);
					mayDismiss = false;
				}

				// Validate the IP address
				String ip = inputIp.getText().toString();
				if(!InetAddressUtils.isIPv4Address(ip)) {
					ipText.setTextColor(Color.RED);
					mayDismiss = false;
				} else {
					ipText.setTextColor(Color.GREEN);
				}

				// If we may not dismiss because of a failed validations, return
				if(!mayDismiss) return;

				// Otherwise set the details and dismiss the dialog.
				// After dismissing we show a Toast of success and refresh the activity
				amr.setIp(ip);
				amr.setPort(portNo);
				alertdialog.dismiss();
				Toast.makeText(amr, R.string.msg_config_set, Toast.LENGTH_SHORT).show();
				actionRefresh();
			}
		});

		// Finally we can show the dialog.
		alertdialog.show();
	}
}
