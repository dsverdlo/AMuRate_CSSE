package com.dsverdlo.AMuRate.objects;

import java.util.Locale;

import com.dsverdlo.AMuRate.services.InternalDatabaseManager;
import com.dsverdlo.AMuRate.services.ServerConnect;

import android.app.Application;
import android.content.res.Configuration;
/**
 * This class is the main Application. 
 * It is used for cross activity communication and use of variables
 * throughout the entire application. 
 * 
 * @author David Sverdlov
 *
 */
public class AMuRate extends Application {
	//will be lazily loaded...
	private InternalDatabaseManager localConnection;
	
	// Some important variables
	private String ip;
	private int portNo;
	private int serverConnectionType;
	
	private String USER_ID;
	private int SCREENWIDTH;
	private int SCREENHEIGHT;

	public void onCreate(){
		super.onCreate();
		//database setup: this can take some time on first run
		System.out.println("AMR: get local connection");
		this.getLocalConnection();
		System.out.println("AMR: got local connection");

		//			ip = "localhost"; // local
		//			ip = "81.164.233.130"; // thuis
//		ip = "134.184.120.178"; // kot 
		//			ip = "10.2.33.36"; // urbizone
		//			ip = "134.184.108.145"; // edoroam
					ip = "134.184.140.70"; // vubnet
		//			ip = "194.168.5.43"; // 3G
		//			ip = "10.0.1.97"; // como
		portNo = 2005;
		serverConnectionType = ServerConnect.USEMYSQL;
		
		// Grab the user AndroidID
		USER_ID = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

		// Set the default language on english
		Configuration config = new Configuration();
		config.locale = Locale.ENGLISH;
		getResources().updateConfiguration(config, null);
	}
	
	/*
	 * lazy loading of the local database on its first use
	 * @return a connection to the local database (SQLite)
	 */
	public synchronized InternalDatabaseManager getLocalConnection() {
		if (this.localConnection == null) {
			this.localConnection = new InternalDatabaseManager(this.getApplicationContext());
		}
		return this.localConnection;
	}
	// Getters and setters
	public void setIp(String ip) {
		serverConnectionType = ServerConnect.USEMYSQL;
		this.ip = ip;
	}
	public String getIp() {
		return ip;
	}
	public String getUser() {
		return USER_ID;
	}
	public int getSCREENWIDTH() {
		return SCREENWIDTH;
	}
	public void setSCREENWIDTH(int SCREENWIDTH) {
		this.SCREENWIDTH = SCREENWIDTH;
	}
	public int getSCREENHEIGHT() {
		return SCREENHEIGHT;
	}
	public void setSCREENHEIGHT(int SCREENHEIGHT) {
		this.SCREENHEIGHT = SCREENHEIGHT;
	}

	public int getPort() {
		return portNo;
	}

	public void setPort(int portNo) {
		this.portNo = portNo;
		serverConnectionType = ServerConnect.USEMYSQL;
	}

	public int getServerConnectionType() {
		return serverConnectionType;
	}
	
	public void switchConnectionType() {
		System.out.println("AMURATE SIWTCHING");
		if(serverConnectionType == ServerConnect.USEMYSQL)
			serverConnectionType = ServerConnect.USEPHP;
		else 
			serverConnectionType = ServerConnect.USEMYSQL;
	}


}
