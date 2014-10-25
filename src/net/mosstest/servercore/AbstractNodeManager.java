package net.mosstest.servercore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.mosstest.scripting.MapNode;

import org.jetbrains.annotations.NonNls;

import com.google.common.collect.HashBiMap;

// TODO: Auto-generated Javadoc

/**
 * The Class AbstractNodeManager.
 */
public abstract class AbstractNodeManager implements INodeManager {

    /**
     * The defined nodes.
     */
    protected ArrayList<MapNode> definedNodes = new ArrayList<>();

    /**
     * The def node by name.
     */
    protected HashMap<String, MapNode> defNodeByName = new HashMap<>();

    /**
     * The pending.
     */
    protected HashBiMap<Short, String> pending = HashBiMap.create();

    protected Set<MapNode> containedNodes = Collections.newSetFromMap(new ConcurrentHashMap<MapNode, Boolean>());

    /**
     * The Constant MAPNODE_UNKNOWN.
     */
    public static final MapNode MAPNODE_UNKNOWN;

    @NonNls
    private static final String SYS_UNKNOWN = "sys:unknown";

    @NonNls
    private static final String BUILTIN_UNKNOWN_TEXTURE = "builtin/unknown.png";

    static {
        MAPNODE_UNKNOWN = new MapNode(SYS_UNKNOWN, Messages.getString("AbstractNodeManager.DESC_UNKNOWN_NODE"), BUILTIN_UNKNOWN_TEXTURE,
                1); //$NON-NLS-1$ //$NON-NLS-2$
        MAPNODE_UNKNOWN.setNodeId((short) -1);
    }

    {
        this.definedNodes.add(this.MAPNODE_UNKNOWN);
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(short)
     */
    @Override
    public abstract MapNode getNode(short nodeId);

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#putNode(net.mosstest.scripting.MapNode)
     */
    @Override
    public abstract short putNode(MapNode node) throws MossWorldLoadException;

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#putNodeAlias(java.lang.String, java.lang.String)
     */
    @Override
    public abstract void putNodeAlias(String alias, String dst);

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(java.lang.String, boolean)
     */
    @Override
    public abstract MapNode getNode(String string, boolean isModified);

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(java.lang.String)
     */
    @Override
    public abstract MapNode getNode(String string);
    
    @Override
    public abstract List<MapNode> getNodeDefinitions ();
    
    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#init()
     */
    @Override
    public abstract void init();
    
}
