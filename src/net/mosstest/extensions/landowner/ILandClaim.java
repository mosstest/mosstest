package net.mosstest.extensions.landowner;

import net.mosstest.scripting.Player;

import java.util.List;

/**
 * Represents a land claim.
 */
public interface ILandClaim {
    /**
     * Gets the owner of the claim.
     */
    public Player getOwner();

    /**
     * Gets a list of players that this claim is shared with. A JavaScript array is acceptable for the return value.
     */
    public List<Player> getShared();

    /**
     * Determines if a player may interact with this claim's contents.
     *
     * @param p    The player that is interacting, or if an entity or node is interacting, the player on whose behalf the interaction is.
     * @param type The type of interaction.
     * @return True if the interaction is allowed, and false if not.
     */
    public boolean mayInteract(Player p, InteractType type);

}
