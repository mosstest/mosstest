
package net.mosstest.scripting;

import net.mosstest.scripting.authentication.MossAuthenticator;
import net.mosstest.scripting.events.IMossEvent;
import net.mosstest.scripting.handlers.MossEventHandler;
import net.mosstest.scripting.handlers.MossNodeChangeHandler;
import net.mosstest.servercore.*;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This class is used by scripts and script-facing portions of Mosstest. Methods
 * restricted to be called from trusted Java-side shall pass a
 * {@link ScriptSandboxBorderToken}. Each event fired will run in the thread
 * pool. All requests via this API need not concern themselves with threading as
 * everything is handled by Mosstest itself.
 * <p>
 * The event handlers called are the ones defined via this class's registerOnFoo
 * methods, followed by any handlers defined in an instance of NodeParams via an
 * anonymous inner class, and finally with the default handler.
 * <p>
 * The order in which handlers registered here are called is undefined due to
 * the undefined order of scripts being loaded. Generally, this is planned to
 * occur in an order based on the SHA512 hash of the script. Comments with dummy
 * information may be used by the script author to attempt to set the position
 * of a script in the execution order via manipulating the hash. Handlers of the
 * same types within the same script are guaranteed to be called in order.
 * <p>
 * An event handler may interrupt handling of the event so that no further event
 * handlers nor the default are ever called, by returning the proper boolean value
 *
 * @author rarkenin
 * @version 0.0
 * @since 0.0
 */
public class MossScriptEnv {
    public static final short SCRIPT_API_VERSION = 1;
    public static final short MIN_SCRIPT_API_VERSION = 1;
    public static final short MAX_SCRIPT_API_VERSION = 1;
    public void registerNodeChangeHandler(MossNodeChangeHandler h) {

    }

    private InheritableThreadLocal<Short> requestedScriptApiVer = new InheritableThreadLocal<Short>(){
        @Override
        protected Short initialValue() {
            return MAX_SCRIPT_API_VERSION;
        }
    };

    public void setRequestedScriptApiVer(short requestedScriptApiVer) {
        if(requestedScriptApiVer >= MIN_SCRIPT_API_VERSION && requestedScriptApiVer <= MAX_SCRIPT_API_VERSION)
            this.requestedScriptApiVer.set(requestedScriptApiVer);
        else {
            throw new MosstestFatalDeathException("A plugin requests an unsatisfiable script API version.");
        }
    }

    private HashMap<Class<? extends IMossEvent>, ArrayList<MossEventHandler>> eventHandlers;

    private ScriptableDatabase db;

    private MapCache nc;

    private FuturesProcessor fp;

    private INodeManager nm;


    /**
     * Sends a chat message to a player.
     *
     * @param recipient A {@link Player} object representing the recipient. A Player
     *                  object may be constructed with
     *                  {@link MossScriptEnv#getPlayerByName(String)}.
     * @param from      A player object representing the sender. A Player object may
     *                  be constructed with
     *                  {@link MossScriptEnv#getPlayerByName(String)}. If null a
     *                  message is sent showing to users as having been sent by the
     *                  server with the prefix <code>[*] Server:</code>
     * @param message   A string representing the message that shall be sent to the
     *                  specified recipient.
     */
    public void sendChatMessage(Player recipient, Player from, String message) {
        // TODO
    }

    /**
     * Sends a chat message to all players.
     *
     * @param from    A player object representing the sender. A Player object may
     *                be constructed with
     *                {@link MossScriptEnv#getPlayerByName(String)}. If null a
     *                message is sent showing to users as having been sent by the
     *                server with the prefix <code>[*] Server:</code>
     * @param message A string representing the message that shall be sent to the
     *                specified recipient.
     */
    public void sendChatAll(Player from, String message) {
        // TODO
    }

    /**
     * Sets the health on an entity or player.
     *
     * @param p      The player to set health on.
     * @param health A positive integer representing the amount of health to set.
     */
    public void setHp(Player p, int health) {
        // TODO Once we have players doing stuff
    }

