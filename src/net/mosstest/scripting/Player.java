package net.mosstest.scripting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import net.mosstest.renderer.RenderProcessor;
import net.mosstest.servercore.PlayerCommunicator;

import org.jetbrains.annotations.NonNls;

// TODO: Auto-generated Javadoc

/**
 * The Class Player.
 */
public class Player {

    /**
     * The inventories.
     */
    private HashMap<String, MossInventory> inventories = new HashMap<>();

    public int playerId;

    /**
     * The name.
     */
    public final String name;

    /**
     * The xoffset.
     */
    public volatile double xoffset;

    /**
     * The yoffset.
     */
    public volatile double yoffset;

    /**
     * The zoffset.
     */
    public volatile double zoffset;

    /**
     * The xchk.
     */
    public volatile int xchk;

    /**
     * The ychk.
     */
    public volatile int ychk;

    /**
     * The zchk.
     */
    public volatile int zchk;

    /**
     * The x velocity.
     */
    public volatile double xVelocity;

    /**
     * The y velocity.
     */
    public volatile double yVelocity;

    /**
     * The z velocity.
     */
    public volatile double zVelocity;

    /**
     * The oldx.
     */
    public volatile double oldx;

    /**
     * The oldy.
     */
    public volatile double oldy;

    /**
     * The oldz.
     */
    public volatile double oldz;

    /**
     * The oldxchk.
     */
    public volatile int oldxchk;

    /**
     * The oldychk.
     */
    public volatile int oldychk;

    /**
     * The oldzchk.
     */
    public volatile int oldzchk;

    /**
     * The last anticheat millis.
     */
    public volatile long lastAnticheatMillis;

    /**
     * The comm.
     */
    private PlayerCommunicator comm;

    /**
     * The privs.
     */
    private HashSet<String> privs;

    /**
     * Object to be synchronized on for.
     */
    public final Object antiCheatDataLock = new Object();

    /**
     * Instantiates a new player.
     *
     * @param name the name
     */
    public Player(String name) {
        this.name = name;
        this.inventories.put("default", new MossInventory(96, 8, 6));
    } //$NON-NLS-1$

    /**
     * Creates the inventory.
     *
     * @param name     the name
     * @param rows     the rows
     * @param cols     the cols
     * @param maxStack the max stack
     * @return the moss inventory
     */
    public MossInventory getInventory(@NonNls String name, int rows, int cols,
                                      int maxStack) {
        // todo inventory caching
        MossInventory inv = inventories.get(name);
        if (inv == null) {
            inv = new MossInventory(maxStack, rows, cols);
            this.inventories.put(name, inv);
        }
        return inv;
    }

    /**
     * Respawn.
     */
    public void respawn() {
        synchronized (this.antiCheatDataLock) {

        }

    }

    /**
     * Grant privilege.
     *
     * @param privs the privs
     */
    public void grantPrivilege(String... privs) {
        for (String priv : privs)
            if (priv.matches("^[a-zA-Z0-9]*$") && priv.length() > 0)
                this.privs.add(priv);

    }

    /**
     * Revoke privilege.
     *
     * @param privs the privs
     */
    public void revokePrivilege(String... privs) {
        for (String priv : privs)
            if (priv.matches("^[a-zA-Z0-9]*$") && priv.length() > 0)
                this.privs.remove(priv);

    }

    /**
     * Sets the chunk position.
     *
     * @param x the x
     * @param y the y
     * @param z the z
     */
    public synchronized void setChunkPosition(int x, int y, int z) {
        this.xchk = x;
        this.ychk = y;
        this.zchk = z;
    }

    /**
     * Sets the position offsets.
     *
     * @param x the x
     * @param y the y
     * @param z the z
     */
    public synchronized void setPositionOffsets(double x, double y, double z) {
        this.xoffset = x;
        this.yoffset = y;
        this.zoffset = z;
    }

    /**
     * Privs to string.
     *
     * @return the string
     */
    public String privsToString() {
        return StringUtils.join(this.privs, ':');
    }

    /**
     * Sets the privs from string.
     *
     * @param serPrivs the new privs from string
     */
    public void setPrivsFromString(String serPrivs) {
        this.privs.clear();
        for (String priv : serPrivs.split(":"))
            if (priv.matches("^[a-zA-Z0-9]*$") && priv.length() > 0)
                this.privs.add(priv);

    }

    /**
     * Force set position.
     *
     * @param offsetx the offsetx
     * @param offsety the offsety
     * @param offsetz the offsetz
     * @param cx      the cx
     * @param cy      the cy
     * @param cz      the cz
     * @throws InterruptedException the interrupted exception
     */
    public void forceSetPosition(double offsetx, double offsety,
                                 double offsetz, int cx, int cy, int cz) throws InterruptedException {
        this.comm.forceSetPosition(this, cx, cy, cz, offsetx, offsety, offsetz);
    }

    /**
     * To byte array.
     *
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(this.xchk);
        dos.writeInt(this.ychk);
        dos.writeInt(this.zchk);
        dos.writeDouble(this.xoffset);
        dos.writeDouble(this.yoffset);
        dos.writeDouble(this.zoffset);
        dos.writeUTF(this.name);
        dos.writeUTF(this.privsToString());
        dos.flush();
        bos.flush();
        return bos.toByteArray();
    }

    /**
     * Instantiates a new player.
     *
     * @param comm_ the comm_
     * @param buf   the buf
     * @throws IOException          Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    public Player(PlayerCommunicator comm_, byte[] buf) throws IOException,
            InterruptedException {
        this.comm = comm_;
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bis);
        this.xchk = dis.readInt();
        this.ychk = dis.readInt();
        this.zchk = dis.readInt();
        this.xoffset = dis.readDouble();
        this.yoffset = dis.readDouble();
        this.zoffset = dis.readDouble();
        this.name = dis.readUTF();
        this.setPrivsFromString(dis.readUTF());
        this.comm.forceSetPosition(this, this.xchk, this.ychk, this.zchk,
                this.xoffset, this.yoffset, this.zoffset);
    }

}
