package net.mosstest.client;

import net.mosstest.netcommand.ToServerHello;
import net.mosstest.scripting.MossScriptEnv;
import net.mosstest.scripting.NodePosition;
import net.mosstest.scripting.Player;
import net.mosstest.servercore.MossNetPacket;
import net.mosstest.servercore.MosstestSecurityManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by hexafraction on 4/26/14.
 */
public class MossClient {
    public static final short PROTOCOL_VERSION = 1;
    private String username;
    RemoteFileManager fileManager;
    ClientNetworkingManager net;
    private byte[] password;

    public ClientState getState() {
        return state;
    }

    private volatile ClientState state;

    private Player ownPlayer;

    private ClientDispatcher dispatcher;

    public MossClient(String remoteHost, int port, String username, byte[] pass) throws IOException {
        this.username = username;
        this.password = pass;
        this.state = ClientState.PREPARING;
        this.fileManager = RemoteFileManager.getInstance();
        this.state = ClientState.CONNECTING;
        this.fileManager.setClient(this);
        this.net = new ClientNetworkingManager(remoteHost, port, false); // so far no UDP
        // we need to send the hello to kick things off
        // This is not happening until we reimplement network handlers with netty on client side.
        //this.net.sendPacket(new MossNetPacket(0x01, new ToServerHello(username, PROTOCOL_VERSION, MossScriptEnv.MIN_SCRIPT_API_VERSION, MossScriptEnv.MAX_SCRIPT_API_VERSION).toByteArray()));
    }


    public byte[] getPassword() {

        SecurityManager sm = System.getSecurityManager();
        if (sm instanceof MosstestSecurityManager) {
            ((MosstestSecurityManager) sm).checkMosstestControl();
        }

        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public enum ClientState {
        PREPARING,
        CONNECTING,
        BUILDING_FILE_LIST,
        FETCHING_FILES,
        EXECUTING_STARTUP_SCRIPTS,
        RENDERER_STARTING,
        PLAYING,
        CRASHED,
        TIMED_OUT
    }
}
