package net.mosstest.servercore;

import net.mosstest.scripting.Player;
import net.mosstest.servercore.serialization.IByteArrayWritable;

public class InventoryManager {
    public class PlayerKey implements IByteArrayWritable {
        private final Player player;

        private final String inventory;

        public PlayerKey(Player player, String inventory) {
            this.player = player;
            this.inventory = inventory;
        }

        @Override
        public byte[] toBytes() {
            return new byte[0];
        }




    }
}
