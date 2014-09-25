package net.mosstest.servercore;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import net.mosstest.scripting.Player;
import net.mosstest.scripting.authentication.AuthChallenge;

/**
 * Created by hexafraction on 9/23/14.
 */
@ThreadSafe
public class ServerConnectionState {
    @GuardedBy("this")
    private Player p;
    @GuardedBy("this")
    private StateMachine state;

    @GuardedBy("this")
    private AuthChallenge challenge;

    public synchronized AuthChallenge getChallenge() {
        return challenge;
    }

    public synchronized void setChallenge(AuthChallenge challenge) {
        this.challenge = challenge;
    }

    public synchronized Player getPlayer() {
        return p;
    }

    public synchronized void setPlayer(Player p) {
        this.p = p;
    }

    public synchronized StateMachine getState() {
        return state;
    }

    public synchronized void setState(StateMachine state) {
        this.state = state;
    }

    public ServerConnectionState() {
        this.p = null;
        this.state = StateMachine.NEW;
    }

    public enum StateMachine {
        NEW,
        AUTHENTICATED,
        TRANSFERRING_MEDIA,
        PLAYING,
        DEAD
    }
}
