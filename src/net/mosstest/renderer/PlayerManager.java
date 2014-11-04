package net.mosstest.renderer;

import net.mosstest.scripting.Player;

public class PlayerManager {
	public static Player makePlayer () {
		Player player = new Player ("Test Guy");
		player.setPositionOffsets (0,5,0);
		player.setChunkPosition(0,0,0);
		return player;
	}
}
