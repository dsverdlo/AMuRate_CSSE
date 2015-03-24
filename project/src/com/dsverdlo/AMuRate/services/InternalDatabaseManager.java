package com.dsverdlo.AMuRate.services;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * DatabaseManager handles the internal database of the project.
 * It is an extension of the class SQLiteOpenHelper.
 * 
 * @author David Sverdlov
 *
 */
public class InternalDatabaseManager extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "AMuRate" + ".db";
	private static final int DATABASE_VERSION = 1;

	/*
	 * Public constructor
	 */
	public InternalDatabaseManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/*
	 * When this is created, see if we have to create the tables.
	 * For every adapter we execute the Table Create SQL
	 */
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(InternalDatabaseRatingAdapter.getSQLTableCreate());
		database.execSQL(InternalDatabaseHistoryAdapter.getSQLTableCreate());
	}

	/*
	 * On upgrading the database we log it and call onCreate
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(InternalDatabaseManager.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		onCreate(db);
	}


	
	
	
	
	


}

