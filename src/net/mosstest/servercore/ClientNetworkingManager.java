package net.mosstest.servercore;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * The Class ClientNetworkingManager.
 */
public class ClientNetworkingManager {

	/** The logger. */
	static Logger logger = Logger.getLogger(ClientNetworkingManager.class);
	// There's a potential DoS attack here but it can only be mounted by the
	// server, so you might as well just not use that server. No security
	// threat except small client hang, won't fix.
	/** The run threads. */
	protected AtomicBoolean runThreads = new AtomicBoolean(true);
	
	/** The bulk data socket. */
	protected Socket bulkDataSocket = new Socket();
	
	/** The low latency stream socket. */
	protected Socket lowLatencyStreamSocket = new Socket();
	
	/** The udp socket. */
	protected DatagramSocket udpSocket;
	
	/** The bulk data out. */
	protected DataOutputStream bulkDataOut;
	
	/** The lowlatency data out. */
	protected DataOutputStream lowlatencyDataOut;
	
	/** The bulk data in. */
	protected DataInputStream bulkDataIn;
	
	/** The lowlatency data in. */
	protected DataInputStream lowlatencyDataIn;
	
	/** The udp on. */
	protected boolean udpOn = false;
	
	/** The fast link ackd. */
	protected AtomicBoolean fastLinkAckd = new AtomicBoolean(false);
	
	/** The endpoint. */
	protected final InetAddress endpoint;
	
	/** The port. */
	protected int port;
	
	/** The last bulk out. */
	protected AtomicLong lastBulkOut = new AtomicLong();
	
	/** The last bulk in. */
	protected AtomicLong lastBulkIn = new AtomicLong();
	
	/** The last fast out. */
	protected AtomicLong lastFastOut = new AtomicLong();
	
	/** The last fast in. */
	protected AtomicLong lastFastIn = new AtomicLong();
	
	/** The last udp out. */
	protected AtomicLong lastUdpOut = new AtomicLong();
	
	/** The last udp in. */
	protected AtomicLong lastUdpIn = new AtomicLong();
	/*
	 * Should be no need for another lowlatency queue unless we find poor
	 * performance
	 */
	/** The packets. */
	public ArrayBlockingQueue<MossNetPacket> packets = new ArrayBlockingQueue<>(
			1024);
	
