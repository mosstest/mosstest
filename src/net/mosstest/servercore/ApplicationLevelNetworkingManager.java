package net.mosstest.servercore;

import net.mosstest.netclient.ClientNetworkingManager;
import net.mosstest.scripting.Position;

// TODO: Auto-generated Javadoc
/**
 * The Class ApplicationLevelNetworkingManager.
 */
public class ApplicationLevelNetworkingManager {
	
	/** The net. */
	ClientNetworkingManager net;
	
	/** The Constant PROTOCOL_VERSION_MAJOR. */
	static final int PROTOCOL_VERSION_MAJOR = 1;
	
	/** The Constant PROTOCOL_VERSION_MINOR. */
	static final int PROTOCOL_VERSION_MINOR = 1; // change for
													// backward-compatible
													// changes such as changes
													// in timing that do not
													// break older versions

	/**
													 * Instantiates a new application level networking manager.
													 *
													 * @param net the net
													 */
													public ApplicationLevelNetworkingManager(ClientNetworkingManager net) {
		this.net = net;
	}

	/**
	 * Send chunk request.
	 *
	 * @param pos the pos
	 */
	public void sendChunkRequest(Position pos) {
		// TODO create generated code

	}
}
