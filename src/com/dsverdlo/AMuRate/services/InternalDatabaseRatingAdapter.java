package com.dsverdlo.AMuRate.services;

import com.dsverdlo.AMuRate.objects.Rating;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * This class is an adapter separating the GUI from the database code.
 * It allows to read/write from the internal database.
 * We can add ratings or retrieve average ratings from unique song id's 
 * (mbid).
 * 
 * @author David Sverdlov
 *
 */
public class InternalDatabaseRatingAdapter {
	// Database variables
	private SQLiteDatabase database;
	private InternalDatabaseManager dbm;

	// SQL Statements
	private static final String TABLE_RATINGS = "ratings";
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_MBID = "mbid";
	private static final String COLUMN_NAME = "name";
	private static final String COLUMN_TITLE = "title";
	private static final String COLUMN_RATING = "rating";
	private static final String COLUMN_DATE = "date";
	private static final String COLUMN_SYNCED = "synced";

	private static final String TABLE_CREATE = "create table IF NOT EXISTS " + 
			TABLE_RATINGS + " ( " + 
			COLUMN_ID + " integer primary key autoincrement, " + 
			COLUMN_MBID + " text not null, " + 
			COLUMN_NAME + " text not null, " +
			COLUMN_TITLE + " text not null, " +
			COLUMN_RATING + " real not null, " + 
			COLUMN_DATE + " int not null, " +
			COLUMN_SYNCED + " integer not null " +
			")";

	// SQL Statements
	private static final String SQL_READ_RATING = "SELECT Rating FROM ratings WHERE mbid='%s';";
	private static final String SQL_READ_RATING_AMOUNT = "SELECT COUNT(*) FROM ratings WHERE mbid='%s';";
	private static final String SQL_GET_UNSYNCED_RATINGS = "SELECT * FROM ratings WHERE synced = 0;";
	private static final String SQL_SET_RATINGS_SYNCED = "UPDATE ratings SET synced = '1' WHERE synced = '0';";
	public static final String SQL_GET_ALL_RATINGS = "SELECT * FROM "+TABLE_RATINGS;
	private static final String SQL_DELETE_ALL_RATINGS = "DELETE FROM "+TABLE_RATINGS+ " WHERE "+COLUMN_SYNCED+" = '1'";
	

	/**
	 * RatingAdapter Public constructor for the class.
	 * @param context Context of the application
	 */
	public InternalDatabaseRatingAdapter(Context context) {
		dbm = new InternalDatabaseManager(context);
	}

	/**
	 * readRating Reads the rating of a song from the local database.
	 * @param mbid Unique identifier of the song.
	 * @return float Rating Average. Value is -1 in case something went wrong.
	 */
	public float readRating(String mbid) {
		database = dbm.getWritableDatabase();
		System.out.println("internal DB: reading a rating");
		Cursor results = database.rawQuery(String.format(SQL_READ_RATING, mbid), null);
		if(results != null) {
			if(results.moveToFirst()) {
				database.close();
				return results.getFloat(0);
			} else {
				database.close();
				return -1; 
			}
		} else {
			// Sql error
			database.close();
			System.out.println("SQL error in readRating");
			return -1;
		}
	}

	/**
	 * readRatingAmount Get the amount of ratings for a particular song
	 * @param mbid The unique identifier of the song.
	 * @return boolean True if already rated, false if not
	 */
	public boolean hasRatedBefore(String mbid) {
		database = dbm.getWritableDatabase();
		System.out.println("internal DB: checking if already rated");
		Cursor results = database.rawQuery(String.format(SQL_READ_RATING_AMOUNT, mbid), null);
		if(results != null && results.moveToFirst()) {
			boolean ret = results.getInt(0) > 0;
			database.close();
			return ret;
		}
		database.close();
		return false;
	}

