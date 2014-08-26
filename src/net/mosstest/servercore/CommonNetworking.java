package net.mosstest.servercore;

// TODO: Auto-generated Javadoc
/**
 * The Class CommonNetworking.
 */
public class CommonNetworking {
	
	/** The constant integer value representing the magic value sent before each packet. */
	public static final int MAGIC =0xfa7d2e4a;
	
	/** An unused magic value that would be used when UDP packets not needing an ACK are sent, */
	public static final int MAGIC_NO_ACK =0xfa7d2e4f;
	
	/** An unused magic value that indicates an ACK for a UDP packet */
	public static final int MAGIC_ACK =0xfa7d2740;

}
