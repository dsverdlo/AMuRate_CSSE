package com.dsverdlo.AMuRate.objects;
/**
 * Abstract Data Type for a rating.
 * A Rating consists of the following members:
 * - MBID of the song
 * - Title of the song
 * - Artist of the song
 * - Rating (float 0-5, step 0.5)
 * - Date (int unix timestamp)
 * - User id of user
 * 
 * @author David Sverdlov
 * @version 1.0
 *
 */
public class History {
		private String mbid, title, artist;
		private int date, key;
		
		/*
		 * Public constructor initializes fields
		 */
		public History() {
			mbid = "";
			title = "";
			artist = "";
			key = 0;
			date = (int) (System.currentTimeMillis() / 1000L);
		}

		// Getters and setters
		public String getMbid() {
			return mbid;
		}

		public void setMbid(String mbid) {
			this.mbid = mbid;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getArtist() {
			return artist;
		}

		public void setArtist(String artist) {
			this.artist = artist;
		}

		public int getDate() {
			return date;
		}

		public void setDate(int date) {
			this.date = date;
		}

		public int getKey() {
			return key;
		}

		public void setKey(int key) {
			this.key = key;
		}
}
