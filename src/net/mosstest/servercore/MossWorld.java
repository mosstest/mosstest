package net.mosstest.servercore;

import net.mosstest.scripting.MapGenerators;
import net.mosstest.scripting.MossScriptEnv;
import net.mosstest.scripting.ScriptableDatabase;
import net.mosstest.scripting.events.IMossEvent;
import net.mosstest.servercore.MosstestSecurityManager.ThreadContext;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MossWorld {
	static {
		System.setSecurityManager(MosstestSecurityManager.instance);
	}

	static Logger logger = Logger.getLogger(MossWorld.class);
	private MossGame game;
	private File baseDir;
	private XMLConfiguration worldCfg;
	private File cfgFile;
	private MapDatabase db;
	private NodeCache nc;
	private MossScriptEnv mossEnv;
	private ScriptEnv sEnv;
	private ScriptableDatabase sdb;
	private EventProcessor evp;

	@SuppressWarnings("unused")
	private ServerNetworkingManager snv;

	volatile boolean run = true;
	private FuturesProcessor fp;
	private INodeManager nm;
	private IRenderPreparator rp;
	private RenderProcessor rend;

	/**
	 * Initializes a server world. This will start the server once the world is
	 * initialized, loaded, and passes basic consistency checks. This
	 * constructor will not initialize load-balancing.
	 * 
	 * @param name
	 *            A string that names the world.
	 * @param port
	 *            The port number on which to run the server. If negative a
	 *            singleplayer stack is created.
	 * @throws MossWorldLoadException
	 *             Thrown if the world cannot be loaded, due to inconsistency,
	 *             missing files, or lack of system resources.
	 * @throws MapDatabaseException
	 * @throws IOException
	 * @throws ConfigurationException
	 */
	@SuppressWarnings("nls")
	public MossWorld(String name, int port) throws MossWorldLoadException,
			MapDatabaseException, IOException, ConfigurationException {
		//Thread.currentThread().setContextClassLoader(
		//		MosstestSecurityManager.instance.getScriptClassLoader(Thread
		//				.currentThread().getContextClassLoader()));
		this.baseDir = new File("data/worlds/" + name); //$NON-NLS-1$
		if (!this.baseDir.exists()) {
			this.baseDir.mkdirs();

		}

		// Sets the security manager to trust attempts to open anything below
		// data/scripts
		MosstestSecurityManager.instance
				.setTrustedBasedir(new File("scripts/"));
		this.cfgFile = new File(this.baseDir, "world.xml"); //$NON-NLS-1$
		if (!this.cfgFile.isFile())

			this.cfgFile.createNewFile();
		this.worldCfg = new XMLConfiguration(this.cfgFile);

		if (!this.worldCfg.containsKey("gameid")) { //$NON-NLS-1$
			throw new MossWorldLoadException(
					Messages.getString("MossWorld.NO_GAME_ID")); //$NON-NLS-1$
		}
		this.game = new MossGame(this.worldCfg.getString("gameid")); //$NON-NLS-1$

		try {
			this.db = new MapDatabase(this.baseDir);
		} catch (MapDatabaseException e) {
			throw new MossWorldLoadException(
					Messages.getString("MossWorld.ERR_DB")); //$NON-NLS-1$
		}

		this.nc = new NodeCache(this.db);
		this.nm = new LocalNodeManager(this.db.nodes);
		// this.db = new MapDatabase(this.baseDir);
		try {
			MapGenerators.setDefaultMapGenerator(
					new MapGenerators.SimplexMapGenerator(), this.nm, 8448);
		} catch (MapGeneratorException e) {
			System.err.println(Messages
					.getString("MossWorld.MG_SELECT_FAILURE")); //$NON-NLS-1$
			System.exit(4);
		}
		this.sdb = new ScriptableDatabase(this.baseDir);
		this.fp = new FuturesProcessor();
		this.mossEnv = new MossScriptEnv(this.sdb, this.nc, this.fp, this.nm);
		this.sEnv = new ScriptEnv(this.mossEnv);
		List<IMossFile> scripts = this.game.getScripts();
		for (IMossFile sc : scripts) {
			this.sEnv.runScript(sc);
		}
		this.evp = new EventProcessor(this.mossEnv,
				ThreadContext.CONTEXT_SCRIPT);
		if (port >= 0) {
			logger.error(Messages.getString("MossWorld.NO_NETWORKING_NOW")); //$NON-NLS-1$
			/*
			 * try { this.snv = new ServerNetworkingManager(port, this); } catch
			 * (IOException e) { throw new MossWorldLoadException(
			 * "Failure in opening server socket for listening!"); }
			 */
		} // else {
		/*	*/this.rp = new LocalRenderPreparator(this.rend, this.nc);
		this.rp.setNodeManager(nm);
		/*	*/this.rend = RenderProcessor.init(this.nm, this.rp);
		// }

	}

	public void enqueueEvent(IMossEvent e) throws InterruptedException {
		this.evp.eventQueue.put(e);
	}

	public static void main(String[] args) throws MossWorldLoadException,
			MapDatabaseException, ConfigurationException, IOException {
        new MossWorld("test", -1); //$NON-NLS-1$

    }
}
