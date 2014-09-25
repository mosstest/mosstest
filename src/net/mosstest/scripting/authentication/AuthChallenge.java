package net.mosstest.scripting.authentication;

/**
 * Created by hexafraction on 9/24/14.
 */
public class AuthChallenge {
    public static final byte[] EMPTY = {};
    public final AuthType type;
    public final byte[] challenge;
    public final String username;
    public AuthChallenge(AuthType type, String username, byte[] challenge) {
        this.type = type;
        this.username = username;
        this.challenge = challenge;
    }
}
