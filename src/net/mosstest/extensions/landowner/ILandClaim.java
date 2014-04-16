package net.mosstest.extensions.landowner;

import net.mosstest.scripting.NodePosition;
import net.mosstest.scripting.Player;

import java.util.List;

/**
 * Represents a land claim.
 */
public interface ILandClaim {
    /**
     * Gets the owner of the claim.
     */
    public Player getOwner() throws LandClaimException;

    /**
     * Gets a list of players that this claim is shared with. A JavaScript array is acceptable for the return value.
     */
    public List<Player> getShared() throws LandClaimException;

    /**
     * Determines if a player may interact with this claim's contents.
     *
     * @param p    The player that is interacting, or if an entity or node is interacting, the player on whose behalf the interaction is.
     * @param type The type of interaction.
     * @return True if the interaction is allowed, and false if not.
     */
    public boolean mayInteract(Player p, InteractType type) throws LandClaimException;

    /**
     * Shares this claim with a player
     * @param p The player to share with.
     * @throws LandClaimException
     */
    public void shareArea(Player p) throws LandClaimException;

    /**
     * Removes a player from the list of players this claim is shared with.
     * @param p The player to remove.
     * @throws LandClaimException
     */
    public void unshareArea(Player p) throws LandClaimException;

    /**
     * Gets a mesh that represents this area.
     * @return A string representing a filename of a mesh object in the scripts directory.
     * @throws LandClaimException
     */
    public String getAreaMesh() throws LandClaimException;

    /**
     * Gets the place to center the mesh returned by getAreaMesh().
     * @return The location of the mesh's origin.
     * @throws LandClaimException
     */
    public NodePosition getMeshCenter() throws LandClaimException;
}
