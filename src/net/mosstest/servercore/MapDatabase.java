package net.mosstest.servercore;

import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.Position;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

//import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

// TODO: Auto-generated Javadoc

/**
 * The Class MapDatabase.
 */
public class MapDatabase {
    private static final Logger logger = Logger.getLogger(MapDatabase.class);
    /**
     * The map.
     */
    DB map;

    /**
     * The entities.
     */
    DB entities;

    /**
     * The metadata.
     */
    DB metadata;

    /**
     * The map heavies.
     */
    DB mapHeavies;

    /**
     * The landclaims.
     */
    DB landclaims;

    /**
     * The players.
     */
    DB players;

    /**
     * The nodes.
     */
    public DB nodes;

    /**
     * Instantiates a new map database.
     *
     * @param basedir the basedir
     * @throws MapDatabaseException   the map database exception
     * @throws MossWorldLoadException the moss world load exception
     */
    @SuppressWarnings("nls")
    public MapDatabase(File basedir) throws MapDatabaseException,
            MossWorldLoadException {
        File dbDir = new File(basedir, "db"); //$NON-NLS-1$
        dbDir.mkdirs();
        try {

            Options options = new Options();
            options.comparator(null);
            this.map = factory.open(new File(dbDir, "map"), options); //$NON-NLS-1$
            this.mapHeavies = factory.open(new File(dbDir, "mapHeavies"), //$NON-NLS-1$
                    options);
            this.entities = factory.open(new File(dbDir, "entities"), options); //$NON-NLS-1$
            this.metadata = factory.open(new File(dbDir, "metadata"), options); //$NON-NLS-1$
            this.players = factory.open(new File(dbDir, "players"), options); //$NON-NLS-1$
            this.nodes = factory.open(new File(dbDir, "nodes"), options); //$NON-NLS-1$
        } catch (IOException e) {
            logger.error(MessageFormat.format(Messages.getString("DB_LOAD_IOEXCEPTION"), e.getMessage()));
            throw new MossWorldLoadException(Messages.getString("MapDatabase.ERR_DB_FAIL"), e); //$NON-NLS-1$
        }

        logger.info(Messages.getString("DB_NORMAL_LOAD"));

    }

    /**
     * Closes the database, making it invalid to use for any stores or lookups.
     */
    public void close() throws MapDatabaseException {


        logger.info(Messages.getString("DB_SHUTDOWN_NORMAL"));

        try {
            this.map.close();
            this.entities.close();
            this.metadata.close();
        } catch (IOException e) {
            throw new MapDatabaseException("Database shutdown failed!", e,
                    MapDatabaseException.SEVERITY_UNKNOWN
                            | MapDatabaseException.SEVERITY_FATAL_TRANSIENT); //$NON-NLS-1$
        }
    }


    /**
     * Gets the chunk, but does not generate it if it does not exist.
     *
     * @param pos the position
     * @return the chunk
     */
    public MapChunk getChunk(final Position pos) throws MapGeneratorException {

        byte[] chunk = this.map.get(pos.toBytes());
        if (chunk == null) {
            return null;
        }
        try {
            return new MapChunk(chunk);
        } catch (MosstestFatalDeathException e) {
            try {
                this.close();
            } catch (MapDatabaseException e1) {
                logger.error(MessageFormat.format(Messages.getString("DB_EMERGENCY_SHUTDOWN_FAIL"), e1.getMessage()));
                throw new MosstestFatalDeathException(e1);
            }
            logger.warn(Messages.getString("DB_SHUTDOWN_EMERGENCY"));

            // MUST rethrow
            throw e;
        }

    }

    /**
     * Adds the map chunk.
     *
     * @param pos      the pos
     * @param mapChunk the map chunk
     */

    void addMapChunk(Position pos, MapChunk mapChunk) {
        this.map.put(pos.toBytes(), mapChunk.writeLight(true));

    }

    /**
     * Gets the heavy.
     *
     * @param pos the pos
     * @return the heavy
     */
    public byte[] getHeavy(Position pos) {
        return this.mapHeavies.get(pos.toBytes());
    }

}
