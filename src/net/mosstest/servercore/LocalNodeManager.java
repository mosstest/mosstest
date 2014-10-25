package net.mosstest.servercore;

import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import net.mosstest.scripting.MapNode;

import org.iq80.leveldb.DB;

// TODO: Auto-generated Javadoc

/**
 * The Class LocalNodeManager.
 */
public class LocalNodeManager extends AbstractNodeManager {

    public static final int NODE_ID_MAX = 16384;
    /**
     * The node db.
     */
    private DB nodeDb;
    private ItemManager itemManager;

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(short)
     */
    @Override
    public MapNode getNode(short nodeId) {
        return this.definedNodes.get(nodeId);
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#putNode(net.mosstest.scripting.MapNode)
     */
    @Override
    public short putNode(MapNode node) throws MossWorldLoadException {
        if (this.pending.containsValue(node.nodeName)) {
            node.setNodeId(this.pending.inverse().get(node.nodeName));
            this.definedNodes.set(this.pending.inverse().get(node.nodeName),
                    node);
            this.defNodeByName.put(node.nodeName, node);
            node.setDropItem(this.itemManager.getForNode(node));
        } else {
            if (this.definedNodes.size() > NODE_ID_MAX)
                throw new MossWorldLoadException("Too many nodedefs"); //$NON-NLS-1$

            node.setNodeId((short) this.definedNodes.size());

            this.definedNodes.add(node);
            this.defNodeByName.put(node.nodeName, node);
            this.containedNodes.add(node);
            this.nodeDb.put(new byte[]{(byte) (node.getNodeId() >>> 8),
                    (byte) (node.getNodeId() & 0xFF)}, bytes(node.nodeName));
        }


        return node.getNodeId();
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#putNodeAlias(java.lang.String, java.lang.String)
     */
    @Override
    public void putNodeAlias(String alias, String dst) {
        MapNode dstNode = this.defNodeByName.get(dst);
        this.defNodeByName.put(alias, dstNode);
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(java.lang.String, boolean)
     */
    @Override
    public MapNode getNode(String string, boolean isModified) {
        MapNode r = this.defNodeByName.get(string);
        return (r == null) ? AbstractNodeManager.MAPNODE_UNKNOWN : r;
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(java.lang.String)
     */
    @Override
    public MapNode getNode(String string) {

        MapNode r = this.defNodeByName.get(string);
        return (r == null) ? AbstractNodeManager.MAPNODE_UNKNOWN : r;
    }

    /**
     * Instantiates a new local node manager.
     *
     * @param nodedb the nodedb
     */
    public LocalNodeManager(DB nodedb, ItemManager mgr) {
        this.nodeDb = nodedb;
        this.itemManager = mgr;
        this.init();
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#init()
     */
    @Override
    public void init() {
        for (Entry<byte[], byte[]> entry : this.nodeDb) {
            short parsedId = (short) (entry.getKey()[0] * 256 + entry.getKey()[1]);
            String parsedString = asString(entry.getValue());
            this.pending.put(parsedId, parsedString);

        }
    }

    @Override
    public boolean containsNode(MapNode node) {
        return this.containedNodes.contains(node);
    }
    
    public List<MapNode> getNodeDefinitions () {
    	return Collections.unmodifiableList(definedNodes);
    }
}
