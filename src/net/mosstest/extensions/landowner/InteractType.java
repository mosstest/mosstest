package net.mosstest.extensions.landowner;

public enum InteractType {
    /**
     * Used for a player (or node/entity on a player's behalf) digging nodes.
     */
    TYPE_DIG,
    /**
     * Used for a player (or node/entity on a player's behalf) placing nodes.
     */
    TYPE_PLACE,
    /**
     * Used for a player (or node/entity on a player's behalf) taking items from a chest/furnace/etc.
     */
    TYPE_INVENTORY_PUT,
    /**
     * Used for a player (or node/entity on a player's behalf) putting items into a chest/furnace/etc.
     */
    TYPE_INVENTORY_TAKE,
    /**
     * Used for a player (or node/entity on a player's behalf) opening and closing doors, access grates, etc.
     */
    TYPE_DOOR_ACCESS,
    /**
     * Used for a player (or node/entity on a player's behalf) editing signs and the like.
     */
    TYPE_TEXT,
    /**
     * Used for a player (or node/entity on a player's behalf) performing miscellaneous actions such as turning on/off circuits.
     */
    TYPE_MISC
}