    /**
     * Damages the tool of a player corresponding to a dig. The player's
     * currently selected tool is damaged.
     *
     * @param actor The player that is digging a node.
     * @param nd    The node dug.
     * @throws MossScriptException Thrown if the current tool cannot be used to dig the node.
     */
    public void damageTool(Player actor, MapNode nd) throws MossScriptException {
        // TODO Auto-generated method stub

    }

    /**
     * Gives a player an item. The item stack will be added to the player's
     * default inventory, adding to the first available partial stack. If no
     * partial stacks are available the item is added to the first open slot in
     * the inventory.
     *
     * @param player the player
     * @param item   the item
     * @return True if the item could be added, false if the item could not be
     * added due to insufficient space.
     */
    public boolean givePlayer(Player player, MossItem.Stack item) {
        MossInventory mi = player.getInventory("default", 4, 8, 128);

        // use side effect
        return (mi.addItem(item) == item.amount);

    }

    /**
     * Sets a node of the world to a given type. This cannot be called on a
     * NodePosition with an existing solid node; use
     * {@link #removeNode(NodePosition)} first.
     *
     * @param pos  The position at which to set a node.
     * @param node The node to place at that position.
     */
    public void setNode(NodePosition pos, MapNode node) throws MapGeneratorException {
        MapChunk chk = this.nc.getChunkNoGenerate(pos.chunk);
        if (chk == null)
            return;
        if (!(this.nm.containsNode(node) || node instanceof IDynamicNode))
            throw new IllegalArgumentException("The mapnode passed is not contained in the world node manager");

        chk.setNode(pos.xl, pos.yl, pos.zl, node.getNodeId());
    }

    /**
     * Removes a node, setting it to air. This may be called on a NodePosition
     * with an existing solid node.
     *
     * @param pos The NodePosition at which to remove the node.
     */
    public void removeNode(NodePosition pos) throws MapGeneratorException {
        MapChunk chk = this.nc.getChunkNoGenerate(pos.chunk);
        if (chk == null)
            return;
        chk.setNode(pos.xl, pos.yl, pos.zl, this.nm.getNode("mg:air") //$NON-NLS-1$
                .getNodeId());
        this.nc.setChunk(pos.chunk, chk);
    }

    /**
     * Get the MapNode at a certain location.
     *
     * @param pos The location at which to get the node
     * @return A MapNode object representing the node at that location.
     * @throws MapGeneratorException the map generator exception
     */
    public MapNode getNode(NodePosition pos) throws MapGeneratorException {
        return this.nm.getNode((short) this.nc.getChunk(pos.chunk).getNodeId(
                pos.xl, pos.yl, pos.zl));

    }

    /**
     * Registers a mapnode in the world, allowing it to be placed.
     *
     * @param nd
     * @throws MossWorldLoadException
     */
    public void registerNode(MapNode nd) throws MossWorldLoadException {
        this.nm.putNode(nd);
    }

