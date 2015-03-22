package com.dsverdlo.AMuRate.services;

import java.io.*;
import java.net.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import com.dsverdlo.AMuRate.gui.TrackActivity;


/**
 * This is the client program which will communicate with the external server/database.
 * On a different thread, a socket is attempted to connect with the server
 * on port 2005 (for no reason, could be some other number also)
 * 
 * Code based on http://stackoverflow.com/questions/1776457/java-client-server-application-with-sockets
 * 
 * @author David Sverdlov
 *
 */

public class ServerConnectPHP extends AsyncTask<String, Void, Double> {

	// request methods
	public static final int ISCONNECTED = 0;
	public static final int SENDRATING = 1;
	public static final int GETRATING = 2;
	public static final int GETAMOUNT = 3;
	public static final int HASRATED = 4;

	// private members
	private TrackActivity activity;
	private DatabaseSyncer syncer;
	private Socket requestSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private String message;
	private int method;


	private int timeOut = 3000; // 3 seconds
	private int portNo = 2005; 


	private String ipAddress;

	public ServerConnectPHP(TrackActivity activity, String ip, int method){ 
		this.activity = activity;
		this.ipAddress = ip;
		this.method = method;
	}


	public ServerConnectPHP(DatabaseSyncer databaseSyncer, String ip, int method) {
		this.syncer = databaseSyncer;
		this.ipAddress = ip;
		this.method = method;
	}


