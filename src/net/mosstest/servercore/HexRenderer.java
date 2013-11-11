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
import com.jme3.material.RenderState.FaceCullMode;
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
import com.jme3.util.BufferUtils;
import com.jme3.math.ColorRGBA;

import java.util.Arrays;

import net.mosstest.scripting.INodeParams;
import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.MapNode;
import net.mosstest.scripting.Position;

public class HexRenderer extends SimpleApplication {

	private static final float blockSize = 10f;
	private static final Vector3f[] chunkVertices = new Vector3f[4913];
	static {
		for (int x = 0; x <= 16; x++) {
			for (int y = 0; y <= 16; y++) {
				for (int z = 0; z <= 16; z++) {
					chunkVertices[289 * x + 17 * y + z] = new Vector3f(x
							* blockSize, y * blockSize, z * blockSize);
				}
			}
		}
	}
	private static final FloatBuffer vertexBuffer = BufferUtils
			.createFloatBuffer(chunkVertices);

	private static final float speed = 3f;
	private static final float playerHeight = 25;
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

	public static HexRenderer init(INodeManager manager, IRenderPreparator prep) {
		HexRenderer app = new HexRenderer();
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
		this.nManager = manager;
		this.rPreparator = prep;
	}

	@Override
	public void simpleInitApp() {
		this.lastTime = 0;
		this.worldNode = new Node("world");
		this.rootNode.attachChild(this.worldNode);
		this.spot.setSpotRange(150f);
		this.spot.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD);
		this.spot.setSpotOuterAngle(35f * FastMath.DEG_TO_RAD);
		this.spot.setColor(ColorRGBA.White.mult(3f));
		this.spot.setPosition(this.cam.getLocation());
		this.spot.setDirection(this.cam.getDirection());

