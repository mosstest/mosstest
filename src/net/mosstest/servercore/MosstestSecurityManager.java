package net.mosstest.servercore;

import net.mosstest.scripting.MossScriptEnv;
import net.mosstest.scripting.ScriptDebugUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FilePermission;
import java.io.IOException;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.Permission;
import java.security.SecureClassLoader;
import java.util.Arrays;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * The Class MosstestSecurityManager.
 */
public class MosstestSecurityManager extends SecurityManager {


    public static final int PORT_HTTP = 80;
    public static final int PORT_HTTPS = 443;
    public static final int MIN_SCRIPT_PORT = 16600;
    public static final int MAX_SCRIPT_PORT = 16512;
    /**
     * The logger.
     */
    static Logger logger = Logger.getLogger(MosstestSecurityManager.class);
    private final boolean testMode;

    public MosstestSecurityManager(boolean testMode) {
        this.testMode = testMode;
        if (testMode) logger.warn("WARNING! The security manager is running in test mode. Security may be diminished.");
        File classDir = null;
        try {
            classDir = new File(MossScriptEnv.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath()) // net.mosstest.scripting.MossScriptEnv
                    .getParentFile() // net.mosstest.scripting
                    .getCanonicalFile();
        } catch (IOException e) {
            logger.warn("Failed to obtain a class directory for the security manager, spurious classloading failures may result.");
        } finally {
            this.classDirectory = classDir;
        }
    }


