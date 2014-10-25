package net.mosstest.tests;

import java.util.ArrayList;
import java.util.Collections;

import net.mosstest.scripting.DefaultNodeParams;
import net.mosstest.scripting.MapNode;
import net.mosstest.servercore.INodeManager;
import net.mosstest.servercore.MossWorldLoadException;

// TODO: Auto-generated Javadoc

/**
 * The Class MockNodeManager.
 */
public class MockNodeManager implements INodeManager {

    /**
     * The Constant MOCK_SOLID_MAPNODE.
     */
    private static final MapNode MOCK_SOLID_MAPNODE = new MapNode(
            new DefaultNodeParams(), "mock.png", "!mock:mock", "Mock node", 1); //NON-NLS NON-NLS NON-NLS

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(short)
     */
    @Override
    public MapNode getNode(short nodeId) {
        return MOCK_SOLID_MAPNODE;
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#putNode(net.mosstest.scripting.MapNode)
     */
    @Override
    public short putNode(MapNode node) throws MossWorldLoadException {
        // TODO Auto-generated method stub
        return 1337;
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#putNodeAlias(java.lang.String, java.lang.String)
     */
    @Override
    public void putNodeAlias(String alias, String dst) {
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(java.lang.String, boolean)
     */
    @Override
    public MapNode getNode(String string, boolean isModified) {
        return MOCK_SOLID_MAPNODE;
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#getNode(java.lang.String)
     */
    @Override
    public MapNode getNode(String string) {
        return MOCK_SOLID_MAPNODE;
    }

    /* (non-Javadoc)
     * @see net.mosstest.servercore.INodeManager#init()
     */
    @Override
    public void init() {
    }

    @Override
    public boolean containsNode(MapNode node) {
        //for testing reasons
        return true;
    }

	@Override
	public ArrayList<MapNode> getNodeDefinitions() {
		return null;
	}

}
