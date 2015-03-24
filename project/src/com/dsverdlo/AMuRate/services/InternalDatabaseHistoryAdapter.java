package com.dsverdlo.AMuRate.services;

import com.dsverdlo.AMuRate.objects.History;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * This class is an adapter separating the GUI from the database code.
 * It allows to open, close and read/write from the database.
 * This adapter creates, reads and updates the history data in the 
 * History table
 * 
 * So far 2 kinds of history items exist:
 * 1. Search history (_id, name, title, date)
 * 2. Track history (_id, name, title, mbid, date)
 * Since these  have a lot of fields in common, they will be put into one table
 * 
 * @author David Sverdlov
 *
 */
public class InternalDatabaseHistoryAdapter {
	// Members
	private SQLiteDatabase database;
	private InternalDatabaseManager dbm;
	
	// History options
	public final static int KEY_SEARCH = 1;
	public final static int KEY_TRACK = 2;

	// SQL variables
	private final static String TABLE_HISTORY = "history";
	
	private final static String COLUMN_ID = "_id";
	private final static String COLUMN_HISTORY_KEY = "history_key";
	private final static String COLUMN_DATE = "date";
	private final static String COLUMN_NAME = "name";
	private final static String COLUMN_TITLE = "title";
	private final static String COLUMN_MBID = "mbid";
	
	private final static String SQL_TABLE_CREATE = "create table IF NOT EXISTS " + 
			TABLE_HISTORY + " ( " +
			COLUMN_ID + " integer primary key autoincrement, " + 
			COLUMN_HISTORY_KEY + " integer not null, " + 
			COLUMN_DATE + " int not null," +
			COLUMN_NAME + " text not null," +
			COLUMN_TITLE + " text not null," +
			COLUMN_MBID + " text not null" +
			");";

	public final static String SQL_GET_ALL = "SELECT * FROM " + TABLE_HISTORY;
	public final static String SQL_GET_ALL_SEARCH = 
			String.format("SELECT %s FROM %s WHERE %s", "*", TABLE_HISTORY, COLUMN_HISTORY_KEY + " = " + KEY_SEARCH);
	public final static String SQL_GET_ALL_TRACKS = 
			String.format("SELECT %s FROM %s WHERE %s", "*", TABLE_HISTORY, COLUMN_HISTORY_KEY + " = " + KEY_TRACK);
	
	public final static String SQL_DELETE_ALL = "DELETE FROM " + TABLE_HISTORY;
	public final static String SQL_DELETE_SEARCH = 
			String.format("DELETE FROM %s WHERE %s", TABLE_HISTORY, COLUMN_HISTORY_KEY + " = " + KEY_SEARCH);
	public final static String SQL_DELETE_TRACK = 
			String.format("DELETE FROM %s WHERE %s", TABLE_HISTORY, COLUMN_HISTORY_KEY + " = " + KEY_TRACK);
	
	/*
	 * Public constructor makes a new databasemanager
	 */
	public InternalDatabaseHistoryAdapter(Context context) {
		dbm = new InternalDatabaseManager(context);
	}

	/*
	 * addHistorySearch adds a pair (name, title) from the search fields
	 * to the local search history table.
	 * 
	 * @param name Given name
	 * @param title Given title
	 * @return id of the row in the database where the entry was placed.
	 */
	public long addHistorySearch(String name, String title) {
		database = dbm.getWritableDatabase();
		ContentValues values = new ContentValues();
	    values.put(COLUMN_HISTORY_KEY, KEY_SEARCH);
	    values.put(COLUMN_MBID, "");
	    values.put(COLUMN_NAME, name);
	    values.put(COLUMN_TITLE, title);
	    values.put(COLUMN_DATE, (int) (System.currentTimeMillis() /1000L));
	    long insertId = database.insert(TABLE_HISTORY, null, values);
		database.close();
		return insertId;
	}
	
	/*
	 * This function is called when a user views a track page.
	 * We store the information of the page (artist, title)
	 * 
	 * @param mbid
	 * @param name
	 * @param title
	 * @return row of inserted entry.
	 */
	public long addHistoryTrack(String mbid, String name, String title) {
		database = dbm.getWritableDatabase();
		ContentValues values = new ContentValues();
	    values.put(COLUMN_HISTORY_KEY, KEY_TRACK);
	    values.put(COLUMN_MBID, mbid);
	    values.put(COLUMN_NAME, name);
	    values.put(COLUMN_TITLE, title);
	    values.put(COLUMN_DATE, (int) (System.currentTimeMillis() / 1000L));
	    long insertId = database.insert(TABLE_HISTORY, null, values);
		database.close();
		return insertId;
	}
	
	/*
	 * GetSearchHistory takes a query from this class. It could be all
	 * or just the track or search history. We must check that it is
	 * in fact a SELECT statement.
	 */
	public History[] getSearchHistory(String query) {
		History[] histories = null;

		// If not a select query; return empty
		if(!query.startsWith("SELECT")) return histories; 
		
		database = dbm.getReadableDatabase();

		// Do the query and get a cursor to the results
		Cursor cursor = database.rawQuery(query, null);
		
		// If the cursor could be set and we can move it to first, 
		// start making History objects from the results
		if(cursor != null && cursor.moveToFirst()) {
			histories = new History[cursor.getCount()];
			for(int i = 0; i < cursor.getCount(); i++) {
				History h = new History();
				h.setArtist(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
				h.setDate(cursor.getInt(cursor.getColumnIndex(COLUMN_DATE)));
				h.setKey(cursor.getInt(cursor.getColumnIndex(COLUMN_HISTORY_KEY)));
				h.setMbid(cursor.getString(cursor.getColumnIndex(COLUMN_MBID)));
				h.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
				
				histories[i] = h;
				cursor.moveToNext();
			}
		}
		cursor.close();
		database.close();
		return histories;

	}
	
	/*
	 * Deletes history. Could be all, just track or search history.
	 */
	public void deleteHistory(String sql) {
		database = dbm.getWritableDatabase();
		
		// small check to see if correct sql was provided
		if(sql.startsWith("DELETE")) database.execSQL(sql);
		
		database.close();
	}
	
	/*
	 * Return the SQL to create this table for the first time
	 * (or when a user manually deleted the app data)
	 */
	public static String getSQLTableCreate() {
		return SQL_TABLE_CREATE;
	}

} 