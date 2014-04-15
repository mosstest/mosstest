package net.mosstest.servercore;

import net.mosstest.scripting.MapNode;
import net.mosstest.scripting.MossScriptEnv;
import net.mosstest.scripting.MossScriptException;
import net.mosstest.scripting.NodePosition;
import net.mosstest.scripting.events.IMossEvent;
import net.mosstest.scripting.events.MossNodeChangeEvent;
import net.mosstest.scripting.events.ThreadStopEvent;
import net.mosstest.scripting.handlers.MossEventHandler;
import net.mosstest.scripting.handlers.MossNodeChangeHandler;
import net.mosstest.servercore.MosstestSecurityManager.ThreadContext;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: Auto-generated Javadoc

/**
 * The Class EventProcessor.
 *
 * @author rarkenin, hexafraction
 *         <p>
 *         Blargh.
 *         <p>
 *         This is a nasty thread pool. If you don't understand threading or
 *         Java well, you may want to stick to only accessing the queue as
 *         otherwise asphyxiation, drowning, or chlorine poisoning may occur.
 *         USE ACCESS METHODS OTHER THAN THE QUEUE AT YOUR OWN RISK.
 */
public class EventProcessor {

    public static final int EVT_QUEUE_CAPACITY = 40;
    static Logger logger = Logger.getLogger(EventProcessor.class);

    ArrayBlockingQueue<IMossEvent> eventQueue = new ArrayBlockingQueue<>(
            EngineSettings.getInt("eventQueueCapacity", EVT_QUEUE_CAPACITY), false); //$NON-NLS-1$

    protected final int maxEventThreads = EngineSettings.getInt(
            "maxEventThreads", 8); //$NON-NLS-1$

    protected final int initialEventThreads = EngineSettings.getInt(
            "initialEventThreads", 8); //$NON-NLS-1$

    ThreadGroup eventProcessorGroup = new ThreadGroup(
            Messages.getString("EventProcessor.THREADGROUP")); //$NON-NLS-1$

    protected AtomicBoolean runManager = new AtomicBoolean(true);

    protected final int sampleInterval = EngineSettings.getInt(
            "eventQueueTuneSampleInterval", 100); //$NON-NLS-1$

    protected final int upshift = EngineSettings.getInt(
            "eventQueueTuneUpshift", 90); //$NON-NLS-1$

    protected final int downshift = EngineSettings.getInt(
            "eventQueueTuneDownshift", 10); //$NON-NLS-1$

    protected final int samples = EngineSettings.getInt(
            "eventQueueTuneSamples", 100); //$NON-NLS-1$

    protected final AtomicInteger currentThreads = new AtomicInteger(0);

    private final MossScriptEnv ev;

    private final ThreadContext tc;

    /**
     * Process events.
     */
    void processEvents() {
        MosstestSecurityManager.instance.setThreadContext(this.tc);
        boolean run = true; // Not synchronized as only used locally
        queueLoop:
        while (true) {
            try {
                IMossEvent myEvent = this.eventQueue.take();
                {// Section for actually handling the events
                    if (myEvent instanceof ThreadStopEvent) {
                        this.currentThreads.decrementAndGet();
                        break;
                    }
                    dispatchEvent(myEvent);
                }

            } catch (InterruptedException e) {
                // thread struck, shut down the operation.
            }

        }
        logger.info("A thread is shutting down."); //$NON-NLS-1$
        this.currentThreads.decrementAndGet();

    }

    private void dispatchEvent(IMossEvent evt) {
        List<MossEventHandler> evtHandlerList = this.ev
                .getEventHandlers(evt.getClass());
        try {
            for (MossEventHandler ourHandler : evtHandlerList) {
                if (dispatchEventInner(ourHandler, evt)) {
                    return;
                }
            }
            DefaultEventHandlers.processEvent(evt, this.ev);
        } catch (MossScriptException | IllegalArgumentException e) {
            logger.warn(e.getClass().getName() + " upon processing event: "
                    + e.getMessage());
        }
    }

