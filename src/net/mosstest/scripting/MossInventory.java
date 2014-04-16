package net.mosstest.scripting;

import net.mosstest.servercore.AbstractByteArrayStorable;
import net.mosstest.servercore.ItemManager;
import org.apache.log4j.Logger;

import java.io.*;

// TODO: Auto-generated Javadoc

/**
 * The Class MossInventory.
 */
public class MossInventory extends AbstractByteArrayStorable<ItemManager> {

    private ItemManager im;

    public void loadBytes(byte[] buf){

    }

    /**
     * The max stack size.
     */
    private int maxStackSize;

    /**
     * The stacks.
     */
    private MossItem.Stack[][] stacks;
    private static final Logger logger = Logger.getLogger(MossInventory.class);

    /**
     * Instantiates a new moss inventory.
     *
     * @param maxStackSize the max stack size
     * @param rows         the rows
     * @param cols         the cols
     */
    public MossInventory(int maxStackSize, int rows, int cols) {
        this.maxStackSize = maxStackSize;
        this.stacks = new MossItem.Stack[rows][cols];
    }

    /**
     * To bytes.
     *
     * @return the byte[] array that can be deserialized to this inventory
     */
    public final byte[] toBytes() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeInt(this.maxStackSize);

        dos.writeInt(this.stacks.length);
        dos.writeInt(this.stacks[0].length);
        for (MossItem.Stack[] sa : this.stacks) {
            for (MossItem.Stack s : sa) {
                dos.writeUTF(s.item.getInternalName());
                dos.writeDouble(s.amount);
            }
        }
        dos.flush();
        bos.flush();
        } catch (IOException e) {
            // This should never happen in real life
            logger.fatal("IOException serializing an inventory. This should not happen, if it does a bug should be filed");
        }
        return bos.toByteArray();
    }


    public MossInventory(byte[] buf, ItemManager im) throws IOException {
        loadBytes(buf, im);
    }

    public void loadBytes(byte[] buf, ItemManager im) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
            try (DataInputStream dis = new DataInputStream(bais)) {
                this.maxStackSize = dis.readInt();
                int rows = dis.readInt();
                int cols = dis.readInt();
                this.stacks = new MossItem.Stack[rows][cols];
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        MossItem itm = im.getItem(dis.readUTF());
                        double amount = dis.readDouble();
                        this.stacks[i][j] = new MossItem.Stack(itm, amount);
                    }
                }
            }
        }
    }

    @Override
    protected void setManager(ItemManager manager) {
        this.im = manager;
    }


    public synchronized double addItem(MossItem.Stack stack) {
        return addItem(stack, stack.item.stackMode);
    }

    public synchronized double addItem(MossItem.Stack stack, MossItem.StackMode stackMode) {
        double added = 0;
        for (int row = 0; row < this.stacks.length; row++) {
            for (int col = 0; col < this.stacks[row].length; col++) {
                if (this.stacks[row][col] == null) {
                    double addedThisRound = MossItem.Stack.getMaxSize(stackMode, stack.amount - added, this.maxStackSize);
                    added += addedThisRound;
                    this.stacks[row][col] = new MossItem.Stack(stack.item,
                            addedThisRound);
                } else if (this.stacks[row][col].item.equals(stack.item)) {
                    double addedThisRound = this.stacks[row][col].getAddable(stackMode, stack.amount - added, this.maxStackSize);
                    added += addedThisRound;
                    this.stacks[row][col].amount += addedThisRound;
                }
                if (added >= stack.amount)
                    return added;
            }
        }
        return added;

    }

    public synchronized MossItem.Stack removeItem(MossItem.Stack stack) {
        return removeItem(stack, stack.item.stackMode);
    }

    public synchronized MossItem.Stack removeItem(MossItem.Stack stack, MossItem.StackMode stackMode) {
        double removed = 0;
        for (int row = 0; row < this.stacks.length; row++)
            for (int col = 0; col < this.stacks[row].length; col++) {
                if (this.stacks[row][col].item.equals(stack.item)) {
                    double removedThisRound = this.stacks[row][col].getRemovable(stackMode, stack.amount - removed);
                    removed += removedThisRound;
                    if (this.stacks[row][col].amount != removedThisRound)
                        this.stacks[row][col].amount -= removedThisRound;
                    else this.stacks[row][col] = null;

                }
                if (removed >= stack.amount)
                    return new MossItem.Stack(stack.item, removed);
            }
        return new MossItem.Stack(stack.item, removed);
    }

    // TODO remove item

    public enum RemovalMode {
        REMOVE_NET_AMOUNT,
        REMOVE_TOOL_LVL,
    }

}
