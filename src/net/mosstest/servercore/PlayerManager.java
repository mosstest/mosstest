package net.mosstest.servercore;

import net.mosstest.scripting.Player;
import net.mosstest.scripting.events.IMossEvent;

import java.io.IOException;

import static org.fusesource.leveldbjni.JniDBFactory.*;
/**
 * Created by hexafraction on 10/18/14.
 */
public class PlayerManager {
    private final MapDatabase db;
    private final MossWorld world;
    public PlayerManager(MapDatabase db, MossWorld world) {
        this.db = db;
        this.world = world;
    }

    public Player getPlayer(String name){
        byte[] serializedPlayer = db.players.get(bytes(name));
        if(serializedPlayer!=null){
            try {
                return new Player(null, serializedPlayer);
            } catch (Exception e) {
                return getNewPlayer(name);
            }
        }
        return getNewPlayer(name);
    }

    private Player getNewPlayer(String name) {
        Player p = new Player(name);
        // world.enqueueEvent(null);
        // FIXME on player join event
        return p;
    }
}
