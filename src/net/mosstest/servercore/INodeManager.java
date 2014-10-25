package net.mosstest.servercore;

import java.util.List;

import net.mosstest.scripting.MapNode;

// TODO: Auto-generated Javadoc
/**
 * The Interface INodeManager.
 */
public interface INodeManager {

	/**
	 * Gets the node.
	 *
	 * @param nodeId the node id
	 * @return the node
	 */
	public abstract MapNode getNode(short nodeId);

	/**
	 * Put node.
	 *
	 * @param node the node
	 * @return the short
	 * @throws MossWorldLoadException the moss world load exception
	 */
	public abstract short putNode(MapNode node) throws MossWorldLoadException;

	/**
	 * Put node alias.
	 *
	 * @param alias the alias
	 * @param dst the dst
	 */
	public abstract void putNodeAlias(String alias, String dst);

	/**
	 * Gets the node.
	 *
	 * @param string the string
	 * @param isModified the is modified
	 * @return the node
	 */
	public abstract MapNode getNode(String string, boolean isModified);

	/**
	 * Gets the node.
	 *
	 * @param string the string
	 * @return the node
	 */
	public abstract MapNode getNode(String string);

	/**
	 * Inits the.
	 */
	public abstract void init();

    boolean containsNode(MapNode node);
    
    public abstract List<MapNode> getNodeDefinitions ();
}