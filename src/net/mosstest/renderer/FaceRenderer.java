package net.mosstest.renderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class FaceRenderer {
	private static FloatBuffer vertices;
	private static FloatBuffer textures;
	private static FloatBuffer normals;
	private static IntBuffer indices;
	private static int vertexIndexCounter;
	
	public static void initialize () {
		vertices = getDirectFloatBuffer(950000);
        textures = getDirectFloatBuffer(950000);
        normals = getDirectFloatBuffer(950000);
		indices = getDirectIntBuffer(950000);
		vertexIndexCounter = 0;
	}
	
	public static boolean isFaceVisible (Face f, int[][][] nodes, byte i, byte j, byte k) {
		switch (f) {
		case FRONT:
			return (k - 1 < 0 || nodes[i][j][k - 1] == 0);
		case BACK:
			return (k + 1 >= nodes[i][j].length || nodes[i][j][k + 1] == 0);
		case TOP:
			return (j + 1 >= nodes[i].length || nodes[i][j + 1][k] == 0);
		case BOTTOM:
			return (j - 1 < 0 || nodes[i][j - 1][k] == 0);
		case LEFT:
			return (i + 1 <= nodes.length || nodes[i + 1][j][k] == 0);
		case RIGHT:
			return (i - 1 < 0 || nodes[i - 1][j][k] == 0);
		default:
			return false;
		}
	}
	
	public static void populateBuffers(Face f, float x, float y, float z, final float NODE_SIZE) {
		/*Vertices start at the top left corner and go clockwise around the face.*/
		if (f == Face.FRONT) {
			vertices.put(x).put(y).put(z);
			vertices.put(x + NODE_SIZE).put(y).put(z);
			vertices.put(x + NODE_SIZE).put(y).put(z - NODE_SIZE);
			vertices.put(x).put(y).put(z - NODE_SIZE);
			
		}
		else if (f == Face.TOP) {
			vertices.put(x).put(y + NODE_SIZE).put(z);
			vertices.put(x + NODE_SIZE).put(y + NODE_SIZE).put(z);
			vertices.put(x + NODE_SIZE).put(y).put(z);
			vertices.put(x).put(y).put(z);
		}
		else if (f == Face.BACK) {
			vertices.put(x + NODE_SIZE).put(y + NODE_SIZE).put(z);
			vertices.put(x).put(y + NODE_SIZE).put(z);
			vertices.put(x).put(y + NODE_SIZE).put(z - NODE_SIZE);
			vertices.put(x + NODE_SIZE).put(y + NODE_SIZE).put(z - NODE_SIZE);
		}
		else if (f == Face.BOTTOM) {
			vertices.put(x + NODE_SIZE).put(y + NODE_SIZE).put(z - NODE_SIZE);
			vertices.put(x).put(y + NODE_SIZE).put(z - NODE_SIZE);
			vertices.put(x).put(y).put(z - NODE_SIZE);
			vertices.put(x + NODE_SIZE).put(y).put(z - NODE_SIZE);
		}
		else if (f == Face.LEFT) {
			vertices.put(x).put(y + NODE_SIZE).put(z);
			vertices.put(x).put(y).put(z);
			vertices.put(x).put(y).put(z - NODE_SIZE);
			vertices.put(x).put(y + NODE_SIZE).put(z - NODE_SIZE);
		}
		else if (f == Face.RIGHT) {
			vertices.put(x + NODE_SIZE).put(y).put(z);
			vertices.put(x + NODE_SIZE).put(y + NODE_SIZE).put(z);
			vertices.put(x + NODE_SIZE).put(y + NODE_SIZE).put(z - NODE_SIZE);
			vertices.put(x + NODE_SIZE).put(y).put(z - NODE_SIZE);
		}
		indices.put(vertexIndexCounter + 0).put(vertexIndexCounter + 2).put(vertexIndexCounter + 1);
		indices.put(vertexIndexCounter + 0).put(vertexIndexCounter + 3).put(vertexIndexCounter + 2);
		
		
		
		textures.put(0).put(0);
		textures.put(0).put(1);
		textures.put(1).put(1);
		textures.put(1).put(0);
		for(int m=0; m<4; m++)
			normals.put(2).put(3).put(5);
		vertexIndexCounter += 4;
	}
	
	public enum Face {
		FRONT, TOP, BACK, BOTTOM, LEFT, RIGHT
	}
	
	private static FloatBuffer getDirectFloatBuffer (int size) {
		ByteBuffer temp = ByteBuffer.allocateDirect(size);
		return temp.asFloatBuffer();
	}	

	private static IntBuffer getDirectIntBuffer (int size) {
		ByteBuffer temp = ByteBuffer.allocateDirect(size);
		return temp.asIntBuffer();
	}
	
	public static FloatBuffer getVertices () {
		return vertices;
	}
	
	public static FloatBuffer getTextureCoordinates () {
		return textures;
	}
	
	
	public static FloatBuffer getNormals () {
		return normals;
	}
	
	
	public static IntBuffer getIndices () {
		return indices;
	}
}
