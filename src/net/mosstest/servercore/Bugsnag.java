package net.mosstest.servercore;

import com.bugsnag.Client;

/**
 * Created by hexafraction on 11/3/14.
 */
public class Bugsnag {
    static Client bugsnag;

    public static synchronized void init() {
        if (bugsnag == null) bugsnag = new Client("cbb9dd103f657c604ee520f8cf6e863d");
    }

    public static synchronized Client getBugsnag(){
        return bugsnag;
    }
}
