package net.mosstest.scripting.authentication;

/**
 * Created by hexafraction on 9/24/14.
 */
public interface MossAuthenticator {
    public AuthChallenge getChallenge(String username);

    /**
     * Return if the challenge and response match up, and the user is allowed to log on. Otherwise, throw an AccessDenied.
     */
    public void checkLogon(AuthChallenge challenge, byte[] response) throws AccessDenied;
}
