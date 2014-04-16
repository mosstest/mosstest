package net.mosstest.extensions.itempipes;

import net.mosstest.scripting.MossItem;

public class PipedStack extends MossItem.Stack{
    private int ttl;

    public int getTtl() {
        return ttl;
    }

    /**

     * Instantiates a new stack.
     *
     * @param item   the item
     * @param amount the amount
     */
    public PipedStack(MossItem item, double amount, int ttl) {
        super(item, amount);
        this.ttl = ttl;
    }
}