    @Override
    public void checkPermission(Permission perm) {

        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {
            logger.fatal("Requested permssion " + perm);
            if (testMode && perm.getName().equals("setIO")) return;
            logger.warn("MosstestSecurityManager prevented the use of arbitrary permissions outside engine contexts.");

            throw new SecurityException(
                    "MosstestSecurityManager prevented the use of arbitrary permissions outside engine contexts.");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkPermission(java.security.Permission,
     * java.lang.Object)
     */
    @Override
    public void checkPermission(Permission perm, Object context) {
        System.err.println(perm.toString() + ":" + context.toString());
        checkPermission(perm);
    }

    /**
     * The Constant instance.
     */
    public static final MosstestSecurityManager instance = new MosstestSecurityManager();

    /**
     * Gets the connected peer.
     *
     * @return the connected peer
     */
    public String getConnectedPeer() {
        return this.connectedPeer;
    }

    /**
     * The lock.
     */
    private InheritableThreadLocal<Object> lock = new InheritableThreadLocal<>();
    private File baseDirectory;
    // Class dir corresponding to "net.mosstest"
    private final File classDirectory;

    public void setTrustedBasedir(File basedir) throws SecurityException,
            IOException {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {
            logger.error("The security manager prevented an attempt to set the trusted base directory.");
            throw new SecurityException("Cannot set base directory.");
        }
        this.baseDirectory = basedir.getCanonicalFile();
    }

    /**
     * Lock.
     *
     * @param key the key
     * @param tc  the tc
     */
    public void lock(Object key, ThreadContext tc) {
        if ((this.lock.get() != null)
                || this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {
            logger.error("The security manager prevented an attempt to lock it on an already-locked thread.");
            throw new SecurityException(
                    "The security manager is already locked for this thread.");
        }
        this.lock.set(key);
        this.threadContext.set(tc);

    }

    private Object forceUnlock() {
        Object key = this.lock.get();
        this.lock.set(null);
        this.threadContext.set(ThreadContext.CONTEXT_ENGINE);
        return key;
    }

    /**
     * Unlock.
     *
     * @param key the key
     */
    public void unlock(Object key) {
        if (this.lock.get() != key) {
            logger.error("The security manager prevented an attempt to unlock it using a mismatched key.");
            throw new SecurityException(
                    "A mismatched key has been used to unlock this thread.");
        }
        this.threadContext.set(ThreadContext.CONTEXT_ENGINE);
    }

    /**
     * Sets the connected peer.
     *
     * @param connectedPeer the new connected peer
     */
    public void setConnectedPeer(String connectedPeer) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {
            logger.warn("MosstestSecurityManager prevented a non-engine-context thread from changing the connected peer.");
            throw new SecurityException(
                    "Attempted to set the connected peer from outside engine code");
        }

        this.connectedPeer = connectedPeer;
    }

    /**
     * Gets the thread context.
     *
     * @return the thread context
     */
    public ThreadContext getThreadContext() {
        return this.threadContext.get();
    }

    /**
     * Sets the thread context.
     *
     * @param tc the new thread context
     */
    public void setThreadContext(ThreadContext tc) {
        if (this.threadContext.get() == null) {
            logger.warn("A thread has started without inheriting a thread context and has been elevated");
            this.threadContext.set(tc);

            return;
        }
        ThreadContext old = this.threadContext.get();
        if (tc == ThreadContext.CONTEXT_LOCKDOWN) {
            this.threadContext.set(tc);
        }
        if (old != ThreadContext.CONTEXT_ENGINE) {
            logger.warn("Attempted to set the thread context type from non-engine code");
            throw new SecurityException(
                    "Attempted to set the thread context type from non-engine code");
        }
    }

    /**
     * The Enum ThreadContext.
     */
    public enum ThreadContext {

        /**
         * The context engine.
         */
        CONTEXT_ENGINE,
        /**
         * The context script.
         */
        CONTEXT_SCRIPT,
        /**
         * The context client.
         */
        CONTEXT_CLIENT,
        /**
         * The context lockdown.
         */
        CONTEXT_LOCKDOWN
    }

    /**
     * The connected peer.
     */
    private String connectedPeer = null;

    /**
     * The thread context.
     */
    private InheritableThreadLocal<ThreadContext> threadContext = new InheritableThreadLocal<MosstestSecurityManager.ThreadContext>() {
        @Override
        protected ThreadContext initialValue() {
            logger.warn("A thread has started without inheriting a thread context and has been elevated");
            return ThreadContext.CONTEXT_ENGINE;
        }
    };

    {
        this.threadContext.set(ThreadContext.CONTEXT_ENGINE);

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkCreateClassLoader()
     */
    @Override
    public void checkCreateClassLoader() {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager stopped an attempt to create a classloader");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to create a classloader");

        } else {
            super.checkCreateClassLoader();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkAccess(java.lang.Thread)
     */
    @Override
    public void checkAccess(Thread t) {
        if(testMode) return;
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager stopped an attempt to stop or modify a thread");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to stop or modify a thread");

        } else {
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkAccess(java.lang.ThreadGroup)
     */
    @Override
    public void checkAccess(ThreadGroup g) {
        if(testMode) return;
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager stopped an attempt to modify a ThreadGroup");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to modify a ThreadGroup");

        } else {
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkExit(int)
     */
    @Override
    public void checkExit(int status) {
        if (testMode) return;
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager stopped an attempt to exit Mosstest improperly from a script");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to exit Mosstest improperly from a script");

        }
        super.checkExit(status);

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkExec(java.lang.String)
     */
    @Override
    public void checkExec(String cmd) {

        logger.warn("MosstestSecurityManager does not allow any script or portion of the engine to start a new process");
        throw new SecurityException(
                "MosstestSecurityManager does not allow any script or portion of the engine to start a new process");
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkLink(java.lang.String)
     */
    @Override
    public void checkLink(String lib) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager stopped an attempt to link to a JN library from insecure code");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to link to a JN library from insecure code");

        }

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkRead(java.io.FileDescriptor)
     */
    @Override
    public void checkRead(FileDescriptor fd) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("1MosstestSecurityManager stopped an attempt to read a file from non-core code "
                    + fd.toString());
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to read a file from non-core code");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkRead(java.lang.String)
     */
    @Override
    public void checkRead(String file) {
        if(file.contains("testtest")){
            System.currentTimeMillis();
        }
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {
//            List<File> safeDirectories = Arrays.asList(this.baseDirectory, this.classDirectory);
            ThreadContext oldTc = MosstestSecurityManager.this
                    .getThreadContext();
            Object oldLock = MosstestSecurityManager.this.forceUnlock();
           // File tested;
//            try {
//
//
//                tested = new File(file).getCanonicalFile();
//                //MosstestSecurityManager.this.lock(oldLock, oldTc);
//            } catch (IOException e1) {
//                throw new SecurityException("The base directory failed to resolve!", e1);
//            }
//
//            do {
//                if (safeDirectories.contains(file)) {
//                    MosstestSecurityManager.this.lock(oldLock, oldTc);
//                    return;
//                }
//                tested = tested.getParentFile();
//          } while (tested != null);
            try {
                AccessController.checkPermission(new FilePermission(file, "read"));
            } catch (SecurityException e) {
                // lock
                this.lock(oldLock, oldTc);
                // rethrow to bail to a failure
                throw e;
            }

            super.checkRead(file);

            this.lock(oldLock, oldTc);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkRead(java.lang.String,
     * java.lang.Object)
     */
    @Override
    public void checkRead(String file, Object context) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("3MosstestSecurityManager stopped an attempt to read a file from non-core code: "
                    + file);
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to read a file from non-core code");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkWrite(java.io.FileDescriptor)
     */
    @Override
    public void checkWrite(FileDescriptor fd) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager stopped an attempt to write to a file from non-core code");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to write to a file from non-core code");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    @Override
    public void checkWrite(String file) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager stopped an attempt to write to a file from non-core code");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to write to a file from non-core code");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkDelete(java.lang.String)
     */
    @Override
    public void checkDelete(String file) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager stopped an attempt to delete a file from non-core code");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to delete a file from non-core code");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkConnect(java.lang.String, int)
     */
    @Override
    public void checkConnect(String host, int port) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {
            if ((port != PORT_HTTP && port != PORT_HTTPS) && (port > MIN_SCRIPT_PORT || port < MAX_SCRIPT_PORT))

                logger.warn("MosstestSecurityManager stopped an attempt to connect to a network port other than 80, 443, or any port in the range 16512-16600");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to connect to a network port other than 80, 443, or any port in the range 16512-16600");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkConnect(java.lang.String, int,
     * java.lang.Object)
     */
    @Override
    public void checkConnect(String host, int port, Object context) {
        if (this.threadContext.get() == ThreadContext.CONTEXT_CLIENT) {
            if ((port != PORT_HTTP && port != PORT_HTTPS) && (port > MIN_SCRIPT_PORT || port < MAX_SCRIPT_PORT)) {

                logger.warn("MosstestSecurityManager stopped an attempt to connect to a network port other than 80, 443, or any port in the range 16512-16600");
                throw new SecurityException(
                        "MosstestSecurityManager stopped an attempt to connect to a network port other than 80, 443, or any port in the range 16512-16600");
            }
            if (!host.equals(this.connectedPeer)) {

                throw new SecurityException(
                        "MosstestSecurityManager stopped an attempt for the client to connect to a peer other than the server currently played");
            }
        } else if (this.threadContext.get() == ThreadContext.CONTEXT_LOCKDOWN) {

            logger.warn("MosstestSecurityManager has denied a thread in lockdown to open a network connection");
            throw new SecurityException(
                    "MosstestSecurityManager has denied a thread in lockdown to open a network connection");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkListen(int)
     */
    @Override
    public void checkListen(int port) {
        if (this.threadContext.get() == ThreadContext.CONTEXT_SCRIPT) {
            if ((port > MIN_SCRIPT_PORT || port < MAX_SCRIPT_PORT))

                logger.warn("MosstestSecurityManager stopped an attempt to listen on a port not in the range 16512-16600");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to listen on a port not in the range 16512-16600");

        } else if (this.threadContext.get() == ThreadContext.CONTEXT_LOCKDOWN
                || this.threadContext.get() == ThreadContext.CONTEXT_CLIENT) {

            logger.warn("MosstestSecurityManager has denied a thread in lockdown or running a client script to listen on a port");
            throw new SecurityException(
                    "MosstestSecurityManager has denied a thread in lockdown or running a client script to listen on a port");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkAccept(java.lang.String, int)
     */
    @Override
    public void checkAccept(String host, int port) {
        if (this.threadContext.get() == ThreadContext.CONTEXT_SCRIPT) {
            if ((port > MIN_SCRIPT_PORT || port < MAX_SCRIPT_PORT))

                logger.warn("MosstestSecurityManager stopped an attempt to listen on a port not in the range 16512-16600");
            throw new SecurityException(
                    "MosstestSecurityManager stopped an attempt to listen on a port not in the range 16512-16600");

        } else if (this.threadContext.get() == ThreadContext.CONTEXT_LOCKDOWN
                || this.threadContext.get() == ThreadContext.CONTEXT_CLIENT) {

            logger.warn("MosstestSecurityManager has denied a thread in lockdown or running a client script to listen on a port");
            throw new SecurityException(
                    "MosstestSecurityManager has denied a thread in lockdown or running a client script to listen on a port");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkMulticast(java.net.InetAddress)
     */
    @Override
    public void checkMulticast(InetAddress maddr) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager has denied a script use of multicast directly without use of loadbalancing facilities");
            throw new SecurityException(
                    "Scripts may not use mutlicast under any circumstances, except through the engine loadbalancing facilities");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkMulticast(java.net.InetAddress, byte)
     */
    @Override
    public void checkMulticast(InetAddress maddr, byte ttl) {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager has denied a script use of multicast directly without use of loadbalancing facilities");
            throw new SecurityException(
                    "Scripts may not use mutlicast under any circumstances, except through the engine loadbalancing facilities");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkPropertiesAccess()
     */
    @Override
    public void checkPropertiesAccess() {
        if(testMode) return;
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager has denied a thread running outside the engine context the right to access system properties.");
            throw new SecurityException(
                    "Scripts may not access system properties");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     */
    @Override
    public void checkPropertyAccess(String key) {
        if(testMode) return;
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager has denied a thread running outside the engine context the right to access system properties.");
            throw new SecurityException(
                   "Scripts may not access system properties. Attempt was to access "+key);

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkTopLevelWindow(java.lang.Object)
     */
    @Override
    public boolean checkTopLevelWindow(Object window) {
        if (this.threadContext.get() == ThreadContext.CONTEXT_LOCKDOWN) {

            logger.warn("MosstestSecurityManager has blocked a thread running outside the engine context frop opening a top-level window.");
            throw new SecurityException(
                    "Threads in lockdown may not create top-level windows");
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkPrintJobAccess()
     */
    @Override
    public void checkPrintJobAccess() {

        logger.warn("MosstestSecurityManager has denied a thread running outside the engine context the right to access system print jobs.");
        throw new SecurityException(
                "Print job access is not allowed for the engine or scripts");
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkSystemClipboardAccess()
     */
    @Override
    public void checkSystemClipboardAccess() {
        if (this.threadContext.get() == ThreadContext.CONTEXT_LOCKDOWN) {

            logger.warn("MosstestSecurityManager has prevented a thread in lockdown from accessing the system clipboard.");
            throw new SecurityException(
                    "Threads in lockdown may not access the system clipboard");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkAwtEventQueueAccess()
     */
    @Override
    public void checkAwtEventQueueAccess() {
        if (this.threadContext.get() == ThreadContext.CONTEXT_LOCKDOWN) {

            logger.warn("MosstestSecurityManager has prevented a thread in lockdown from accessing the AWT queue.");
            throw new SecurityException(
                    "Threads in lockdown may not access the AWT queue");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkPackageAccess(java.lang.String)
     */
    @Override
    public void checkPackageAccess(String pkg) {
        super.checkPackageAccess(pkg);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkPackageDefinition(java.lang.String)
     */
    @Override
    public void checkPackageDefinition(String pkg) {
        super.checkPackageDefinition(pkg);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkSetFactory()
     */
    @Override
    public void checkSetFactory() {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager has prevented a thread in lockdown from setting a Java system factory.");
            throw new SecurityException(
                    "Non-engine threads may not set java system factories");
        }
        super.checkSetFactory();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkMemberAccess(java.lang.Class, int)
     */
    @Override
    public void checkMemberAccess(Class<?> clazz, int which) {
        super.checkMemberAccess(clazz, which);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#checkSecurityAccess(java.lang.String)
     */
    @Override
    public void checkSecurityAccess(String target) {
        super.checkSecurityAccess(target);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.SecurityManager#getThreadGroup()
     */
    @Override
    public ThreadGroup getThreadGroup() {
        return super.getThreadGroup();
    }

    /**
     * Check mosstest control.
     */
    public void checkMosstestControl() {
        if (this.threadContext.get() != ThreadContext.CONTEXT_ENGINE) {

            logger.warn("MosstestSecurityManager has prevented a plugin from controlling certain portions of the Mosstest engine.");
            throw new SecurityException(
                    "Non-engine threads may not control Mosstest execution");
        }
        super.checkSetFactory();
    }

    public MosstestSecurityManager() {
        this(false);
    }

}
