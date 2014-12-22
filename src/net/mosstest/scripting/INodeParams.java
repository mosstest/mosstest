package net.mosstest.scripting;

// TODO: Auto-generated Javadoc
/**
 * Interface to specify handlers to be called when various actions are taken by
 * players directed at a node. These handlers are called after the ones defined
 * in {@link MossScriptEnv} but before the default handlers.
 * 
 * @author rarkenin
 * 
 */
public interface INodeParams {

	public static final double GRAVITY_EARTH = -9.81;
	/**
	 * Function that is called when a node is punched.
	 * 
	 * @param player
	 *            The player punching the node.
	 * @param tool
	 *            The tool being used to punch the node.
	 * @param target
	 *            The position of the targeted node.
	 * @param punchedFace
	 *            The face being punched.
	 */
	public abstract void onPunch(Player player, MossItem tool,
			NodePosition target, Face punchedFace);

	/**
	 * Determines if a fall should continue.
	 * 
	 * @param player
	 *            The player.
	 * @param height
	 *            The height.
	 * @return A boolean representing whether the fall should continue or be
	 *         marked as a fall.
	 */
	public abstract boolean shouldContinueFall(Player player, double height);

	/**
	 * Called when a node is fully dug.
	 * 
	 * @param player
	 *            The player digging the node.
	 * @param tool
	 *            The tool used to dig the node.
	 * @param target
	 *            The position of the node that has been dug.
	 * @param dugFace
	 *            The face on which the the tool was pointed when digging was
	 *            complete. Note that if a dig begins on one face but the player
	 *            moves the aim to another face the onPunch handler may receive
	 *            a different face than this handler.
	 */
	public abstract void onDig(Player player, MossItem tool,
			NodePosition target, Face dugFace);

	/**
	 * Called when a node is placed directly above, below, left, right, in front
	 * of, or behind this node.
	 * 
	 * @param player
	 *            The player placing the node.
	 * @param target
	 *            the target
	 * @param placed
	 *            the placed
	 */
	public abstract void onPlaceNextTo(Player player, NodePosition target,
			NodePosition placed);

	/**
	 * Called when a right-click is done on a node. Note that this is called
	 * even if no node is placed, such as if the player is not holding anything,
	 * or is holding a tool.
	 * 
	 * @param player
	 *            Player performing the action.
	 * @param tool
	 *            Tool held by user in active slot during this action.
	 * @param target
	 *            Position of targeted node
	 * @param clickedFace
	 *            Face which was right-clicked.
	 */
	public abstract void onRightClick(Player player, MossItem tool,
			NodePosition target, Face clickedFace);

	/**
	 * Called when a player steps on a node either from jumping, falling, or
	 * walking forward.
	 * 
	 * @param player
	 *            Player that steps on this node.
	 * @param pos
	 *            Position of node that is stepped on.
	 * @return Currently unused.
	 */
	public abstract boolean onStepOn(Player player, NodePosition pos);

	/**
	 * Calculates the height a player will bounce when landing on this node.
	 * 
	 * @param player
	 *            the player
	 * @param fallheight
	 *            The height from which the player has fallen. This may be 0.
	 * @return A double value representing the height the player is to bounce. A
	 *         negative height represents sinking.
	 */
	public abstract double calcBounceHeight(Player player, double fallheight);

	/**
	 * Calculates at which rate a player will sink in if they are holding the
	 * key to actively descend.
	 * 
	 * @param player
	 *            The player to calculate sink for.
	 * @param fallheight
	 *            The height from which they have fallen. 0 if the player walks
	 *            onto the node without jumping or falling(feet at surface), or
	 *            negative if they have already sunk in partially.
	 * @return The rate for the player to rise or sink, positive for rising
	 */
	public abstract double calcSinkActive(Player player, double fallheight);

	/**
	 * Calculates at which rate a player will sink in if they are holding
	 * neither the jump nor descend key
	 * 
	 * @param player
	 *            The player to calculate sink for.
	 * @param fallheight
	 *            The height from which they have fallen. 0 if the player walks
	 *            onto the node without jumping or falling(feet at surface), or
	 *            negative if they have already sunk in partially.
	 * @return The rate for the player to rise or sink, positive for rising
	 */
	public abstract double calcSinkPassive(Player player, double fallheight);

	/**
	 * Calculates the rate at which a player will rise out of this node if they
	 * have sunk in and are pressing the jump key. If the player is in multiple
	 * node the lowermost node at the player's horizontal center is considered.
	 * 
	 * @param player
	 *            The player to perform the calculation for.
	 * @param sinkheight
	 *            The depth to which the player has sunk. 0 is the surface at
	 *            the player's feet (and corresponds to jumping off a solid
	 *            node's surface). 1 corresponds to the player's feet being at
	 *            the bottom of the node, and 2 or more is if the player is
	 *            submerged in multiple nodes.
	 * @return The rate at which the player should rise when they hold the key
	 *         corresponding to jumping, in nodes per second.
	 */
	public abstract double calcSinkJump(Player player, double sinkheight);

	/**
	 * Calculates the speed at which a player will move across this node when
	 * walking.
	 * 
	 * @param player
	 *            The player for which the walk rate is being calculated.
	 * @return The rate in nodes/second at which the player should walk.
	 */
	public abstract double calcWalkSpeed(Player player);

	/**
	 * Calculates the speed at which a player will move across this node when
	 * sprinting.
	 * 
	 * @param player
	 *            The player for which the sprint rate is being calculated.
	 * @return The rate in nodes/second at which the player should sprint.
	 */
	public abstract double calcSprintSpeed(Player player);

	/**
	 * Calculates the damage a player will take when falling onto this node.
	 * 
	 * @param player
	 *            The player falling onto this node.
	 * @param height
	 *            The fall height.
	 * @return The damage to be given to the player. A player has a default
	 *         health of 64 units.
	 */
	public abstract double calcFallDamage(Player player, double height);

	/**
	 * Calculate whether a ray aimed from the crosshairs should stop at this
	 * node.
	 * 
	 * @param player
	 *            The player.
	 * @param distance
	 *            The distance from the camera to the node.
	 * @return Whether the aim ray should hit this node.
	 */
	public abstract boolean shouldHitAimRay(Player player, double distance);

	/**
	 * Calculate a dig property, as a double: 0 for inability to dig, 1 for
	 * digging the node in one second (where dig time is the 1/return value).
	 * Negative values signify instant digging.
	 * 
	 * @param key
	 *            The interact type
	 * @param interactStrength
	 *            How strong the tool is (where 1 corresponds to the wooden
	 *            pick/hand).
	 * @return The reciprocal of the dig time, 0 for non-diggability, or
	 *         negative values for instant digging.
	 */
	public abstract double calcInteractProperties(MossTool.InteractType key,
			double interactStrength);

}
