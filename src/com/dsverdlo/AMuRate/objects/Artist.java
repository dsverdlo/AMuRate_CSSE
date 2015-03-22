package com.dsverdlo.AMuRate.objects;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Artist abstract data type. Stores all kind of data from an artist.
 * The public constructor takes one string which is a JSON representation
 * of an album, returned by the api.
 * 
 * @author David Sverdlov
 */

public class Artist implements Parcelable {
	private String artistName;
	private int listeners;
	private String imageS, imageM;
	private String imageL, imageXL;
	private String url;
	private String mbid;
	
	// more fields from getInfo
	private int playcount;
	private boolean streamable; 
	private String urlOut;
	private String summary;
	private String content;
	
	// Top level keys for Artist JSON coming from search
	private enum ArtistKeys {
		name, mbid, url, image,	listeners, streamable
	}
	
	// Top level keys for Artist JSON coming from getInfo
	private enum ArtistInfoKeys {
		name, mbid, url, image,	stats, streamable, bio,
		listeners, playcount, ontour, similar, tags,
		links, published, summary, content, placeformed,
		yearformed, formationlist
	}
	
	/*
	 * This method is called by every constructor to initialize the members
	 */
	private void initialize() {
		artistName = "";
		listeners = 0;
		imageS = "";
		imageM = "";
		imageL = "";
		imageXL = "";
		url = "";
		setMbid("");
		
		playcount = 0;
		streamable = false;
		urlOut = "";
		summary = "";
		content = "";	
	}
	
	/*
	 * Default constructor initializes the members
	 */
	public Artist() {
		initialize();
	}
	
	/*
	 * This method is called with a string of JSON from the Extra bundle
	 */
	public void loadFromSearch(String extraArtist) {
		initialize();
		
		// Try parsing it to a JSONObject
		try {
			JSONObject JSONArtist = new JSONObject(extraArtist);
			Iterator<?> it = JSONArtist.keys();
			String key = "";
			while(it.hasNext()) {
				try { 
					key = (String) it.next();
					switch(ArtistKeys.valueOf(key)) {
					case name: 
						artistName = JSONArtist.getString("name");
						break;
						
					case listeners:
						listeners = JSONArtist.getInt("listeners");
						break;
					
					case url:
						url = JSONArtist.getString("url");
						break;
						
					case mbid:
						setMbid(JSONArtist.getString("mbid"));
						break;
												
					case image:
						// Image is given in an array
						JSONArray imageUrls = JSONArtist.getJSONArray("image");
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
					System.out.println("Error: illegal argument exception in Album: " + key);
				}
			}

		} catch (JSONException e) {
			System.out.println("JSONException in Album(public constructor)");
			e.printStackTrace();
		}
	}

	/*
	 * This method is called from getInfo
	 */
	public void loadFromInfo(String stringExtra) {
		initialize();
		try {
			JSONObject JSONArtist = new JSONObject(stringExtra);
			Iterator<?> it = JSONArtist.keys();
			String key = "";
			while(it.hasNext()) {
				try { 
					key = (String) it.next();
					switch(ArtistInfoKeys.valueOf(key)) {
					case name: 
						artistName = JSONArtist.getString("name");
						break;
						
					case stats:
						JSONObject JSONstats = JSONArtist.getJSONObject("stats");
						listeners = JSONstats.getInt("listeners");
						playcount = JSONstats.getInt("playcount");
						break;
					
					case url:
						url = JSONArtist.getString("url");
						break;
						
					case mbid:
						setMbid(JSONArtist.getString("mbid"));
						break;
												
					case image:
						JSONArray imageUrls = JSONArtist.getJSONArray("image");
						for(int i = 0; i < imageUrls.length(); i++) {
							JSONObject imageUrl = imageUrls.getJSONObject(i);
							String size = imageUrl.getString("size");
							String url = imageUrl.getString("#text");
							if(size.equals("small")) { imageS = url; }
							else if(size.equals("medium")) { imageM = url; }
							else if(size.equals("large")) { imageL = url; }
							else if(size.equals("extralarge")) { imageXL = url; }
						}
						break;
					case bio:
						JSONObject JSONbio = JSONArtist.getJSONObject("bio");
						Iterator<?> itBio = JSONbio.keys();
						while(itBio.hasNext()) {
							String keyBio = (String) itBio.next();
							switch(ArtistInfoKeys.valueOf(keyBio)) {
							case links: 
								JSONObject links = JSONbio.getJSONObject("links");
								JSONObject link = links.getJSONObject("link");
								urlOut = link.getString("href");
								break;
							case summary:
								summary = JSONbio.getString("summary");
								break;
							case content:
								content = JSONbio.getString("content");
								break;
							default:
								break;
							}
						}
					default: break;
					}
				} catch (IllegalArgumentException iae) {
					System.out.println("Error: illegal argument exception in Album: "+key);
				}
			}

		} catch (JSONException e) {
			System.out.println("JSONException in Artist.java Album(public constructor)");
			e.printStackTrace();
		}
	}
	
	// Getters and setters
	public String getArtistName() {
		return artistName;
	}

	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}

	public int getListeners() {
		return listeners;
	}

	public void setListeners(int listeners) {
		this.listeners = listeners;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getImage(String size) {
		if(size.equals("s") || size.equals("S")) return imageS;
		if(size.equals("m") || size.equals("M")) return imageM;
		if(size.equals("l") || size.equals("L")) return imageL;
		if(size.equals("xl") || size.equals("XL")) return imageXL;
		return "";
	}

	public String getMbid() {
		return mbid;
	}

	public void setMbid(String mbid) {
		this.mbid = mbid;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel arg0, int arg1) {		
	}

	public String getContent() {
		return this.content;
	}

	public String getUrlOut() {
		// Returns a html link to the Wiki page
		return "<a href="+this.urlOut+"> Wiki </a>";
	}

	public String getSummary() {
		return this.summary;
	}
}