	/** The bulk read handler. */
	protected Thread bulkReadHandler = new Thread(new Runnable() {

		@Override
		public void run() {

			try {
				recvLoop: while (ClientNetworkingManager.this.runThreads.get()) {

					if (ClientNetworkingManager.this.bulkDataIn.readInt() != CommonNetworking.magic) {
						// Handle reconnect
					}

					int length = ClientNetworkingManager.this.bulkDataIn
							.readShort();
					byte[] buf = new byte[length];

					int commandId = ClientNetworkingManager.this.bulkDataIn
							.readUnsignedByte();
					if (commandId == 255) {
						ClientNetworkingManager.this.partyQuenched.set(true);
						continue recvLoop;
					}

					ClientNetworkingManager.this.bulkStreamIn.read(buf);
					if (commandId == 254) {
						ClientNetworkingManager.this.fastLinkAckd.set(true); 
						sendPacketLowLatency(254, buf);
						sendPacketUdp(254, buf, true);
						continue recvLoop;
					}
					ClientNetworkingManager.this.packets.add(new MossNetPacket(
							commandId, buf));
					ClientNetworkingManager.this.lastBulkIn.set(System
							.currentTimeMillis());
					if (ClientNetworkingManager.this.packets
							.remainingCapacity() < 32)
						sendQuench();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}, "ClientBulkRecv"); //$NON-NLS-1$
	
	/** The fast read handler. */
	protected Thread fastReadHandler = new Thread(new Runnable() {
		// TODO
		@Override
		public void run() {

			try {
				recvLoop: while (ClientNetworkingManager.this.runThreads.get()) {

					if (ClientNetworkingManager.this.lowlatencyDataIn.readInt() != CommonNetworking.magic) {
						// Handle reconnect
					}
					int length = ClientNetworkingManager.this.lowlatencyDataIn
							.readShort();
					byte[] buf = new byte[length];

					int commandId = ClientNetworkingManager.this.lowlatencyDataIn
							.readUnsignedByte();
					if (commandId == 255) {
						ClientNetworkingManager.this.partyQuenched.set(true);
						continue recvLoop;
					}
					ClientNetworkingManager.this.fastStreamIn.read(buf);
					ClientNetworkingManager.this.packets.add(new MossNetPacket(
							commandId, buf));
					ClientNetworkingManager.this.lastFastIn.set(System
							.currentTimeMillis());
					ClientNetworkingManager.this.packets.add(new MossNetPacket(
							commandId, buf));
					if (ClientNetworkingManager.this.packets
							.remainingCapacity() < 32)
						sendQuench();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}, "ClientBulkRecv"); //$NON-NLS-1$
	
	/** The party quenched. */
	protected AtomicBoolean partyQuenched = new AtomicBoolean(false);
	
	/** The quenched since. */
	AtomicLong quenchedSince = new AtomicLong(0);
	
	/** The dgram read handler. */
	protected Thread dgramReadHandler = new Thread(new Runnable() {
		// TODO--spanish for "all"
		@Override
		public void run() {

			recvLoop: while (ClientNetworkingManager.this.runThreads.get()) {
				byte[] buf = new byte[270];
				DatagramPacket pckt = new DatagramPacket(buf, 270);
				try {
					ClientNetworkingManager.this.udpSocket.receive(pckt);
					ByteArrayInputStream bufStr = new ByteArrayInputStream(
							pckt.getData());
					if (!pckt.getAddress().equals(
							ClientNetworkingManager.this.endpoint)) {
						logger.warn("A UDP packet was received with a mismatched origin IP."); //$NON-NLS-1$
						continue recvLoop;
					}
					DataInputStream dos = new DataInputStream(bufStr);
					int magic = dos.readInt();

					if (magic == CommonNetworking.magic)
						sendAck(dos.readUnsignedShort());
					if (!(magic == CommonNetworking.magic || magic == CommonNetworking.magicNoAck)) {
						logger.warn("A packet was received with an invalid magic number and has been dropped.");
						continue recvLoop;
					}
					int length = dos.readUnsignedByte();
					int commandId = dos.readUnsignedByte();
					if (commandId == 255) {
						ClientNetworkingManager.this.partyQuenched.set(true);
						continue recvLoop;
					}
					byte[] pBuf = new byte[length];
					bufStr.read(pBuf);
					ClientNetworkingManager.this.lastUdpIn.set(System
							.currentTimeMillis());
					ClientNetworkingManager.this.packets.add(new MossNetPacket(
							commandId, pBuf));

				} catch (IOException e) {
					ClientNetworkingManager.this.udpOn = false;
				}
			}

		}
	}, "ClientDgramRecv"); //$NON-NLS-1$

	/** The bulk stream in. */
	protected InputStream bulkStreamIn;
	
	/** The bulk stream out. */
	protected OutputStream bulkStreamOut;
	
	/** The fast stream in. */
	protected InputStream fastStreamIn;
	
	/** The fast stream out. */
	protected OutputStream fastStreamOut;

	/**
	 * Instantiates a new client networking manager.
	 *
	 * @param endpoint the endpoint
	 * @param port the port
	 * @param useUdp the use udp
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public ClientNetworkingManager(String endpoint, int port, boolean useUdp)
			throws IOException {
		this.endpoint = InetAddress.getByName(endpoint);
		this.lowLatencyStreamSocket.setPerformancePreferences(0, 1, 0);
		this.lowLatencyStreamSocket.setTrafficClass(0x10);
		this.lowLatencyStreamSocket.setTcpNoDelay(true);
		this.bulkDataSocket.connect(new InetSocketAddress(endpoint, 16511),
				16511);
		this.bulkStreamIn = this.bulkDataSocket.getInputStream();
		this.bulkStreamOut = this.bulkDataSocket.getOutputStream();
		this.bulkDataOut = new DataOutputStream(this.bulkStreamOut);
		this.bulkDataIn = new DataInputStream(this.bulkStreamIn);
		this.lowLatencyStreamSocket.connect(new InetSocketAddress(endpoint,
				16511), 16511);
		this.fastStreamIn = this.lowLatencyStreamSocket.getInputStream();
		this.fastStreamOut = this.lowLatencyStreamSocket.getOutputStream();
		this.lowlatencyDataOut = new DataOutputStream(this.fastStreamOut);
		this.lowlatencyDataIn = new DataInputStream(this.fastStreamIn);
		this.udpOn = false;
		if (useUdp) {
			try {
				this.udpSocket = new DatagramSocket(port,
						InetAddress.getByName(endpoint));
				this.udpSocket.setSoTimeout(0);
				sendTosUdpConn();
			} catch (SocketException e) {
				this.udpOn = false;
			}
		}
		this.lastBulkIn.set(System.currentTimeMillis());
		this.lastBulkOut.set(System.currentTimeMillis());
		this.lastFastIn.set(System.currentTimeMillis());
		this.lastFastOut.set(System.currentTimeMillis());
		this.lastUdpIn.set(System.currentTimeMillis());
		this.lastUdpOut.set(System.currentTimeMillis());
		this.bulkReadHandler.start();
		this.fastReadHandler.start();
		this.dgramReadHandler.start();
		this.sendQueueThread.start();
		this.netTimeoutThread.start();
	}

	/**
	 * Send ack.
	 *
	 * @param seqnum the seqnum
	 */
	protected void sendAck(int seqnum) {
		// TODO Auto-generated method stub

	}

	/**
	 * Send tos udp conn.
	 */
	protected void sendTosUdpConn() {
		// TODO Auto-generated method stub

	}

	/**
	 * Send quench.
	 */
	protected void sendQuench() {
		// TODO Sends a request for the server to back off with data and skip
		// non-essential data.

	}

	/**
	 * Send a packet, dispatching to the correct socket.
	 *
	 * @param p the p
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void sendPacket(MossNetPacket p) throws IOException {
		if(this.partyQuenched.get()&&!p.isImportant) return;
		
		if (p.needsFast) {
			if ((p.payload.length < 250) && this.udpOn)
				sendPacketUdp(p.commandId, p.payload, p.needsAck);
			else {
				sendPacketLowLatency(p.commandId, p.payload);
			}
		} else
			sendPacketDefault(p.commandId, p.payload);

	}

	/**
	 * Send packet default.
	 *
	 * @param commandId the command id
	 * @param payload the payload
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void sendPacketDefault(int commandId, byte[] payload)
			throws IOException {
		this.lastBulkOut.set(System.currentTimeMillis());
		synchronized (this.bulkDataOut) {
			try {
				this.bulkDataOut.writeInt(CommonNetworking.magic);
				this.bulkDataOut.writeShort(payload.length);
				this.bulkDataOut.write(commandId);
				this.bulkDataOut.write(payload);
				this.bulkDataOut.flush();
			} catch (IOException e) {
				defaultReinit();
				this.bulkDataOut.writeInt(CommonNetworking.magic);
				this.bulkDataOut.writeShort(payload.length);
				this.bulkDataOut.write(commandId);
				this.bulkDataOut.write(payload);
				this.bulkDataOut.flush();
			}
		}
	}

	/**
	 * Default reinit.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void defaultReinit() throws IOException {
		this.bulkDataIn.close();
		this.bulkDataOut.close();
		this.bulkDataSocket.close();
		this.bulkDataSocket = new Socket();
		this.bulkDataSocket.connect(new InetSocketAddress(this.endpoint,
				this.port), 10000);

		this.bulkDataOut = new DataOutputStream(
				this.bulkDataSocket.getOutputStream());
		this.bulkDataIn = new DataInputStream(
				this.bulkDataSocket.getInputStream());
		performBulkReconnect();
		this.lastBulkIn.set(System.currentTimeMillis());
		this.lastBulkOut.set(System.currentTimeMillis());

	}

	/**
	 * Perform bulk reconnect.
	 */
	protected void performBulkReconnect() {
		synchronized (this.bulkDataOut) {
			// PERFORM RECONNECTION
		}

	}

	/**
	 * Send packet low latency.
	 *
	 * @param commandId the command id
	 * @param payload the payload
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void sendPacketLowLatency(int commandId, byte[] payload)
			throws IOException {
		if (!this.fastLinkAckd.get()) {
			sendPacketDefault(commandId, payload);
		} else {
			this.lastFastOut.set(System.currentTimeMillis());
			try {
				this.lowlatencyDataOut.writeInt(CommonNetworking.magic);
				this.lowlatencyDataOut.writeShort(payload.length);
				this.lowlatencyDataOut.write(commandId);
				this.lowlatencyDataOut.flush();
				this.fastStreamOut.write(payload);
				this.fastStreamOut.flush();

			} catch (IOException e) {
				defaultReinit();
				this.lowlatencyDataOut.writeInt(CommonNetworking.magic);
				this.lowlatencyDataOut.writeShort(payload.length);
				this.lowlatencyDataOut.write(commandId);
				this.lowlatencyDataOut.flush();
				this.fastStreamOut.write(payload);
				this.fastStreamOut.flush();
			}
		}
	}

	/**
	 * Send packet udp.
	 *
	 * @param commandId the command id
	 * @param payload the payload
	 * @param needsAck the needs ack
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void sendPacketUdp(int commandId, byte[] payload, boolean needsAck)
			throws IOException {
		this.lastUdpOut.set(System.currentTimeMillis());
		DatagramPacket toSend = new DatagramPacket(payload, payload.length);
		toSend.setSocketAddress(new InetSocketAddress(this.endpoint, this.port));
		synchronized (this.udpSocket) {
			this.udpSocket.send(toSend);
		}

	}

	/**
	 * Enqueue packet.
	 *
	 * @param p the p
	 * @throws InterruptedException the interrupted exception
	 */
	public void enqueuePacket(MossNetPacket p) throws InterruptedException {
		this.sendQueue.put(p);
	}

	/**
	 * The Class StateMachine.
	 */
	protected class StateMachine {
		
		/** The Constant DISCONNECTED. */
		static final int DISCONNECTED = 0;
		
		/** The Constant LINK. */
		static final int LINK = 1;
		
		/** The Constant AUTH. */
		static final int AUTH = 2;
		
		/** The Constant RESOURCE_XFER. */
		static final int RESOURCE_XFER = 3;
		
		/** The Constant ESTABLISHED. */
		static final int ESTABLISHED = 4;
		
		/** The Constant DENIED. */
		static final int DENIED = 5;
		
		/** The Constant TIMEDOUT. */
		static final int TIMEDOUT = 6;
		
		/** The cur status. */
		int curStatus = 0;
	}

	/**
	 * Begin connect handshake.
	 */
	public void beginConnectHandshake() {
		// TODO Auto-generated method stub

	}

	/** The send queue. */
	final ArrayBlockingQueue<MossNetPacket> sendQueue = new ArrayBlockingQueue<>(
			1024);

	/** The send queue thread. */
	private Thread sendQueueThread = new Thread(new Runnable() {

		@Override
		public void run() {
			while (ClientNetworkingManager.this.runThreads.get()) {
				try {
					MossNetPacket p = ClientNetworkingManager.this.sendQueue.take();
					ClientNetworkingManager.this.sendPacket(p);
				} catch (InterruptedException | IOException e) {
					// superfluous exception
				}
			}
		}
	}, Messages.getString("ClientNetworkingManager.THREAD_QUEUEING")); //$NON-NLS-1$

	/** The net timeout thread. */
	private Thread netTimeoutThread = new Thread(new Runnable() {

		@Override
		public void run() {
			while (ClientNetworkingManager.this.runThreads.get()) {
				long cTime = System.currentTimeMillis();
				if (cTime - ClientNetworkingManager.this.lastBulkIn.get() > 10000
						|| cTime
								- ClientNetworkingManager.this.lastFastIn.get() > 10000
						|| cTime - ClientNetworkingManager.this.lastUdpIn.get() > 10000) {
					ClientManager
							.showErrorScreen(
									Messages.getString("ClientNetworkingManager.ERR_NETWORK_TIMEOUT"), //$NON-NLS-1$
									Messages.getString("ClientNetworkingManager.DESC_NETWORK_TIMEOUT"), //$NON-NLS-1$
									true);
					logger.error("The connection to the server has timed out or otherwise failed.");

				}
				try {
					if (cTime - ClientNetworkingManager.this.lastBulkOut.get() > 4000)
						sendPacketDefault(00, new byte[] {});
				} catch (IOException e) {
					// pass
				}
				try {
					if (cTime - ClientNetworkingManager.this.lastFastOut.get() > 4000)
						sendPacketLowLatency(00, new byte[] {});
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					if (cTime - ClientNetworkingManager.this.lastUdpOut.get() > 4000)
						sendPacketUdp(00, new byte[] {}, false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (cTime - ClientNetworkingManager.this.quenchedSince.get() > 4000)
					ClientNetworkingManager.this.partyQuenched.set(false);

				try {
					// oh, joy
					Thread.sleep(4000
							- cTime
							+ Math.min(
									ClientNetworkingManager.this.lastBulkIn
											.get(),
									Math.min(
											ClientNetworkingManager.this.lastBulkOut
													.get(),
											Math.min(
													ClientNetworkingManager.this.lastFastIn
															.get(),
													Math.min(
															ClientNetworkingManager.this.lastFastOut
																	.get(),
															Math.min(
																	ClientNetworkingManager.this.lastUdpIn
																			.get(),
																	Math.min(
																			ClientNetworkingManager.this.lastUdpOut
																					.get(),
																			ClientNetworkingManager.this.quenchedSince
																					.get())))))));
				} catch (InterruptedException e) {
					// pass
				}

			}

		}
	}, Messages.getString("ClientNetworkingManager.THREAD_NET_TIMEOUT")); //$NON-NLS-1$

}
