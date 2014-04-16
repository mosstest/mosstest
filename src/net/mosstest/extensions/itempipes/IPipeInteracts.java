package net.mosstest.extensions.itempipes;

import net.mosstest.scripting.Face;
import net.mosstest.scripting.MossItem;

public interface IPipeInteracts {
    /**
     * Returns true if this face should make a connection to other itempipe-compliant nodes.
     * This is used both for cosmetic appearance of pipes that connect, and for routing.
     *
     * @param f The face to test.
     * @return A boolean representing whether the connection should be made.
     */
    public abstract boolean connects(Face f);

    /**
     * Called when this node is to receive a stack.
     * @param stack The item stack to receive.
     * @return A double representing how much of the stack had been taken.
     */
    public abstract double offer(PipedStack stack);

}
