package net.mosstest.servercore;

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
     * The Enum State.
     */
    public static enum State {


        CONN_NEW,

        CONN_AUTH_SENT,

        CONN_AUTH_RECV,

        CONN_GAME_HANDSHAKE,

        CONN_PLAYING,

        CONN_BOT,

        CONN_TIMEOUT
    }
}
