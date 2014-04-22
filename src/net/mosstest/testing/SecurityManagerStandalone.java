package net.mosstest.testing;

import net.mosstest.servercore.MosstestSecurityManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.List;

/**
 * Created by hexafraction on 4/22/14.
 */
public class SecurityManagerStandalone {
    private static final Logger logger = Logger.getLogger(SecurityManagerStandalone.class);

    public static void main(String[] args) throws IOException {
        testWhitelist();


    }

    private static void testWhitelist() throws IOException {
        logger.info("Now warming up for whitelist testing...");
        MosstestSecurityManager mgr = getWarmedUp();
        BufferedReader whitelistReader = new BufferedReader(new FileReader(new File("data/tests/security/whitelist")));
        String line = null;
        while ((line = whitelistReader.readLine()) != null) {
            try {
                mgr.checkRead(line);
            } catch (SecurityException e){
                logger.error("Security manager denied access to whitelisted file "+line);
            }
        }
    }

    private static MosstestSecurityManager getWarmedUp() throws IOException {
        MosstestSecurityManager mgr = new MosstestSecurityManager();
        mgr.setThreadContext(MosstestSecurityManager.ThreadContext.CONTEXT_SCRIPT);
        BufferedReader warmupReader = new BufferedReader(new FileReader(new File("data/tests/security/warmup")));
        String line = null;
        while ((line = warmupReader.readLine()) != null) {
            mgr.checkRead(line);
        }
        return mgr;
    }
}
