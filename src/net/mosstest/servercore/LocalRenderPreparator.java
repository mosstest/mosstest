package net.mosstest.servercore;

import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.Player;
import net.mosstest.scripting.Position;
import org.apache.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;

// TODO: Auto-generated Javadoc

/**
 * The Class LocalRenderPreparator.
 */
public class LocalRenderPreparator implements IRenderPreparator {

    public static final int CHUNK_REQUESTS_QUEUE_SIZE = 1024;
    /**
     * The logger.
     */
    static Logger logger = Logger.getLogger(LocalRenderPreparator.class);

    /**
     * The Class ChunkLookupRunnable.
     */
    public class ChunkLookupRunnable implements Runnable {

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            while (LocalRenderPreparator.this.run) {
                try {
                    Position requested = LocalRenderPreparator.this.chunkRequests
                            .take();
                    System.out
                            .println(Messages
                                    .getString("LocalRenderPreparator.MSG_REQUESTED") + requested.x + "," //$NON-NLS-1$ //$NON-NLS-2$
                                    + requested.y + "," + requested.z); //$NON-NLS-1$
                    MapChunk chk = LocalRenderPreparator.this.nc
                            .getChunk(requested);
                    chk.pos = requested;
                    LocalRenderPreparator.this.rend.renderEventQueue
                            .put(new MossRenderChunkEvent(chk));
                } catch (InterruptedException e) {
                    // pass
                } catch (MapGeneratorException e) {
                    logger.error(Messages
                            .getString("LocalRenderPreparator.MG_EXCEPT")); //$NON-NLS-1$
                }
            }
        }

    }

    /**
     * The local player.
     */
    private Player localPlayer;

    /**
     * The rend.
     */
    private RenderProcessor rend;

    /**
     * The nc.
     */
    private MapCache nc;

    /**
     * The run.
     */
    private volatile boolean run = true;

    /**
     * The chunk requests.
     */
    public ArrayBlockingQueue<Position> chunkRequests = new ArrayBlockingQueue<>(
            CHUNK_REQUESTS_QUEUE_SIZE);
    // private HashMap<Position, Position> outstandingChunks = new HashMap<>();

    /**
     * The lookup thread.
     */
    private Thread lookupThread = new Thread(new ChunkLookupRunnable());

    /**
     * The nm.
     */
    private INodeManager nm;

    /* (non-Javadoc)
     * @see net.mosstest.servercore.IRenderPreparator#requestChunk(net.mosstest.scripting.Position)
     */
    @Override
    public MapChunk requestChunk(Position pos) throws MapGeneratorException,
            InterruptedException {
        try {
            MapChunk chk = this.nc.getChunkFailFast(pos);
            logger.trace(chk == null ? "Chunk is null" : "Chunk obtained is: " + chk //$NON-NLS-1$
                    .toString());
            if (chk == null) {
                this.chunkRequests.put(pos);
            }
            return chk;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Instantiates a new local render preparator.
     *
     * @param rp the rp
     * @param nc the nc
     */
    public LocalRenderPreparator(RenderProcessor rp, MapCache nc) {
        this.rend = rp;
        this.nc = nc;
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.IRenderPreparator#shutdown()
     */
    @Override
    public void shutdown() {
        this.run = false;
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.IRenderPreparator#recvOutstandingChunk(net.mosstest.scripting.Position, net.mosstest.scripting.MapChunk)
     */
    @Override
    public void recvOutstandingChunk(Position pos, MapChunk chk) {
        // pass
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.IRenderPreparator#start()
     */
    @Override
    public void start() {

        logger.info(Messages
                .getString("LocalRenderPreparator.START_MSG")); //$NON-NLS-1$
        this.lookupThread.setDaemon(true);
        this.lookupThread.start();

    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.IRenderPreparator#setRenderProcessor(net.mosstest.servercore.RenderProcessor)
     */
    @Override
    public void setRenderProcessor(RenderProcessor rend) {
        this.rend = rend;
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.IRenderPreparator#setNodeManager(net.mosstest.servercore.INodeManager)
     */
    @Override
    public void setNodeManager(INodeManager nm) {
        this.nm = nm;
    }


    /* (non-Javadoc)
     * @see net.mosstest.servercore.PlayerCommunicator#forceSetPosition(net.mosstest.scripting.Player, int, int, int, double, double, double)
     */
    @Override
    public void forceSetPosition(Player pl, int cx, int cy, int cz,
                                 double offsetx, double offsety, double offsetz) throws InterruptedException {
        if (!pl.equals(this.localPlayer))
            return;
        MossRenderSetPositionEvent mrspe = new MossRenderSetPositionEvent(
                offsetx, offsety, offsetz, cx, cy, cz);
        this.rend.renderEventQueue.put(mrspe);
    }

    // TODO: Handle player movement, other server->client events affecting
    // rendering

}
