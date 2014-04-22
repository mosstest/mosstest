package net.mosstest.servercore;

import net.mosstest.netclient.MossNetPacket;
import net.mosstest.scripting.Player;

import java.net.DatagramSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Auto-generated Javadoc

/**
 * The ServerSession class unifies a session on a server. Each incoming connection has a different session.
 * When a connection is determined to be associated with an existing session,
 *
 * @author rarkenin
 */
public class ServerSession {

    /**
     * The player.
     */
    public Player player;

    /**
     * The auth challenge.
     */
    public String authChallenge;

    /**
     * The packets.
     */
    public ArrayBlockingQueue<MossNetPacket> packets;

    /**
     * The bulk socket.
     */
    public volatile Socket bulkSocket;

    /**
     * The fast socket.
     */
    public volatile Socket fastSocket;

    /**
     * The dgram socket.
     */
    public volatile DatagramSocket dgramSocket;

    /**
     * The is valid.
     */
    public AtomicBoolean isValid = new AtomicBoolean(true);

    /**
     * The state.
     */
    public ServerSession.State state;

    /**
     * The quenched since.
     */
    public volatile long quenchedSince = 0;

    /**
     * The Enum State.
     */
    public static enum State {

        /**
         * The conn new.
         */
        CONN_NEW,
        /**
         * The conn auth sent.
         */
        CONN_AUTH_SENT,
        /**
         * The conn auth recv.
         */
        CONN_AUTH_RECV,
        /**
         * The conn game handshake.
         */
        CONN_GAME_HANDSHAKE,
        /**
         * The conn playing.
         */
        CONN_PLAYING,
        /**
         * The conn bot.
         */
        CONN_BOT,
        /**
         * The conn timeout.
         */
        CONN_TIMEOUT
    }
}
