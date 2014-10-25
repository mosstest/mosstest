package net.mosstest.servercore;

import com.google.common.collect.HashBiMap;
import net.mosstest.scripting.MapNode;
import org.iq80.leveldb.DB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;

// TODO: Auto-generated Javadoc

/**
 * The Class NodeManager.
 */
public class NodeManager {

    public static final int NODEDEF_MAX = 16384;
    /**
     * The defined nodes.
     */
    private ArrayList<MapNode> definedNodes = new ArrayList<>();

    /**
     * The def node by name.
     */
    private HashMap<String, MapNode> defNodeByName = new HashMap<>();

    /**
     * The pending.
     */
    private HashBiMap<Short, String> pending = HashBiMap.create();

    /**
     * The node db.
     */
    private DB nodeDb;

    /**
     * The Constant unknownFallbackNode.
     */
    private static final MapNode unknownFallbackNode; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$


    static {

        unknownFallbackNode = new MapNode("sys:unknown", Messages.getString("NodeManager.DESC_UNKNWN_NODE"), "builtin/unknown.png", 1);
        unknownFallbackNode.setNodeId((short) -1);
    }

    /* instance creation */ {
        this.definedNodes.add(this.unknownFallbackNode);
    }

    /**
     * Gets the node.
     *
     * @param nodeId the node id
     * @return the node
     */
    public MapNode getNode(short nodeId) {
        return this.definedNodes.get(nodeId);
    }

    /**
     * Put node.
     *
     * @param node the node
     * @return the short
     * @throws MossWorldLoadException the moss world load exception
     */
    public short putNode(MapNode node) throws MossWorldLoadException {
        if (this.pending.containsValue(node.nodeName)) {
            node.setNodeId(this.pending.inverse().get(node.nodeName));
            this.definedNodes.set(this.pending.inverse().get(node.nodeName),
                    node);
            this.defNodeByName.put(node.nodeName, node);
        } else {
            if (this.definedNodes.size() > NODEDEF_MAX)
                throw new MossWorldLoadException("Too many nodedefs"); //$NON-NLS-1$

            node.setNodeId((short) this.definedNodes.size());

            this.definedNodes.add(node);
            this.defNodeByName.put(node.nodeName, node);
            this.nodeDb.put(new byte[]{(byte) (node.getNodeId() >>> 8),
                    (byte) (node.getNodeId() & 0xFF)}, bytes(node.nodeName));
        }


        return node.getNodeId();
    }

    /**
     * Put node alias.
     *
     * @param alias the alias
     * @param dst   the dst
     */
    public void putNodeAlias(String alias, String dst) {
        MapNode dstNode = this.defNodeByName.get(dst);
        this.defNodeByName.put(alias, dstNode);
    }

    /**
     * Gets the node.
     *
     * @param string     the string
     * @param isModified the is modified
     * @return the node
     */
    public MapNode getNode(String string, boolean isModified) {
        MapNode r = this.defNodeByName.get(string);
        return r == null ? this.unknownFallbackNode : r;
    }


    /**
     * Gets the node.
     *
     * @param string the string
     * @return the node
     */
    public MapNode getNode(String string) {

        MapNode r = this.defNodeByName.get(string);
        return r == null ? this.unknownFallbackNode : r;
    }

    /**
     * Instantiates a new node manager.
     *
     * @param nodedb the nodedb
     */
    public NodeManager(DB nodedb) {
        this.nodeDb = nodedb;
        for (Entry<byte[], byte[]> entry : nodedb) {
            short parsedId = (short) (entry.getKey()[0] * 256 + entry.getKey()[1]);
            String parsedString = asString(entry.getValue());
            this.pending.put(parsedId, parsedString);
        }
    }

    /**
     * Gets the unknown fallback node.
     *
     * @return the unknown fallback node
     */
    public static MapNode getUnknownFallbackNode() {
        return unknownFallbackNode;
    }
}
