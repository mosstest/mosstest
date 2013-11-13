package net.mosstest.servercore;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import jme3tools.optimize.GeometryBatchFactory;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import com.jme3.math.ColorRGBA;

import java.util.Arrays;

import net.mosstest.scripting.INodeParams;
import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.MapNode;
import net.mosstest.scripting.Position;

public class RenderProcessor extends SimpleApplication {

	private final float speed = 3f;
	private final float playerHeight = 25;
	private final float blockSize = 10f;
	private final float rotationSpeed = 1f;
	private float[] locChanges = { 0, 0, 0 };
	private double lastTime;
	private boolean invertY = false;
	
	private Vector3f initialUpVec;
	private Node worldNode;
	private SpotLight spot = new SpotLight();
	private DirectionalLight sun = new DirectionalLight();
	private HashMap<Position, RenderMapChunk> allChunks = new HashMap<Position, RenderMapChunk>();

	public INodeManager nManager;
	public IRenderPreparator rPreparator;
	public ArrayBlockingQueue<MossRenderEvent> renderEventQueue = new ArrayBlockingQueue<>(
			24000, false);

	public static RenderProcessor init(INodeManager manager, IRenderPreparator prep) {
		RenderProcessor app = new RenderProcessor();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(800, 600);
		settings.setSamples(2);
		app.setSettings(settings);
		app.setShowSettings(false);
		app.initNodeThings(manager, prep);
		app.start();
		return app;
	}

	private void initNodeThings(INodeManager manager, IRenderPreparator prep) {
		nManager = manager;
		rPreparator = prep;
	}

	@Override
	public void simpleInitApp() {
		lastTime = 0;
		worldNode = new Node("world");
		rootNode.attachChild(worldNode);
		spot.setSpotRange(150f);
		spot.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD);
		spot.setSpotOuterAngle(35f * FastMath.DEG_TO_RAD);
		spot.setColor(ColorRGBA.White.mult(3f));
		spot.setPosition(cam.getLocation());
		spot.setDirection(cam.getDirection());

