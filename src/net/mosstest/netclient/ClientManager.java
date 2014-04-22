package net.mosstest.netclient;

import net.mosstest.servercore.ApplicationLevelNetworkingManager;
import net.mosstest.servercore.EngineSettings;

import java.io.IOException;


// TODO: Auto-generated Javadoc
/**
 * The Class ClientManager.
 */
public class ClientManager {
	
	/** The state. */
	volatile static int state=0;
	
	/** The Constant STATE_DISCONNECTED. */
	static final int STATE_DISCONNECTED=0;
	
	/** The Constant STATE_FETCHING_SCRIPTS. */
	static final int STATE_FETCHING_SCRIPTS=1;
	
	/** The Constant STATE_FETCHING_TEXTURES. */
	static final int STATE_FETCHING_TEXTURES=2;
	
	/** The Constant STATE_FETCHING_MESHES. */
	static final int STATE_FETCHING_MESHES=3;
	
	/** The Constant STATE_PLAYABLE. */
	static final int STATE_PLAYABLE=4;
	
	/** The Constant STATE_INVALID. */
	static final int STATE_INVALID=-1;
	
	/** The net. */
	static ClientNetworkingManager net;
	
	/**
	 * Inits the.
	 *
	 * @param host the host
	 * @param port the port
	 * @param username the username
	 * @param password the password
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	static void init(String host, int port, String username, char[] password) throws IOException{
		net=new ClientNetworkingManager(host, port, EngineSettings.getBool("client_udp", true)); //$NON-NLS-1$
		net.beginConnectHandshake();
		
	}

	/**
	 * Gets the networking manager.
	 *
	 * @return the networking manager
	 */
	public static ClientNetworkingManager getNetworkingManager() {
		return net;
	}

	/**
	 * Gets the application level networking manager.
	 *
	 * @return the application level networking manager
	 */
	public static ApplicationLevelNetworkingManager getApplicationLevelNetworkingManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Show an error screen on the client.
	 *
	 * @param title The title to show
	 * @param msg The message to show
	 * @param disconnect Whether to disconnect from the current server, if any.
	 */
	public static void showErrorScreen(String title, String msg, boolean disconnect) {
		// TODO Auto-generated method stub
		
	}
}
