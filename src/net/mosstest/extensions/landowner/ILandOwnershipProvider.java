package net.mosstest.extensions.landowner;

import net.mosstest.scripting.NodePosition;
import net.mosstest.scripting.Player;

/**
 * Interface for providers of landclaim control.
 */
public interface ILandOwnershipProvider {

    /**
     * Returns a claim for a given node position.
     *
     * @param pos The node position.
     * @return An implementation of ILandClaim that describes the claim at this location, or null if no claim exists.
     * @throws  LandClaimException If an error prevents the lookup from completing.
     */
    public ILandClaim getClaim(NodePosition pos) throws LandClaimException;

    /**
     * Determines if a player may interact with a given claim's contents.
     *
     * @param pos  The node position that is being interacted with.
     * @param p    The player that is interacting, or if an entity or node is interacting, the player on whose behalf the interaction is.
     * @param type The type of interaction.
     * @return True if the interaction should be allowed, false if not.
     * @throws  LandClaimException If an error prevents the check from completing.
     */
    public boolean mayInteract(NodePosition pos, Player p, InteractType type) throws LandClaimException;

    /**
     * Creates a landclaim at pos, stores it to a claim database (if applicable), and returns an ILandClaim object representing it.
     *
     * @param pos    The node position that the claim should contain.
     * @param owner  The claim's owner.
     * @param shared A set of players with whom the claim is shared.
     * @return The landclaim that was created.
     * @throws LandClaimException If an issue such as an already existing claim, a lack of player privileges, or another condition prevents the claim from being created.
     */
    public ILandClaim addClaim(NodePosition pos, Player owner, Player... shared) throws LandClaimException;

}
