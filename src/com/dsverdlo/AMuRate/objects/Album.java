package com.dsverdlo.AMuRate.objects;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Album abstract data type. Stores all kind of data from an album record.
 * The public constructor takes one string which is a JSON representation
 * of an album, returned by the api.
 * 
 * @author David Sverdlov
 */

public class Album {
	private String albumTitle;
	private String artistName;
	private int listeners;
	private int playcount;
	private String imageS, imageM, imageL;
	private JSONArray tracks;
	private String summary;
	private String mbid;

	// The top level tags of the JSON 
	private enum AlbumKeys {
		name, artist, id, mbid, url, releasedate, image,
		listeners, playcount, tracks, toptags, wiki
	}

	// When loading JSON, initialize all the members first
	private void initialize() {
		albumTitle = "";
		artistName = "";
		listeners = 0;
		playcount = 0;
		imageS = "";
		imageM = "";
		imageL = "";
		summary = "";
		mbid = "";
		setTracks(new JSONArray());
	}

	/*
	 * This method is called by a loadFromX method.
	 * It assumes the given JSON object is an album
	 */
	private void switchAlbumInfo(JSONObject JSONAlbum) {
		try {
			Iterator<?> it = JSONAlbum.keys();
			String key = "";
			while(it.hasNext()) {
				try { 
					key = (String) it.next();
					switch(AlbumKeys.valueOf(key)) {
					case name: 
						albumTitle = JSONAlbum.getString("name");
						break;

					case artist:
						artistName = JSONAlbum.getString("artist");
						break;

					case listeners:
						listeners = JSONAlbum.getInt("listeners");
						break;

					case playcount:
						playcount = JSONAlbum.getInt("playcount");
						break;
					
					case mbid:
						mbid = JSONAlbum.getString("mbid");
						break;

					case wiki:
						JSONObject JSONwiki = JSONAlbum.getJSONObject("wiki");
						summary = JSONwiki.getString("summary");
						break;
					case tracks:
						JSONObject JSONtracks = JSONAlbum.getJSONObject("tracks");
						// If there is only one track, it is not put in a JSON array
						// So we take the Object and place it in our own array
						if(JSONtracks.get("track").getClass() == JSONObject.class) {
							JSONArray tracksArray = new JSONArray();
							tracksArray.put(JSONtracks.getJSONObject("track"));
							setTracks(tracksArray);
						}else {
							// Otherwise we just set the given array 
						setTracks(JSONtracks.getJSONArray("track"));
						}
						break;

					case image:
						// Image will also be given in an array
						JSONArray imageUrls = JSONAlbum.getJSONArray("image");
						for(int i = 0; i < imageUrls.length(); i++) {
							JSONObject imageUrl = imageUrls.getJSONObject(i);
							String size = imageUrl.getString("size");
							String url = imageUrl.getString("#text");
							if(size.equals("small")) { imageS = url; }
							else if(size.equals("medium")) { imageM = url; }
							else if(size.equals("large")) { imageL = url; }
						}
						break;
					default: break;
					}
				} catch (IllegalArgumentException iae) {
					System.out.println("Illegal argument exception in Album(switchAlbumInfo):" + key);
				}
			}
		}
		catch (JSONException e) {
			System.out.println("JSONException in Album(public constructor)");
			e.printStackTrace();
		}		
	}

	/*
	 * Parse an Album object with a string coming from Extra bundle
	 */
	public Album(String extraAlbum) {
		initialize();

		// Try parsing it to JSON Object and then extracting the album
		try {
			JSONObject JSONobject = new JSONObject(extraAlbum);
			JSONObject JSONAlbum = JSONobject.getJSONObject("album");
			switchAlbumInfo(JSONAlbum);

		} catch (JSONException e) {
			System.out.println("JSONException in Album(public constructor)");
			e.printStackTrace();
		}

	}

	/*
	 * This method is called when the album is already known in JSON
	 */
	public Album (JSONObject JSONAlbum) {
		initialize();
		switchAlbumInfo(JSONAlbum);
	}

	// Getters and setters
	
	public String getAlbumTitle() {
		return albumTitle;
	}
	public void setAlbumTitle(String albumTitle) {
		this.albumTitle = albumTitle;
	}
	public String getArtistName() {
		return artistName;
	}
	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}
	public String getImageL() {
		return imageL;
	}
	public void setImageL(String imageL) {
		this.imageL = imageL;
	}
	public int getPlaycount() {
		return playcount;
	}
	public void setPlaycount(int playcount) {
		this.playcount = playcount;
	}
	public int getListeners() {
		return listeners;
	}
	public void setListeners(int listeners) {
		this.listeners = listeners;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public JSONArray getTracks() {
		return tracks;
	}
	public void setTracks(JSONArray tracks) {
		this.tracks = tracks;
	}
	public String getMbid() {
		return mbid;
	}
}