    /**
     * Register a new liquid in the node manager, generating intermediates as needed.
     *
     * @param sysname        The name such as default:lava to set. The prefix mg: is used
     *                       for mapgen-specific nodes, and should be done by creating a
     *                       node with a different prefix and aliasing mg:foo to it.
     * @param userFacingName The name to display in the UI, such as Lava or Iron Ore
     * @param params         An implementation of the {@link LiquidNodeParams} interface
     *                       detailing the action of the node. {@link LiquidSourceNodeParams} and
     *                       {@link LiquidFlowingNodeParams} are valid for liquid sources and flowing liquid nodes,
     *                       respectively.
     * @param flowParams     the source params
     * @param textures       A string stating the filename of the textures image.
     * @param light          The amount of light from 0 to 255 to be emitted.
     * @return The MapNode object that has been created and added to the
     * manager.
     * @throws MossWorldLoadException If an exception occurs during the execution of the
     *                                registering.
     */
    public LiquidNode registerLiquid(@NonNls String sysname, String userFacingName,
                                     LiquidNodeParams params, LiquidNodeParams flowParams,
                                     String textures, int light) throws MossWorldLoadException {
        LiquidNode nd = new LiquidNode(params, textures, sysname,
                userFacingName, light);
        this.nm.putNode(nd);
        nd.level = 0;
        for (int i = 1; i < 8; i++) {
            LiquidNode innerNd = new LiquidNode(flowParams, textures, sysname
                    + "$LEVEL$" + i, userFacingName, light); //$NON-NLS-1$
            innerNd.setByBounds(-.5f, .5f, -.5f, .5f, -.5f, (i / 8f) - 0.5f);
            nd.liquidLevels[i] = innerNd;
            innerNd.liquidLevels = nd.liquidLevels;
            innerNd.level = i;
            this.nm.putNode(innerNd);
        }
        nd.liquidLevels[0] = nd;
        return nd;
    }

    /**
     * Registers a node alias. Since the map generator and scripts work via
     * string names, registering an alias of mg:dirt to myscript:specialdirt
     * will cause a mapgen that recognizes mg:dirt as a generated element to use
     * specialdirt for that.
     *
     * @param alias The alias to create, i.e. mg:dirt
     * @param dst   The existing node to set as the alias target, i.e
     *              myscript:specialdirt. This element must already exist.
     */
    public void registerNodeAlias(String alias, String dst) {
        this.nm.putNodeAlias(alias, dst);
    }


    /**
     * Gets the inv by name.
     *
     * @param player the player
     * @param name   the name
     * @return the inv by name
     */
    public MossInventory getInvByName(Player player, String name) {
        return null;
    }

    /**
     * Creates the inv by name.
     *
     * @param p    the p
     * @param name the name
     * @return the moss inventory
     */
    public MossInventory createInvByName(Player p, String name) {
        return null;
    }

    /**
     * Gets the player by name.
     *
     * @param name the name
     * @return the player by name
     */
    public Player getPlayerByName(String name) {
        return null;
    }

    /**
     * Gets the node by name.
     *
     * @param name the name
     * @return the node by name
     */
    public MapNode getNodeByName(String name) {
        return null;
    }

    /**
     * Gets the db.
     *
     * @return the db
     */
    public ScriptableDatabase getDb() {
        return this.db;
    }

    /**
     * Instantiates a new moss script env.
     *
     * @param db the db
     * @param nc the nc
     * @param fp the fp
     * @param nm the nm
     */
    public MossScriptEnv(ScriptableDatabase db, MapCache nc,
                         FuturesProcessor fp, INodeManager nm) {
        this.db = db;
        this.nc = nc;
        this.fp = fp;
        this.nm = nm;
    }

    /**
     * Gets the futures processor.
     *
     * @return the futures processor
     */
    public FuturesProcessor getFuturesProcessor() {
        return this.fp;
    }

    private HashMap<Class<? extends IMossEvent>, List<WrappedHandler>> handlers = new HashMap<>();

    public List<WrappedHandler> getEventHandlers(
            Class<? extends IMossEvent> clazz) {
        List<WrappedHandler> l = Collections.unmodifiableList(handlers.get(clazz));
        if (l == null) {
            handlers.put(clazz, new ArrayList<WrappedHandler>());
            return Collections.EMPTY_LIST;
        }
        return l;

    }

    public void registerHandler(MossEventHandler handler, Class<? extends IMossEvent> clazz) {
        List<WrappedHandler> l = handlers.get(clazz);
        if (l == null) {
            l = new ArrayList<WrappedHandler>();
            handlers.put(clazz, l);
        }
        l.add(new WrappedHandler(handler, this.requestedScriptApiVer.get()));

    }

    public MossAuthenticator getAuthenticator(String username) {
        throw new NotImplementedException("Need to implement getting an authenticator");
        //return null;
    }
}
