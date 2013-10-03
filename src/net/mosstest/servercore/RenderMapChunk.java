package net.mosstest.servercore;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

public class RenderMapChunk {
	private RenderNode[][][] myNodes = new RenderNode[16][16][16];
	private int x, y, z;
	/**
	 * 
	 * @param holdThese must be length [16][16][16]
	 */
	public RenderMapChunk (RenderNode[][][] holdThese, int x, int y, int z) {
		for(int i = 0; i < holdThese.length; i++) {
			for(int j = 0; j < holdThese[i].length; j++) {
				for(int k = 0; k < holdThese[i][j].length; k++) {
					myNodes[i][j][k] = holdThese[i][j][k];
				}
			}
		}
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void addNode (MapNode def, Material mat, float size, int i, int j, int k) {
		Vector3f loc = new Vector3f(x+i*size, y-j*size-40, z+k*size);
		myNodes[i][j][k] = new RenderNode(mat, loc, size, def);
	}
	
	public Vector3f getNodeLoc (int i, int j, int k, float size) {
		Vector3f loc = new Vector3f(x*i*size, y-j*size-40, z+k*size);
		return loc;
	}
}