	private void setUpConnection() {
		try{
			//1. creating a socket to connect to the server

			//here you must put your computer's IP address.
			InetAddress serverAddr = InetAddress.getByName(ipAddress);
			System.out.println("[c]Connecting to "+serverAddr.toString()+" in port "+portNo);

			Socket requestSocket = new Socket();
			requestSocket.connect(new InetSocketAddress(serverAddr, portNo), timeOut);

			System.out.println("[c]Connected to ^ in port " + portNo);
			//2. get Input and Output streams
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush(); 
			in = new ObjectInputStream(requestSocket.getInputStream());
			//3: Communicating with the server
		}
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioe){
			System.out.println("IOException in Client.java [setUpConnection]\n"+ioe);
		} catch(Exception e) {
			System.out.println("Exception in Client.java [setUpConnection]\n"+e);
		}
	}

	/*
	 * tearDownConnection closes the in- and out ports and nullifies the socket.
	 */
	private void tearDownConnection() {
		//4: Closing connection
		try{
			if(in != null) in.close();
			if(out != null) out.close();
			if(requestSocket != null) requestSocket.close();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	/*
	 * Helper function for sending a message on the out-stream to the server
	 */
	private void sendMessage(String msg)
	{
		try{
			out.writeObject(msg);
			out.flush();
			System.out.println("[c]client>" + msg);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	/*
	 * SendRating send a rating on the output stream to the server
	 * Then verifies the result
	 * @return boolean on succes
	 */
	private boolean sendRating(String mbid, String artist, String title, double rating, int date, String user) {
		boolean result = false;
		try {
		String urlParameters = "title="+title+"&artist="+artist+"&date="+date+"&rating="+rating+"&user="+user+"&mbid="+mbid+"&apptoken=4444MuR4444t333";
		URL url = new URL("http://dsverdlo.net23.net/testpost.php");
		URLConnection conn = url.openConnection();

		conn.setDoOutput(true);

		OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

		writer.write(urlParameters);
		writer.flush();

		String line;
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

		while ((line = reader.readLine()) != null) {
			if(!line.equals("")) result = (Double.parseDouble(line)>0);
		}
		writer.close();
		reader.close();
		return result;
		
		} catch (Exception ioe) {
			System.out.println("exception generates caz of httpResponse :" + ioe);
			ioe.printStackTrace();
		}
		return result;
	}

	/*
	 * This function checks if the given User has rated the given MBID track
	 */
	private double hasRated(String mbid, String user) {
		double result = -1;
		try{
			URL url = new URL("http://dsverdlo.net23.net/testhasrated.php?user="+user+"&mbid="+mbid);
			URLConnection conn = url.openConnection();

			conn.setDoOutput(true);

			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			while ((line = reader.readLine()) != null) {
				if(!line.equals("")) 
					try { 
						result = Double.parseDouble(line);
					} catch (NumberFormatException e ){
						// text was returned. Invalid response!
						continue;
					}
			}
			reader.close();
			return result;

		}
		catch (Exception e) { e.printStackTrace();} 

		return result;
	}

	/*
	 * This function tests if we can connect with the server
	 */
	private boolean testConnection() {
		try {

			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet("http://dsverdlo.net23.net/testconn.php");

			HttpResponse httpResponse = httpClient.execute(httpGet);
			System.out.println("httpResponse");

			InputStream inputStream = httpResponse.getEntity().getContent();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			StringBuilder stringBuilder = new StringBuilder();
			String bufferedStrChunk = null;
			while((bufferedStrChunk = bufferedReader.readLine()) != null){
				stringBuilder.append(bufferedStrChunk);
			}
			return (stringBuilder.toString().equals("True"));


		} catch (Exception e) {
			System.out.println("Exception in ServerConnectP.java[testConnection]");
			return false; 
		}
	}

	/*
	 * This function gets the average rating of a given MBID 
	 * and the amount of ratings given on it.
	 */
	private double getRatingAvg(String mbid) {
		Double result = (double) 0;
		System.out.println("Getting" + mbid);

		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("http://dsverdlo.net23.net/testget.php?method=avg&mbid="+mbid);

		try {
			HttpResponse httpResponse = httpClient.execute(httpGet);
			System.out.println("httpResponse");

			InputStream inputStream = httpResponse.getEntity().getContent();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			StringBuilder stringBuilder = new StringBuilder();
			String bufferedStrChunk = null;
			while((bufferedStrChunk = bufferedReader.readLine()) != null){
				stringBuilder.append(bufferedStrChunk);
			}
			if(!stringBuilder.toString().equals(""))
				result += Double.parseDouble(stringBuilder.toString());
			// get second
			httpGet = new  HttpGet("http://dsverdlo.net23.net/testget.php?method=amt&mbid="+mbid);
			httpResponse = httpClient.execute(httpGet);
			System.out.println("httpResponse");

			inputStream = httpResponse.getEntity().getContent();
			inputStreamReader = new InputStreamReader(inputStream);
			bufferedReader = new BufferedReader(inputStreamReader);
			stringBuilder = new StringBuilder();
			bufferedStrChunk = null;
			while((bufferedStrChunk = bufferedReader.readLine()) != null){
				stringBuilder.append(bufferedStrChunk);
			}
			if(!stringBuilder.toString().equals(""))
				result += 10 * Double.parseDouble(stringBuilder.toString());
			
			return result;

		} catch (Exception ioe) {
			System.out.println("exception generates caz of httpResponse :" + ioe);
			ioe.printStackTrace();
		}
		return result;
	}

	/*
	 * This method is called when the operations are done communicating 
	 * with the server. We expect a Double result.
	 */
	protected void onPostExecute(Double result) {

		switch(method) {
		case SENDRATING: 
			// Since send rating could alse be done by the DatabaseSyncer
			if(activity != null) { activity.onDoneSendingExternal(result); break; }
			if(syncer != null) { syncer.onDoneSendingSynced(result); break; }

		case GETRATING: activity.onDoneGettingExternal(result); break;

		case ISCONNECTED: 
			if(activity != null) {activity.onDoneTestingExternalConnection(result); break; }
			if(syncer != null) { syncer.onDoneTestingExternalConnection(result); break; }

		case HASRATED: { activity.onDoneCheckingHasRated(result); break; }
		default: return;
		}

	}

	/*
	 * In background we switch on the given method. Depending on that,
	 * we call the private member functions to handle communication.
	 */
	protected Double doInBackground(String... strings) {
		// The strings object has been passed through from the general
		// ServerConnect class, so we had to append the strings into one
		// using a delimiter which is the first string in strings.
		String delimiter = strings[0];
		String[] params = strings[1].split(delimiter);
	
		switch(method) {
		case SENDRATING:
			String mbid = params[0];
			String artist = params[1];
			String title = params[2];
			float rating = Float.parseFloat(params[3]);
			int date = Integer.parseInt(params[4]);
			String user = params[5];
			// If sendRating would return True, we return the Double version of True
			if(sendRating(mbid, artist, title, rating, date, user))	return (double) 1;
			// Otherwise the Double version of False
			return (double) -1;

		case GETRATING:
			return getRatingAvg(params[0]);

		case ISCONNECTED:
			return (double) ((testConnection()) ?  1 : -1) ;

		case HASRATED:
			return hasRated(params[0], params[1]);

		default:
			return (double) -1;
		}
	}





}