		sun.setColor(ColorRGBA.White);
		sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
		rootNode.addLight(sun);
		rootNode.addLight(spot);
		testChunkEvents();
		//testLoadSurroundingChunks();
		flyCam.setEnabled(false);
		initialUpVec = cam.getUp().clone();
		initKeyBindings();
	}

	@Override
	/**
	 * Constant running loop that's built into SimpleApplication.
	 * Looks for new events in the renderEventQueue, moves if necessary.
	 */
	public void simpleUpdate(float tpf) {
		if (lastTime + 10 < System.currentTimeMillis()) {
			moveWorld(locChanges[0], locChanges[1], locChanges[2]);
			lastTime = System.currentTimeMillis();
		}

		inputManager.setCursorVisible(false);
		MossRenderEvent myEvent = renderEventQueue.poll();
		if (myEvent instanceof MossRenderStopEvent) {
			System.out.println("Thread shutting down");
		} else if (myEvent instanceof MossRenderChunkEvent) {
			renderChunk(((MossRenderChunkEvent) myEvent).getChk(),
					((MossRenderChunkEvent) myEvent).getPos());
		}/*
		 * else if (myEvent instanceof MossNodeAddEvent) { int x =
		 * ((MossNodeAddEvent) myEvent).getX(); int y = ((MossNodeAddEvent)
		 * myEvent).getY(); int z = ((MossNodeAddEvent) myEvent).getZ();
		 * Position pos = ((MossNodeAddEvent) myEvent).getPosition();
		 * 
		 * short defRef = ((MossNodeAddEvent) myEvent).getDef(); MapNode def =
		 * /*NodeManager.getNode(defRef)null; Material mat =
		 * getMaterial(defRef); allChunks.get(pos).addNode(def, mat, blockSize,
		 * x, y, z); Vector3f loc = allChunks.get(pos).getNodeLoc(x, y, z,
		 * blockSize); RenderNode geom = new RenderNode (mat, loc, blockSize,
		 * def); worldNode.attachChild(geom);
		 * System.out.println("ADDED A NODE"); } else if (myEvent instanceof
		 * MossRenderAddAssetPath) { String path = ((MossRenderAddAssetPath)
		 * myEvent).getPath(); assetManager.registerLocator(path,
		 * com.jme3.asset.plugins.FileLocator.class); }
		 */
	}
	
	public void getChunk (Position pos) {
		MapChunk maybe = null;
		try {maybe = rPreparator.requestChunk(pos);} 
		catch (MapGeneratorException e) {e.printStackTrace();} 
		catch (InterruptedException e) {e.printStackTrace();}
		if (maybe != null) {renderChunk(maybe, pos);}
	}
	
	public void renderChunk(MapChunk chk, Position pos) {
		//Unnecessary?
		if (chk == null) {
			return;
		}
		double offset = 16 * blockSize;
		int ixOffset = 0;
		Mesh bigMesh = new Mesh ();
		FloatBuffer tcoords = FloatBuffer.allocate(500000);
		FloatBuffer vertices = FloatBuffer.allocate(500000);
		IntBuffer indices = IntBuffer.allocate(500000);
		//RenderNode[][][] nodesInChunk = new RenderNode[16][16][16];

		for (byte i = 0; i < 5; i++) {
			for (byte j = 0; j < 5; j++) {
				for (byte k = 0; k < 5; k++) {
					int nVal = chk.getNodeId(i, j, k);
					//MapNode node = nManager.getNode((short) nVal);
					//Material mat = getMaterial((short) nVal);
					if (nVal == 0) {System.out.println("WARRRNINGINIGNINGINGINN");return;}
					
					else {
						
						float x = (float) ((pos.x + (32 * blockSize * pos.x)) - offset + (i * 3 * blockSize));
						float y = (float) ((pos.y - playerHeight) - (j * 3 * blockSize));
						float z = (float) ((pos.z + (32 * blockSize * pos.z)) - offset  + (k * 3 * blockSize));
						
						addVertex(x, y, z, vertices); //Front face
						addVertex(x, y - 2*blockSize, z, vertices);
						addVertex(x + 2*blockSize, y, z, vertices);
						addVertex(x + 2*blockSize, y - 2*blockSize, z, vertices); //Top Face
						addVertex(x, y, z + 2*blockSize, vertices);
						addVertex(x + 2*blockSize, y, z + 2*blockSize, vertices);
						addVertex(x + 2*blockSize, y - 2*blockSize, z + 2*blockSize, vertices); //right face
						addVertex(x, y - 2*blockSize, z + 2*blockSize, vertices); //left face

						addTriIX(ixOffset + 3, ixOffset + 1, ixOffset + 0, indices);//front
						addTriIX(ixOffset + 3, ixOffset + 0, ixOffset + 2, indices);
						addTriIX(ixOffset + 4, ixOffset + 2, ixOffset + 0, indices);//top
						addTriIX(ixOffset + 4, ixOffset + 5, ixOffset + 2, indices);
						addTriIX(ixOffset + 3, ixOffset + 2, ixOffset + 6, indices);//right
						addTriIX(ixOffset + 2, ixOffset + 5, ixOffset + 6, indices);
						addTriIX(ixOffset + 0, ixOffset + 1, ixOffset + 7, indices);//left
						addTriIX(ixOffset + 0, ixOffset + 7, ixOffset + 4, indices);
						addTriIX(ixOffset + 4, ixOffset + 6, ixOffset + 5, indices);//back
						addTriIX(ixOffset + 4, ixOffset + 7, ixOffset + 6, indices);
						addTriIX(ixOffset + 1, ixOffset + 6, ixOffset + 7, indices);//bottom
						addTriIX(ixOffset + 1, ixOffset + 3, ixOffset + 6, indices);
						
						/*for(int cv=0; cv<8; cv++) {
							tcoords.put(1); tcoords.put(0);
							tcoords.put(1); tcoords.put(1);
						}*/
						
						
						
						//RenderNode geom = new RenderNode(mat, loc, blockSize, NodeManager.getNode((short)nVal)null);
						//nodesInChunk[i][j][k] = geom;
						ixOffset += 8;
						System.out.println(ixOffset);
					}

				}
			}
		}
		Material mat = getMaterial((short) 1);
		bigMesh.setBuffer(Type.Position, 3, vertices);
		//bigMesh.setBuffer(Type.TexCoord, 2, tcoords);
		bigMesh.setBuffer(Type.Index, 3, indices);
		bigMesh.updateBound();
		Geometry geom = new Geometry("chunkMesh", bigMesh);
		geom.setMaterial(mat);
		worldNode.attachChild(geom);
		/*RenderMapChunk thisChunk = new RenderMapChunk(nodesInChunk, x, y, z);
		allChunks.put(pos, thisChunk);*/
	}
	private void addVertex (float x, float y, float z, FloatBuffer buffer) {
		buffer.put(x);
		buffer.put(y);
		buffer.put(z);
	}
	
	private void addTriIX (int a, int b, int c, IntBuffer buffer) {
		buffer.put(a);
		buffer.put(b);
		buffer.put(c);
	}
	
	private void testLoadSurroundingChunks() {
		Position p1 = new Position(0, 0, 0, 0);
		Position p2 = new Position(1, 0, 0, 0);
		Position p3 = new Position(0, 0, 1, 0);
		Position p4 = new Position(1, 0, 1, 0);
		// Position p5 = new Position(-1,0,0,0);
		// Position p6 = new Position(0,0,-1,0);
		// Position p7 = new Position(-1,0,-1,0);

		getChunk(p1);
		getChunk(p2);
		getChunk(p3);
		getChunk(p4);
		// getChunk(p5);
		// getChunk(p6);
		// getChunk(p7);
	}

	private void testChunkEvents() {
		Position pos = new Position(0, 0, 0, 0);
		Position pos2 = new Position(1, 0, 1, 0);
		boolean[][][] testModified = new boolean[16][16][16];
		boolean[][][] tM2 = new boolean[16][16][16];
		for (boolean[][] l1 : testModified) {
			for (boolean[] l2 : l1) {
				Arrays.fill(l2, false);
			}
		}
		for (boolean[][] l1 : tM2) {
			for (boolean[] l2 : l1) {
				Arrays.fill(l2, false);
			}
		}

		int[][][] tN2 = new int[16][16][16];
		int[][][] testNodes = new int[16][16][16];
		for (int[][] l1 : testNodes) {
			for (int[] l2 : l1) {
				Arrays.fill(l2, 1);
			}
		}
		for (int[][] l1 : tN2) {
			for (int[] l2 : l1) {
				Arrays.fill(l2, 1);
			}
		}

		MapChunk ch = new MapChunk(pos, testNodes, testModified);
		//MapChunk ch2 = new MapChunk(pos2, tN2, tM2);
		renderChunk(ch, pos);
		//renderChunk(ch2, pos2);
		GeometryBatchFactory.optimize(worldNode);
	}
	 
	public Material getMaterial(short nVal) {
		Material mat = null;
		switch (nVal) {
		case 1:
			/*mat = new Material(assetManager,
					"Common/MatDefs/Light/Lighting.j3md");*/
			mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
			mat.setColor("Color", ColorRGBA.Green);
			//mat.setBoolean("UseMaterialColors", true);
			//mat.setColor("Ambient", ColorRGBA.Green);
			//mat.setColor("Diffuse", ColorRGBA.Green);
		}
		return mat;
	}
	private void moveWorld(float cx, float cy, float cz) {

		Vector2f transVector = new Vector2f(cam.getDirection().x,
				cam.getDirection().z);

		worldNode.setLocalTranslation(worldNode
				.getLocalTranslation()
				.addLocal(
						new Vector3f(-cz * transVector.x, 0f, -cz
								* transVector.y))
				.addLocal(-cx * transVector.y, 0f, cx * transVector.x)
				.addLocal(0f, -cy, 0f));
	}

	/**
	 * Runs when the mouse moves to look around.
	 */
	private void rotateCamera(float value, Vector3f axis) {

		Matrix3f mat = new Matrix3f();
		mat.fromAngleNormalAxis(rotationSpeed * value, axis);

		Vector3f up = cam.getUp();
		Vector3f left = cam.getLeft();
		Vector3f dir = cam.getDirection();

		mat.mult(up, up);
		mat.mult(left, left);
		mat.mult(dir, dir);

		Quaternion q = new Quaternion();
		q.fromAxes(left, up, dir);
		q.normalizeLocal();

		cam.setAxes(q);

		spot.setDirection(cam.getDirection());
	}

	/**
	 * Set up key bindings and event listeners for key bindings
	 */
	private void initKeyBindings() {
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_LSHIFT));
		inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));

		inputManager.addMapping("CAM_Left", new MouseAxisTrigger(
				MouseInput.AXIS_X, true), new KeyTrigger(KeyInput.KEY_LEFT));

		inputManager.addMapping("CAM_Right", new MouseAxisTrigger(
				MouseInput.AXIS_X, false), new KeyTrigger(KeyInput.KEY_RIGHT));

		inputManager.addMapping("CAM_Up", new MouseAxisTrigger(
				MouseInput.AXIS_Y, false), new KeyTrigger(KeyInput.KEY_UP));

		inputManager.addMapping("CAM_Down", new MouseAxisTrigger(
				MouseInput.AXIS_Y, true), new KeyTrigger(KeyInput.KEY_DOWN));

		inputManager.addListener(actionListener, "Jump");
		inputManager.addListener(actionListener, "Down");
		inputManager.addListener(actionListener, "Left");
		inputManager.addListener(actionListener, "Right");
		inputManager.addListener(actionListener, "Forward");
		inputManager.addListener(actionListener, "Back");
		inputManager.addListener(analogListener, "CAM_Left");
		inputManager.addListener(analogListener, "CAM_Right");
		inputManager.addListener(analogListener, "CAM_Up");
		inputManager.addListener(analogListener, "CAM_Down");
	}

	private AnalogListener analogListener = new AnalogListener() {

		public void onAnalog(String name, float value, float tpf) {
			if (name.equals("CAM_Left")) {
				rotateCamera(value, initialUpVec);
			} else if (name.equals("CAM_Right")) {
				rotateCamera(-value, initialUpVec);
			} else if (name.equals("CAM_Up")) {
				rotateCamera(-value * (invertY ? -1 : 1), cam.getLeft());
			} else if (name.equals("CAM_Down")) {
				rotateCamera(value * (invertY ? -1 : 1), cam.getLeft());
			}
		}
	};
	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (name.equals("Jump") && keyPressed/* && jumpSpeed == 0 */) {
				locChanges[1] = 2f;
			} else if (name.equals("Jump") && !keyPressed) {
				locChanges[1] = 0f;
			}

			if (name.equals("Down") && keyPressed) {
				locChanges[1] = -2f;
			} else if (name.equals("Down") && !keyPressed) {
				locChanges[1] = 0f;
			}

			if (name.equals("Left") && keyPressed) {
				locChanges[0] = speed;
			} else if (name.equals("Left") && !keyPressed
					&& locChanges[0] == speed) {
				locChanges[0] = 0;
			}

			if (name.equals("Right") && keyPressed) {
				locChanges[0] = -speed;
			} else if (name.equals("Right") && !keyPressed
					&& locChanges[0] == -speed) {
				locChanges[0] = 0;
			}

			if (name.equals("Forward") && keyPressed) {
				locChanges[2] = speed;
			} else if (name.equals("Forward") && !keyPressed
					&& locChanges[2] == speed) {
				locChanges[2] = 0;
			}

			if (name.equals("Back") && keyPressed) {
				locChanges[2] = -speed;
			} else if (name.equals("Back") && !keyPressed
					&& locChanges[2] == -speed) {
				locChanges[2] = 0;
			}
		}
	};
}