    private boolean dispatchEventInner(MossEventHandler ourHandler,
                                       IMossEvent evt) throws IllegalArgumentException {
        try {
            if (evt instanceof MossNodeChangeEvent) {

                return ((MossNodeChangeHandler) ourHandler)
                        .onAction((MossNodeChangeEvent) evt);

            } else
                throw new IllegalArgumentException(
                        "The event passed in was not a type that Mosstest is equipped to handle.");

        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    "The event handler did not match in type with the event.", e);
        }
    }

    /**
     * Constructs a new event processor.
     *
     * @param ev A script environment populated with event handlers.
     * @param tc the tc
     */
    public EventProcessor(MossScriptEnv ev, ThreadContext tc) {
        this.ev = ev;
        this.tc = tc;
        Thread manager = new Thread(this.eventProcessorGroup,
                new Runnable() {
                    /**
                     * The manager. Controls the thread number.
                     */
                    @SuppressWarnings("nls")
                    @Override
                    public void run() {

                        logger.info("The manager thread has been started."); //$NON-NLS-1$
                        int ticks = 0;
                        int ticksBusy = 0;
                        int lSampleInterval = EventProcessor.this.sampleInterval;
                        int lUpshift = EventProcessor.this.upshift;
                        int lDownshift = EventProcessor.this.downshift;
                        int lSamples = EventProcessor.this.samples;
                        Thread[] threads = new Thread[EventProcessor.this.maxEventThreads];

                        for (int i = 0; i < EventProcessor.this.initialEventThreads; i++) {
                            int c = EventProcessor.this.currentThreads.get();
                            threads[c] = new Thread(
                                    EventProcessor.this.eventProcessorGroup,
                                    new Runnable() {

                                        @Override
                                        public void run() {
                                            logger.debug(Messages
                                                    .getString("EventProcessor.MSG_THREAD_START")); //$NON-NLS-1$
                                            processEvents();
                                        }

                                    });
                            threads[c].setDaemon(true);
                            threads[c].start();

                            EventProcessor.this.currentThreads.incrementAndGet();

                        }
                        while (EventProcessor.this.runManager.get()) {
                            ticks++;
                            if (!EventProcessor.this.eventQueue.isEmpty()) {
                                ticksBusy++;
                            }
                            if (ticks >= lSamples) {
                                if ((EventProcessor.this.currentThreads.get() < EventProcessor.this.maxEventThreads)
                                        && (((float) ticksBusy / (float) ticks) > ((float) lUpshift / (float) lSamples))) {
                                    new Thread(
                                            EventProcessor.this.eventProcessorGroup,
                                            new Runnable() {

                                                @Override
                                                public void run() {
                                                    System.out.println(Messages
                                                            .getString("EventProcessor.MSG_ADD_DYNAMIC")); //$NON-NLS-1$
                                                    processEvents();
                                                }

                                            }).run();
                                    EventProcessor.this.currentThreads
                                            .incrementAndGet();

                                }
                                if (((float) ticksBusy / (float) ticks) < ((float) lDownshift / (float) lSamples)) {
                                    logger.info((Messages
                                            .getString("EventProcessor.MSG_STOP_ONE_THREAD"))); //$NON-NLS-1$
                                    EventProcessor.this.eventQueue
                                            .add(new ThreadStopEvent());

                                }
                                ticks = 0;
                                ticksBusy = 0;
                            }
                            try {
                                Thread.sleep(lSampleInterval);
                            } catch (InterruptedException e) {
                                // manager awoke, no problem
                            }
                        }
                    }
                }, Messages.getString("EventProcessor.THREAD_NAME_MGR"));

        manager.setDaemon(true);
        manager.start();

    }

    /**
     * Tests the event processor
     */
    public static void main(String... args) throws ConfigurationException, MossWorldLoadException, MapDatabaseException, IOException, InterruptedException {
        MossWorld mw = new MossWorld("test", -1);
        EventProcessor evp = mw.getEvp();
        MossNodeChangeHandler handler = new MossNodeChangeHandler() {
            @Override
            public boolean onAction(MossNodeChangeEvent evt) {
                logger.info("Received test event " + evt.toString());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return false;
            }
        };
        mw.getMossEnv().registerHandler(handler, MossNodeChangeEvent.class);
        long s = 0;
        while (true) {
            logger.debug("Enqueueing!");
            evp.eventQueue.put(new MossNodeChangeEvent(
                    null,
                    new NodePosition(0, 1, 2, 3, (byte) 4, (byte) 5, (byte) 6),
                    s++, MossNodeChangeEvent.NodeActionType.NODE_ACTION_REPLACE,
                    new MapNode("test", "Test", "test/test.png", 0), null));
            //Thread.sleep(1000);
        }
    }

}
