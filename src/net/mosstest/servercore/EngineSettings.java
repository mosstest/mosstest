package net.mosstest.servercore;

// TODO: Auto-generated Javadoc

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NonNls;

/**
 * The Class EngineSettings.
 */
public class EngineSettings {
private static final Logger logger = Logger.getLogger(EngineSettings.class);
	
	/**
	 * Gets the int.
	 *
	 * @param name the name
	 * @param def the def
	 * @return the int
	 */
	public static int getInt(@NonNls String name, int def){
        logger.error(Messages.getString("FIXME_MESSAGE"));
        Bugsnag.getBugsnag().notify(new RuntimeException("Missing functionality--EngineSettings"));
		if("forced".equals("false")){ //$NON-NLS-1$ //$NON-NLS-2$
			return 0; //TODO this case
		}
		return def;
	}

	/**
	 * Gets the bool.
	 *
	 * @param string the string
	 * @param def the def
	 * @return the bool
	 */
	public static boolean getBool(String string, boolean def) {
        logger.error(Messages.getString("FIXME_MESSAGE"));
		if("forced".equals("false")){ //$NON-NLS-1$ //$NON-NLS-2$
			return false; //TODO this case
		}
		return def;
	}
}
