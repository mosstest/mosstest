package net.mosstest.scripting.authentication;

/**
* Created by hexafraction on 9/24/14.
*/
public enum DenialReason {
    REASON_UNKNWN,
    REASON_BAD_PASS,
    REASON_BANNED,
    REASON_PLAYER_LIMIT,
    REASON_LOGON_HOUR,
    REASON_NO_NEW_PLAYERS,
    REASON_VERSION_MISMATCH,
    REASON_AUTH_TIMED_OUT,
    REASON_SERVER_MAINT,
    REASON_FAILED_CONNECTION
}
