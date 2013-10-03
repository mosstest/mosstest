package net.mosstest.servercore;

public class MossRenderChunkEvent extends MossRenderEvent {
	private MapChunk myChunk;
	private Position myPos;
	public MossRenderChunkEvent (MapChunk ch) {
		myChunk = ch;
		myPos = ch.pos;
	}
	
	public int getX () {
		return myPos.x;
	}
	public int getY() {
		return myPos.y;
	}
	public int getZ() {
		return myPos.z;
	}
	
	public Position getPos () {
		return myPos;
	}
	
	public int getNodeId (byte x, byte y, byte z) {
		return myChunk.getNodeId(x, y, z);
	}
}
 