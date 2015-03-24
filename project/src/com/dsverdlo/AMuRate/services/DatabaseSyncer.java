package com.dsverdlo.AMuRate.services;

import android.content.Context;
import android.os.AsyncTask;

import com.dsverdlo.AMuRate.objects.AMuRate;
import com.dsverdlo.AMuRate.objects.Rating;

/**
 * This class will check if the local database has unsynced ratings
 * if so it will copy them from the internal one and try to send em
 * to the external database.
 * 
 * @author David Sverdlov
 *
 */
public class DatabaseSyncer extends AsyncTask<Void, Void, Void> {

	// private members 
	private InternalDatabaseRatingAdapter ra;
	private int amtOfSyncs;
	private int amtOfSyncsReceived;
	private int amtOfSyncsReceivedGood;
	private Rating[] unsynced;
	private String user;
	private AMuRate amr;

	// public constuctor instantiates members
	public DatabaseSyncer(AMuRate amr) {
		this.amr = amr;
		this.user = amr.getUser();
		ra = new InternalDatabaseRatingAdapter(amr);
		amtOfSyncs = 0;
		amtOfSyncsReceived = 0;
		amtOfSyncsReceivedGood = 0;
	}

	// Sync function runs on a different thread than UI
	@Override
	protected Void doInBackground(Void... params) {
		System.out.println("DatabaseSyncer is syncing in background...");
		// Get all the unsynced tracks
		unsynced = ra.getUnsyncedRatings();

		// If there are any: make a server connection and test if we can connect
		if(unsynced != null && unsynced.length > 0) {
			amtOfSyncs = unsynced.length;
			new ServerConnect(this, amr, ServerConnect.ISCONNECTED).execute();

		} else {
			System.out.println("DatabaseSyncer: Guess there was nothing to sync..");
		}
		return null;		
	}


	/*
	 * This method is called every time a rating result returns.
	 */
	public void onDoneSendingSynced(Double result) {
		// Update with new result info		
		if(result > 0) {
			// Rating got sent succesfully to ext db, so we add1 to received and good
			System.out.println("DatabaseSyncer: succes");
			amtOfSyncsReceived++;
			amtOfSyncsReceivedGood++; 
		} else {
			// Rating got lost or something so don't add1 the good count
			System.out.println("DatabaseSyncer: fail");
			amtOfSyncsReceived++;		
		}


		// Now check if we are done
		if(amtOfSyncsReceived == amtOfSyncs) {
			
			// if they all returned check if they all passed
			if(amtOfSyncsReceivedGood == amtOfSyncs) {
				// They all were sent fine so flag them synced
				if(ra.setAllRatingsSynced()) {
					System.out.println("[iDB] All tracks are now synced");
				} else {
					System.out.println("[iDB] Something went wrong flagging synced");
				}
			} else {
				// They did all return but not good so we won't flag them as passed
				System.out.println("DatabaseSyncer: not all tracks got sent so aborting sync");
				return;
			}
		}
	}

	/*
	 * When the serverconnection returns with connection result
	 */
	public void onDoneTestingExternalConnection(Double result) {
		if(result < 0 ) {
			System.out.println("DatabaseSyncer: no connection with ext database, guess we'll try later");
		} else {
			for(int i = 0; i < unsynced.length; i++) {

				// Before doing any unnecessary work: check if no ratings got lost
				if(amtOfSyncsReceived > amtOfSyncsReceivedGood) {
					System.out.println("DatabaseSyncer: a rating didnt make it to the database. Aborting sync.");
					return;
				}
 
				Rating r = unsynced[i];  
				System.out.println("DatabaseSyncer: executing unsynced request");
				
				// Execute the server connection
				new ServerConnect(this, amr, ServerConnect.SENDRATING).execute(
						r.getMbid(),
						r.getArtist(),
						r.getTitle(),
						""+r.getRating(),
						""+r.getDate(),
						user);
				}
		}
	}
}

