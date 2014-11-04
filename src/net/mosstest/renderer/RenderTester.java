package net.mosstest.renderer;

import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.Position;
import net.mosstest.servercore.MossRenderChunkEvent;
import net.mosstest.servercore.MossRenderEvent;

public class RenderTester {
	public static void preparatorChunkTest(RenderProcessor renderProcessor) {
		int[][] positions = {{ 0,  0,  0,  0},
							 { 1,  0,  0,  0},
							 { 0,  1,  0,  0},
							 { 1,  1,  0,  0},
							 {-1,  0,  0,  0},
							 { 0, -1,  0,  0},
							 {-1, -1,  0,  0}};
		
		for (int i = 0; i < positions.length; i++) {
			Position p = new Position(positions[i][0], positions[i][1],
									  positions[i][2], positions[i][2]);
			renderProcessor.getChunk(p);
		}
	}	
	
	public static void blankChunkTest (RenderProcessor renderProcessor) {
		Position p1 = new Position(0, 0, 0, 0);
		Position p2 = new Position(1, 1, 1, 0);
		
		int[][][] n1 = new int[16][16][16];
		int[][][] n2 = new int[16][16][16];
		for (int i = 0; i < n1.length; i++) {
			for (int j = 0; j < n1[i].length; j++) {
				for (int k = 0; k < n1[i][j].length; k++) {
					n1[i][j][k] = 1;
					n2[i][j][k] = 1;
				}
			}
		}
		
		MapChunk c1 = new MapChunk(p1, n1);
		MapChunk c2 = new MapChunk(p2, n2);
		
		MossRenderEvent e1 = new MossRenderChunkEvent(c1);
		MossRenderEvent e2 = new MossRenderChunkEvent(c2);
		
		renderProcessor.renderEventQueue.add(e1);
		renderProcessor.renderEventQueue.add(e2);
	}
}
