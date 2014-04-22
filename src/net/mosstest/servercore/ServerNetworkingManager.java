package net.mosstest.servercore;

import net.mosstest.netclient.MossNetPacket;

import java.io.*;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: Auto-generated Javadoc
/**
 * Server networking manager. Now uses standard IO. This is a nasty thread pool.
 * Misuse may result in chlorine poisoning, asphyxiation, drowning, death, or
 * injury. The proximity of large and fast streams and datagram ice floes is
 * also a danger.
 * 
 * @author rarkenin
 * 
 */
public class ServerNetworkingManager {
	
	/**
	 * The Class AcceptRunnable.
	 */
	public class AcceptRunnable implements Runnable {
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			acLoop: while (ServerNetworkingManager.this.runConnections.get()) {
				try {
					@SuppressWarnings("resource")
					// To be closed from other side or explcitly. We don't want
					// to implicitly close on clients.
					Socket sock = ServerNetworkingManager.this.sSock.accept();
					if (ServerNetworkingManager.this.currentConnections.get() >= ServerNetworkingManager.this.maxConnections) {
						writeTcpPacket(sock.getOutputStream(), 0x03,
								new byte[] { 0x03 });
						sock.close();
						continue acLoop;
					}
					ServerNetworkingManager.this.currentConnections
							.incrementAndGet();
					ServerNetworkingManager.this.connectionQueue.put(sock);
					if (ServerNetworkingManager.this.currentConnections.get() > ServerNetworkingManager.this.currentServicingThreads
							.get()) {
						new Thread(ServerNetworkingManager.this.svrNetGroup,
								new SocketRecvRunnable(), Messages.getString("ServerNetworkingManager.CONN_THREAD_NAME") //$NON-NLS-1$
										+ Math.random()).start();
					}

				} catch (IOException | InterruptedException e) {
					// pass
				}
			}
		}

	}

	/**
	 * Instantiates a new server networking manager.
	 *
	 * @param port the port
	 * @param world the world
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public ServerNetworkingManager(int port, MossWorld world)
			throws IOException {
		this.bindingIdentifiers = new HashMap<>();
		this.acceptThread.start();
		this.maxConnections = EngineSettings.getInt("net.maxConnections", 255) * 2 + 1; //$NON-NLS-1$
		this.runConnections = new AtomicBoolean(true);
		this.currentConnections = new AtomicInteger(0);
		this.currentServicingThreads = new AtomicInteger(0);
		this.sSock = new ServerSocket(port);
		this.connectionQueue = new ArrayBlockingQueue<>(4);
		this.acceptThread = new Thread(new AcceptRunnable(), Messages.getString("ServerNetworkingManager.ACCEPT_THREAD")); //$NON-NLS-1$
		this.acceptThread.start();
	}

	/** The binding identifiers. */
	private HashMap<Long, ServerSession> bindingIdentifiers;
	
	/** The max connections. */
	private final int maxConnections;
	
	/** The svr net group. */
	private ThreadGroup svrNetGroup = new ThreadGroup(Messages.getString("ServerNetworkingManager.THREADGROUP")); //$NON-NLS-1$
	
	/** The run connections. */
	protected AtomicBoolean runConnections;
	
	/** The current connections. */
	protected AtomicInteger currentConnections;
	
	/** The current servicing threads. */
	protected AtomicInteger currentServicingThreads;
	
	/** The s sock. */
	protected ServerSocket sSock;
	
	/** The connection queue. */
	protected ArrayBlockingQueue<Socket> connectionQueue;
	
	/** The send thread form queue. */
	protected ArrayBlockingQueue<ServerSession> sendThreadFormQueue;
	
	/** The accept thread. */
	private Thread acceptThread;

	/**
	 * Write tcp packet.
	 *
	 * @param os the os
	 * @param commandId the command id
	 * @param payload the payload
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected static void writeTcpPacket(OutputStream os, int commandId,
			byte[] payload) throws IOException {

	}

	/**
	 * The Class SocketSendRunnable.
	 */
	protected class SocketSendRunnable implements Runnable {
		
		/** The sess. */
		ServerSession sess;

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			this.sess = ServerNetworkingManager.this.sendThreadFormQueue.poll();
			if (this.sess == null) {
				return;
			}
			pLoop: while (this.sess.isValid.get()) {
				try {
					MossNetPacket p = this.sess.packets.take();
					if (this.sess.quenchedSince < System.currentTimeMillis() - 4000
							&& !p.isImportant)
						continue pLoop;

					if (p.needsFast) {
						if ((p.payload.length < 250)
								&& this.sess.dgramSocket != null)
							sendPacketUdp(p.commandId, p.payload, p.needsAck);
						else {
							sendPacketLowLatency(p.commandId, p.payload);
						}
					} else
						sendPacketDefault(p.commandId, p.payload);

				} catch (InterruptedException | IOException e) {
					// PASS
				}

			}

		}

		/**
		 * Send packet default.
		 *
		 * @param commandId the command id
		 * @param payload the payload
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		private void sendPacketDefault(int commandId, byte[] payload)
				throws IOException {
			sendImpl(commandId, payload, this.sess.bulkSocket);

		}

		/**
		 * Send packet low latency.
		 *
		 * @param commandId the command id
		 * @param payload the payload
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		@SuppressWarnings("resource")
		private void sendPacketLowLatency(int commandId, byte[] payload)
				throws IOException {
			Socket targetSocket = (this.sess.fastSocket == null) ? this.sess.bulkSocket
					: this.sess.fastSocket;
			sendImpl(commandId, payload, targetSocket);
		}

		/**
		 * Send impl.
		 *
		 * @param commandId the command id
		 * @param payload the payload
		 * @param targetSocket the target socket
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		private void sendImpl(int commandId, byte[] payload, Socket targetSocket)
				throws IOException {

			OutputStream os = targetSocket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeInt(CommonNetworking.magic);
			dos.writeInt(payload.length);
			dos.writeByte(commandId);
			dos.write(payload);
			dos.flush();
			os.flush();
			dos.close();
			os.close();

		}

		/**
		 * Send packet udp.
		 *
		 * @param commandId the command id
		 * @param payload the payload
		 * @param needsAck the needs ack
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		private void sendPacketUdp(int commandId, byte[] payload,
				boolean needsAck) throws IOException {
			if (this.sess.dgramSocket == null)
                throw new SocketException("Null datagram socket!"); //$NON-NLS-1$
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
			dos.writeInt(CommonNetworking.magicNoAck);
			dos.writeByte(commandId);
			dos.writeByte(payload.length);
			dos.flush();
			bos.write(payload);
			DatagramPacket pc = new DatagramPacket(bos.toByteArray(),
					bos.toByteArray().length);
			this.sess.dgramSocket.send(pc);

		}

	}

	/**
	 * The Class SocketRecvRunnable.
	 */
	private class SocketRecvRunnable implements Runnable {

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@SuppressWarnings("resource")
		@Override
		public void run() {
			ServerNetworkingManager.this.currentServicingThreads
					.incrementAndGet();
			try {
				while (ServerNetworkingManager.this.runConnections.get()) {
					try {
						Socket sock = ServerNetworkingManager.this.connectionQueue
								.take();
						InputStream in = sock.getInputStream();
						DataInputStream dataIn = new DataInputStream(in);
						long bindId = new Random().nextLong();
						ServerSession sess = new ServerSession();
						ServerNetworkingManager.this.sendThreadFormQueue
								.put(sess);
						sess.bulkSocket = sock;
						ByteArrayOutputStream ba = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(ba);
						dos.writeLong(bindId);
						dos.flush();
						ba.flush();
						sess.packets.put(new MossNetPacket(0xFE, ba
								.toByteArray(), false, true, true));
						SocketSendRunnable ssr = new SocketSendRunnable();
						Thread sendThread = new Thread(ssr);
						sendThread.start();
						while (ServerNetworkingManager.this.runConnections
								.get() && !sock.isClosed()) {
							if (dataIn.readInt() != CommonNetworking.magic) {
								// Handle reconnect
							}
							int length = dataIn.readShort();
							byte[] buf = new byte[length];
							int commandId = dataIn.readUnsignedByte();
							if (commandId == 255) {
								sess.quenchedSince = System.currentTimeMillis();
							}
							if (commandId == 254) {
								sess.isValid.set(false);
								sess = ServerNetworkingManager.this.bindingIdentifiers.get(Long
										.valueOf(dataIn.readLong()));
								sess.fastSocket = sock;
								sock.setTcpNoDelay(true);

							}

						}

					} catch (InterruptedException e) {
						// pass
					}

				}
			} catch (Exception e) {// pass
			} finally {
				ServerNetworkingManager.this.currentServicingThreads
						.decrementAndGet();

			}
		}
	}

}