	/**
	 * addRating Add a rating to the local database.
	 * @param mbid The unique identifier of the song.
	 * @param name The name of the artist of the song
	 * @param title The title of the song to be added
	 * @param rating This float is the given rating 
	 * @return insertID The id of the filled row. OR -1 is rated before
	 */
	public long addRating(String mbid, String name, String title, float rating, boolean synced) {
		if(hasRatedBefore(mbid)) return -1;
		database = dbm.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_RATING, rating);
		values.put(COLUMN_MBID, mbid);
		values.put(COLUMN_NAME, name);
		values.put(COLUMN_TITLE, title);
		values.put(COLUMN_SYNCED, (synced) ? 1 : 0);
		values.put(COLUMN_DATE, (int) (System.currentTimeMillis() / 1000L));
		long insertId = database.insert(TABLE_RATINGS, null, values);
		database.close();
		return insertId;
	}


	/**
	 * getUnsyncedRatings Get all the unsynced ratings
	 * @param void
	 * @return Rating[] all the unsynced ratings
	 */
	public Rating[] getUnsyncedRatings() {
		database = dbm.getWritableDatabase();
		System.out.println("internal DB: getting unsynced ratings");
		Cursor results = database.rawQuery(SQL_GET_UNSYNCED_RATINGS, null);
		Rating[] ratings = null;

		// if there are results and we can begin from a first:
		if(results != null && results.moveToFirst()) {

			// Make the array as big as the count of results
			ratings = new Rating[results.getCount()];
			System.out.println("[iDB] " + results.getCount() + " unsynced");

			// For every result: extract the rating and put it in ratings
			for(int i = 0; i < ratings.length; i++) {
				Rating r = new Rating();
				r.setArtist(results.getString(results.getColumnIndex(COLUMN_NAME)));
				r.setMbid(results.getString(results.getColumnIndex(COLUMN_MBID)));
				r.setRating(results.getFloat(results.getColumnIndex(COLUMN_RATING)));
				r.setTitle(results.getString(results.getColumnIndex(COLUMN_TITLE)));
				r.setDate(results.getInt(results.getColumnIndex(COLUMN_DATE)));
				r.setUser(results.getString(results.getColumnIndex(COLUMN_ID)));
				ratings[i] = r;
				results.moveToNext();
			}
		}
		database.close(); 
		return ratings;
	}

	/**
	 * setAllRatingsSynced Set all the ratings in the database synced
	 * @param 
	 * @return boolean depending on success or failure
	 */
	public boolean setAllRatingsSynced() {
		database = dbm.getWritableDatabase();
		System.out.println("internal DB: flaggin all unsynced ratings synced");
		database.execSQL(InternalDatabaseRatingAdapter.SQL_SET_RATINGS_SYNCED);

		database.close();
		return true;
	}


	/**
	 * getSQLTableCreate Retrieve the SQL script to create the table.
	 * @return SQL_TABLE_CREATE
	 */
	public static String getSQLTableCreate() {
		return TABLE_CREATE;
	}

	/**
	 * getRatings Retrieves ratings from the database.
	 * @param sql stating which ratings need to be get
	 * @return Rating[]
	 */
	public Rating[] getRatings(String sql) {
		database = dbm.getWritableDatabase();
		Cursor results = database.rawQuery(SQL_GET_ALL_RATINGS, null);
		Rating[] ratings = null;
		
		// If there are results
		if(results != null && results.moveToFirst()) {
			ratings = new Rating[results.getCount()];
			
			// For each returned rating, create a Rating object
			for(int i = 0; i < results.getCount(); i++) {
				Rating r = new Rating();
				r.setArtist(results.getString(results.getColumnIndex(COLUMN_NAME)));
				r.setDate(results.getInt(results.getColumnIndex(COLUMN_DATE)));
				r.setMbid(results.getString(results.getColumnIndex(COLUMN_MBID)));
				r.setRating(results.getFloat(results.getColumnIndex(COLUMN_RATING)));
				r.setTitle(results.getString(results.getColumnIndex(COLUMN_TITLE)));
				
				ratings[i] = r;
				results.moveToNext();
			}

			database.close();
			return ratings;

		} else {
			// Sql error
			database.close();
			System.out.println("SQL error in readRating");
			return ratings;
		}
	}
	
	/**
	 * Deletes all the synced Ratings in the database
	 * @param Void
	 * @return Void
	 */
	public void deleteRatings() {
		database = dbm.getWritableDatabase();
		
		database.execSQL(SQL_DELETE_ALL_RATINGS);
		
		database.close();
	}
}