		this.sun.setColor(ColorRGBA.White);
		this.sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
		this.rootNode.addLight(this.sun);
		this.rootNode.addLight(this.spot);
		testChunkEvents();
		// testLoadSurroundingChunks();
		this.flyCam.setEnabled(false);
		this.initialUpVec = this.cam.getUp().clone();
		initKeyBindings();
	}

	@Override
	/**
	 * Constant running loop that's built into SimpleApplication.
	 * Looks for new events in the renderEventQueue, moves if necessary.
	 */
	public void simpleUpdate(float tpf) {
		if (this.lastTime + 10 < System.currentTimeMillis()) {
			moveWorld(this.locChanges[0], this.locChanges[1],
					this.locChanges[2]);
			this.lastTime = System.currentTimeMillis();
		}

		this.inputManager.setCursorVisible(false);
		MossRenderEvent myEvent = this.renderEventQueue.poll();
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

	public void getChunk(Position pos) {
		MapChunk maybe = null;
		try {
			maybe = this.rPreparator.requestChunk(pos);
		} catch (MapGeneratorException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (maybe != null) {
			renderChunk(maybe, pos);
		}
	}

	public void renderChunk(MapChunk chk, Position pos) {
		if (chk == null) {
			return;
		}
		double offset = 16 * blockSize;
		int ixOffset = 0;
		Mesh bigMesh = new Mesh();
		FloatBuffer tcoords = FloatBuffer.allocate(100000);

		// 6 ints per face * 6 faces * 16 floors * 16 rows * 16 cols
		// RenderNode[][][] nodesInChunk = new RenderNode[16][16][16];
		IntBuffer indices = IntBuffer.allocate(6 * 6 * 16 * 16 * 16);
		indices.rewind();
		for (byte i = 0; i < 16; i++) {
			for (byte j = 0; j < 16; j++) {
				testLoop:for (byte k = 0; k < 16; k++) {
					if(Math.random()>0.02) continue testLoop;
					int nVal = chk.getNodeId(i, j, k);
					// MapNode node = nManager.getNode((short) nVal);
					// Material mat = getMaterial((short) nVal);
					if (nVal == 0) {
						System.out.println("WARRRNINGINIGNINGINGINN");
						return;
					}

					else {
						// These will probably be unused.
						float x = (float) ((pos.x + (32 * blockSize * pos.x))
								- offset + (i * 3 * blockSize));
						float y = (float) ((pos.y - playerHeight) - (j * 3 * blockSize));
						float z = (float) ((pos.z + (32 * blockSize * pos.z))
								- offset + (k * 3 * blockSize));

						// Tex coordinates are going to require some reworking.

						// The following is a valid name of a variable. It has
						// no special properties other than being distinctive.
						int _ = 289 * i + 17 * j + k;

						// IntBuffer#put returns that FloatBuffer so we can
						// chain like this.
						indices.put(_).put(_ + 289).put(_ + 1);
						indices.put(_ + 289 + 1).put(_ + 1).put(_ + 289);

						indices.put(_ + 289).put(_ + 17 + 289)
								.put(_ + 1 + 17 + 289);
						indices.put(_ + 1 + 289).put(_ + 289)
								.put(_ + 1 + 17 + 289);

						indices.put(_ + 1).put(_ + 1 + 289).put(_ + 1 + 17);
						indices.put(_ + 1 + 17 + 289).put(_ + 1 + 17)
								.put(_ + 1 + 289);

						indices.put(_ + 17).put(_ + 1 + 17).put(_ + 17 + 289);
						indices.put(_ + 1 + 17 + 289).put(_ + 17 + 289)
								.put(_ + 1 + 17);

						indices.put(_).put(_ + 17).put(_ + 289);
						indices.put(_ + 17 + 289).put(_ + 289).put(_ + 17);

						indices.put(_).put(_ + 1).put(_ + 1 + 17);
						indices.put(_ + 1 + 17).put(_ + 17).put(_ + 1);
						System.out.println("<<<<<<<<FINISHED NODE>>>>>>>>");
						System.out.println(indices.position() + ":"
								+ indices.limit() + ":" + indices.capacity());
						// RenderNode geom = new RenderNode(mat, loc, blockSize,
						// NodeManager.getNode((short)nVal)null);
						// nodesInChunk[i][j][k] = geom;
					}

				}
			}
		}
		Material mat = getMaterial((short) 1);
		bigMesh.setBuffer(Type.Position, 3, HexRenderer.vertexBuffer);
		bigMesh.setBuffer(Type.TexCoord, 2, tcoords);
		bigMesh.setBuffer(Type.Index, 3, indices);
		
		bigMesh.updateBound();
		Geometry geom = new Geometry("chunkMesh", bigMesh);
		//mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		geom.setMaterial(mat);
		this.worldNode.attachChild(geom);
		/*
		 * RenderMapChunk thisChunk = new RenderMapChunk(nodesInChunk, x, y, z);
		 * allChunks.put(pos, thisChunk);
		 */
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
		// MapChunk ch2 = new MapChunk(pos2, tN2, tM2);
		renderChunk(ch, pos);
		// renderChunk(ch2, pos2);
		GeometryBatchFactory.optimize(this.worldNode);
	}

	public Material getMaterial(short nVal) {
		Material mat = null;
		switch (nVal) {
		case 1:
			/*mat = new Material(this.assetManager,
					"Common/MatDefs/Light/Lighting.j3md");
			mat.setBoolean("UseMaterialColors", true);
			mat.setColor("Ambient", ColorRGBA.Green);
			mat.setColor("Diffuse", ColorRGBA.Red);*/
			mat = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
			mat.setColor("Color", ColorRGBA.Red);
		}
		return mat;
	}

	private void moveWorld(float cx, float cy, float cz) {

		Vector2f transVector = new Vector2f(this.cam.getDirection().x,
				this.cam.getDirection().z);

		this.worldNode.setLocalTranslation(this.worldNode
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
		mat.fromAngleNormalAxis(this.rotationSpeed * value, axis);

		Vector3f up = this.cam.getUp();
		Vector3f left = this.cam.getLeft();
		Vector3f dir = this.cam.getDirection();

		mat.mult(up, up);
		mat.mult(left, left);
		mat.mult(dir, dir);

		Quaternion q = new Quaternion();
		q.fromAxes(left, up, dir);
		q.normalizeLocal();

		this.cam.setAxes(q);

		this.spot.setDirection(this.cam.getDirection());
	}

	/**
	 * Set up key bindings and event listeners for key bindings
	 */
	private void initKeyBindings() {
		this.inputManager
				.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
		this.inputManager.addMapping("Down",
				new KeyTrigger(KeyInput.KEY_LSHIFT));
		this.inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
		this.inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
		this.inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
		this.inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));

		this.inputManager.addMapping("CAM_Left", new MouseAxisTrigger(
				MouseInput.AXIS_X, true), new KeyTrigger(KeyInput.KEY_LEFT));

		this.inputManager.addMapping("CAM_Right", new MouseAxisTrigger(
				MouseInput.AXIS_X, false), new KeyTrigger(KeyInput.KEY_RIGHT));

		this.inputManager.addMapping("CAM_Up", new MouseAxisTrigger(
				MouseInput.AXIS_Y, false), new KeyTrigger(KeyInput.KEY_UP));

		this.inputManager.addMapping("CAM_Down", new MouseAxisTrigger(
				MouseInput.AXIS_Y, true), new KeyTrigger(KeyInput.KEY_DOWN));

		this.inputManager.addListener(this.actionListener, "Jump");
		this.inputManager.addListener(this.actionListener, "Down");
		this.inputManager.addListener(this.actionListener, "Left");
		this.inputManager.addListener(this.actionListener, "Right");
		this.inputManager.addListener(this.actionListener, "Forward");
		this.inputManager.addListener(this.actionListener, "Back");
		this.inputManager.addListener(this.analogListener, "CAM_Left");
		this.inputManager.addListener(this.analogListener, "CAM_Right");
		this.inputManager.addListener(this.analogListener, "CAM_Up");
		this.inputManager.addListener(this.analogListener, "CAM_Down");
	}

	private AnalogListener analogListener = new AnalogListener() {

		public void onAnalog(String name, float value, float tpf) {
			if (name.equals("CAM_Left")) {
				rotateCamera(value, HexRenderer.this.initialUpVec);
			} else if (name.equals("CAM_Right")) {
				rotateCamera(-value, HexRenderer.this.initialUpVec);
			} else if (name.equals("CAM_Up")) {
				rotateCamera(-value * (HexRenderer.this.invertY ? -1 : 1),
						HexRenderer.this.cam.getLeft());
			} else if (name.equals("CAM_Down")) {
				rotateCamera(value * (HexRenderer.this.invertY ? -1 : 1),
						HexRenderer.this.cam.getLeft());
			}
		}
	};
	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (name.equals("Jump") && keyPressed/* && jumpSpeed == 0 */) {
				HexRenderer.this.locChanges[1] = 2f;
			} else if (name.equals("Jump") && !keyPressed) {
				HexRenderer.this.locChanges[1] = 0f;
			}

			if (name.equals("Down") && keyPressed) {
				HexRenderer.this.locChanges[1] = -2f;
			} else if (name.equals("Down") && !keyPressed) {
				HexRenderer.this.locChanges[1] = 0f;
			}

			if (name.equals("Left") && keyPressed) {
				HexRenderer.this.locChanges[0] = speed;
			} else if (name.equals("Left") && !keyPressed
					&& HexRenderer.this.locChanges[0] == speed) {
				HexRenderer.this.locChanges[0] = 0;
			}

			if (name.equals("Right") && keyPressed) {
				HexRenderer.this.locChanges[0] = -speed;
			} else if (name.equals("Right") && !keyPressed
					&& HexRenderer.this.locChanges[0] == -speed) {
				HexRenderer.this.locChanges[0] = 0;
			}

			if (name.equals("Forward") && keyPressed) {
				HexRenderer.this.locChanges[2] = speed;
			} else if (name.equals("Forward") && !keyPressed
					&& HexRenderer.this.locChanges[2] == speed) {
				HexRenderer.this.locChanges[2] = 0;
			}

			if (name.equals("Back") && keyPressed) {
				HexRenderer.this.locChanges[2] = -speed;
			} else if (name.equals("Back") && !keyPressed
					&& HexRenderer.this.locChanges[2] == -speed) {
				HexRenderer.this.locChanges[2] = 0;
			}
		}
	};
}
