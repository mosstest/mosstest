package net.mosstest.servercore;

import net.mosstest.renderer.RenderProcessor;
import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.Position;

// TODO: Auto-generated Javadoc
/**
 * The Interface IRenderPreparator.
 */
public interface IRenderPreparator extends PlayerCommunicator{

	/**
	 * Sets the render processor.
	 *
	 * @param rend the new render processor
	 */
	public abstract void setRenderProcessor(RenderProcessor rend);

	/**
	 * Request chunk.
	 *
	 * @param pos the pos
	 * @return the map chunk
	 * @throws MapGeneratorException the map generator exception
	 * @throws InterruptedException the interrupted exception
	 */
	public abstract MapChunk requestChunk(Position pos)
			throws MapGeneratorException, InterruptedException;

	/**
	 * Recv outstanding chunk.
	 *
	 * @param pos the pos
	 * @param chk the chk
	 */
	public abstract void recvOutstandingChunk(Position pos, MapChunk chk);

	/**
	 * Shutdown.
	 */
	public abstract void shutdown();

	/**
	 * Start.
	 */
	public abstract void start();

	/**
	 * Sets the node manager.
	 *
	 * @param nm the new node manager
	 */
	public abstract void setNodeManager(INodeManager nm);
}