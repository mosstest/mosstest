package net.mosstest.servercore;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import jme3tools.optimize.GeometryBatchFactory;
import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.Player;
import net.mosstest.scripting.Position;
import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

// TODO: Auto-generated Javadoc
/**
 * The Class RenderProcessor.
 */
public class RenderProcessor extends SimpleApplication {

	/** The logger. */
	static Logger logger = Logger.getLogger(RenderProcessor.class);
	
	/** The speed. */
	private final float SPEED = 3f;
	
	/** The player height. */
	private final float PLAYER_HEIGHT = 25;
	
	/** The block size. */
	private final float BLOCK_SIZE = 20f;
	
	/** The chunk size. */
	private final float CHUNK_SIZE = 16*BLOCK_SIZE;
	
	/** The rotation speed. */
	private final float ROTATION_SPEED = 1f;
	
	/** The block offset from center. */
	private final double BLOCK_OFFSET_FROM_CENTER = 8 * BLOCK_SIZE;
	
	/** The chunk offset. */
	private final double CHUNK_OFFSET = 8 * BLOCK_SIZE;
	
	/** The loc changes. */
	private float[] locChanges = { 0, 0, 0 };
	
	/** The last time. */
	private double lastTime;
	
	/** The invert y. */
	private boolean invertY = false;
	
	/** The initial up vec. */
	private Vector3f initialUpVec;
	private Object renderKey;
	private Node worldNode;
	
	/** The spot. */
	private SpotLight spot;
	
	/** The lamp. */
	private PointLight lamp;
	
	/** The sun. */
	private DirectionalLight sun;
	
	/** The all chunks. */
	private HashMap<Position, RenderMapChunk> allChunks = new HashMap<Position, RenderMapChunk>();

	/** The n manager. */
	public INodeManager nManager;
	
	/** The r preparator. */
	public IRenderPreparator rPreparator;
	public Player player;
	public ArrayBlockingQueue<MossRenderEvent> renderEventQueue = new ArrayBlockingQueue<>(
			24000, false);

	/**
	 * Inits the.
	 *
	 * @param manager the manager
	 * @param preparator the preparator
	 * @return the render processor
	 */
	public static RenderProcessor init(INodeManager manager, IRenderPreparator preparator) {
		java.util.logging.Logger.getLogger("").setLevel(Level.WARNING);
		RenderProcessor app = new RenderProcessor();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(800, 600);
		settings.setSamples(2);
		settings.setFullscreen(false);
		app.setSettings(settings);
		app.setShowSettings(false);
		app.initNodeManager(manager);
		app.initPreparator(preparator);
		app.initSecurityLock();
		app.start();
		return app;
	}

	private void initNodeManager (INodeManager manager) {
		nManager = manager;
	}
	
	/**
	 * Inits the preparator.
	 *
	 * @param prep the prep
	 */
	private void initPreparator(IRenderPreparator prep) {
		rPreparator = prep;
		logger.info("The renderer is starting its preparator, which is of type "+prep.getClass().getSimpleName()+".");
		rPreparator.setRenderProcessor(this);
		rPreparator.start();
	}

	private void initSecurityLock () {
		renderKey = new Object();
	}
	
	@Override
	public void simpleInitApp() {
		lastTime = 0;
		
		//acquireLock();
		setupWorldNode ();
		setupFlashlight();
		setupSunlight();
		setupLamplight();
		setupPlayer();
		
		preparatorChunkTest();
		flyCam.setEnabled(false);
		initialUpVec = cam.getUp().clone();
		initKeyBindings();
		//localChunkTest();
	}

	/* (non-Javadoc)
	 * @see com.jme3.app.SimpleApplication#simpleUpdate(float)
	 */
	@Override
	/**
	 * Constant running loop that's built into SimpleApplication.
	 * Looks for new events in the renderEventQueue, moves if necessary.
	 */
	public void simpleUpdate(float tpf) {
		if (lastTime + 10 < System.currentTimeMillis()) {
			move(locChanges[0], locChanges[1], locChanges[2]);
			lastTime = System.currentTimeMillis();
		}

		inputManager.setCursorVisible(false);
		MossRenderEvent myEvent = renderEventQueue.poll();
		if (myEvent instanceof MossRenderStopEvent) {
			logger.info("The renderer thread is shutting down.");
		}
		else if (myEvent instanceof MossRenderChunkEvent) {
			renderChunk(((MossRenderChunkEvent) myEvent).getChk(),
					((MossRenderChunkEvent) myEvent).getPos());
			GeometryBatchFactory.optimize(worldNode);
		}
	}

    /**
     * Gets the chunk.
     */
    public void getChunk (Position pos) {
		MapChunk maybe = null;
        try {
            maybe = rPreparator.requestChunk(pos);
        } catch (MapGeneratorException | InterruptedException e) {
            e.printStackTrace();
        }
        if (maybe != null) {
            renderChunk(maybe, pos);
        }
    }

    /**
     * Render chunk.
	 *
	 * @param chk the chk
	 * @param pos the pos
	 */
	public void renderChunk(MapChunk chk, Position pos) {
		int vertexIndexCounter = 0;
		
		Mesh completeMesh = new Mesh ();
		FloatBuffer vertices = getDirectFloatBuffer(150000);
		FloatBuffer normals = getDirectFloatBuffer(150000);
		IntBuffer indices = getDirectIntBuffer(150000);
		RenderNode[][][] renderNodes = new RenderNode[16][16][16];
		for (byte i = 0; i < 16; i++) {
			for (byte j = 0; j < 16; j++) {
				for (byte k = 0; k < 16; k++) {
					if (isNodeVisible(chk.getNodes(), i, j, k)) {
					
						int nVal = chk.getNodeId(i, j, k);
						//MapNode node = nManager.getNode((short) nVal);
						//Material mat = getMaterial((short) nVal);
						if (nVal == 0) {/*System.out.println("GOT A 0");*/}
						
						else {
							float x = (float) ((pos.x + (CHUNK_SIZE * pos.x)) - BLOCK_OFFSET_FROM_CENTER + CHUNK_OFFSET + (i * BLOCK_SIZE));
							float z = (float) ((pos.y - PLAYER_HEIGHT) - (j * BLOCK_SIZE));
							float y = (float) ((pos.z + (CHUNK_SIZE * pos.z)) - BLOCK_OFFSET_FROM_CENTER  + CHUNK_OFFSET + (k * BLOCK_SIZE));
							
							vertices.put(x).put(y).put(z); //Front face
							vertices.put(x).put(y - BLOCK_SIZE).put(z);
							vertices.put(x + BLOCK_SIZE).put(y).put(z);
							vertices.put(x + BLOCK_SIZE).put(y - BLOCK_SIZE).put(z); //Top Face
							vertices.put(x).put(y).put(z + BLOCK_SIZE);
							vertices.put(x + BLOCK_SIZE).put(y).put(z + BLOCK_SIZE);
							vertices.put(x + BLOCK_SIZE).put(y - BLOCK_SIZE).put(z + BLOCK_SIZE); //right face
							vertices.put(x).put(y - BLOCK_SIZE).put(z + BLOCK_SIZE); //left face
							
							for(int m=0; m<8; m++) {
								normals.put(0).put(0).put(10);
							}
							
							indices.put(vertexIndexCounter + 3).put(vertexIndexCounter + 1).put(vertexIndexCounter + 0);//front
							indices.put(vertexIndexCounter + 3).put(vertexIndexCounter + 0).put(vertexIndexCounter + 2);
							indices.put(vertexIndexCounter + 4).put(vertexIndexCounter + 2).put(vertexIndexCounter + 0);//top
							indices.put(vertexIndexCounter + 4).put(vertexIndexCounter + 5).put(vertexIndexCounter + 2);
							indices.put(vertexIndexCounter + 3).put(vertexIndexCounter + 2).put(vertexIndexCounter + 6);//right
							indices.put(vertexIndexCounter + 2).put(vertexIndexCounter + 5).put(vertexIndexCounter + 6);
							indices.put(vertexIndexCounter + 0).put(vertexIndexCounter + 1).put(vertexIndexCounter + 7);//left
							indices.put(vertexIndexCounter + 0).put(vertexIndexCounter + 7).put(vertexIndexCounter + 4);
							indices.put(vertexIndexCounter + 4).put(vertexIndexCounter + 6).put(vertexIndexCounter + 5);//back
							indices.put(vertexIndexCounter + 4).put(vertexIndexCounter + 7).put(vertexIndexCounter + 6);
							indices.put(vertexIndexCounter + 1).put(vertexIndexCounter + 6).put(vertexIndexCounter + 7);//bottom
							indices.put(vertexIndexCounter + 1).put(vertexIndexCounter + 3).put(vertexIndexCounter + 6);
							//RenderNode geom = new RenderNode(mat, loc, BLOCK_SIZE, NodeManager.getNode((short)nVal)null);
							//renderNodes[i][j][k] = geom;
							vertexIndexCounter += 8;
						}
					}
				}
			}
		}
		Material mat = getMaterial((short) 1);
		completeMesh.setBuffer(Type.Position, 3, vertices);
		completeMesh.setBuffer(Type.Normal, 3, normals);
		completeMesh.setBuffer(Type.Index, 3, indices);
		completeMesh.updateBound();
		Geometry geom = new Geometry("chunkMesh", completeMesh);
		geom.setMaterial(mat);
		worldNode.attachChild(geom);
		RenderMapChunk currentChunk = new RenderMapChunk(renderNodes);
		allChunks.put(pos, currentChunk);
	}

	private void preparatorChunkTest() {
		Position p1 = new Position(0, 0, 0, 0);
		Position p2 = new Position(1, 0, 0, 0);
		Position p3 = new Position(0, 1, 0, 0);
		Position p4 = new Position(1, 1, 0, 0);
		Position p5 = new Position(-1,0,0,0);
		Position p6 = new Position(0,-1,0,0);
		Position p7 = new Position(-1,-1,0,0);

		getChunk(p1);
		getChunk(p2);
		getChunk(p3);
		getChunk(p4);
		getChunk(p5);
		getChunk(p6);
		getChunk(p7);
	}
	
	/**
	 * Gets the direct float buffer.
	 *
	 * @param size the size
	 * @return the direct float buffer
	 */
	private FloatBuffer getDirectFloatBuffer (int size) {
		ByteBuffer temp = ByteBuffer.allocateDirect(size);
		return temp.asFloatBuffer();
	}
	
	/**
	 * Gets the direct int buffer.
	 *
	 * @param size the size
	 * @return the direct int buffer
	 */
	private IntBuffer getDirectIntBuffer (int size) {
		ByteBuffer temp = ByteBuffer.allocateDirect(size);
		return temp.asIntBuffer();
	}

	/**
	 * Setup flashlight.
	 */
	private void setupFlashlight () {
		spot = new SpotLight();
		spot.setSpotRange(300f);
		spot.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD);
		spot.setSpotOuterAngle(35f * FastMath.DEG_TO_RAD);
		spot.setColor(ColorRGBA.White.mult(3f));
		spot.setPosition(cam.getLocation());
		spot.setDirection(cam.getDirection());
		rootNode.addLight(spot);
	}
	
	/**
	 * Setup sunlight.
	 */
	private void setupSunlight () {
		sun = new DirectionalLight();
		sun.setColor(ColorRGBA.White);
		sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
		rootNode.addLight(sun);
	}
	
	/**
	 * Setup lamplight.
	 */
	private void setupLamplight () {
		lamp = new PointLight();
		lamp.setColor(ColorRGBA.Yellow);
		lamp.setRadius(4f);
		lamp.setPosition(cam.getLocation());
		rootNode.addLight(lamp);
	}
	
	/**
	 * Setup world node.
	 */
	private void setupWorldNode () {
		worldNode = new Node("world");
		rootNode.attachChild(worldNode);
	}
	
	/**
	 * Setup player.
	 */
	private void setupPlayer () {
		player = new Player ("Test Guy");
		player.setPositionOffsets (0,0,0);
		player.setChunkPosition(0,0,0);
		cam.setLocation(new Vector3f(0,0,0));
	}
	
	/**
	 * Gets the material.
	 *
	 * @param nVal the n val
	 * @return the material
	 */
	private Material getMaterial(short nVal) {
		Material mat = null;
		switch (nVal) {
		case 1:/*
			mat = new Material(assetManager,
					"Common/MatDefs/Light/Lighting.j3md");
			mat.setBoolean("UseMaterialColors", true);
			mat.setColor("Ambient", ColorRGBA.Green);
			mat.setColor("Diffuse", ColorRGBA.Green);
			*/
			//mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
			mat= new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
			mat.setColor("Diffuse", ColorRGBA.Green);
			mat.setColor("Specular",ColorRGBA.Green);
			
		}
		return mat;
	}
	
	/**
	 * Move.
	 *
	 * @param cx the cx
	 * @param cy the cy
	 * @param cz the cz
	 */
	private void move(float cx, float cy, float cz) {

		Vector2f transVector = new Vector2f(cam.getDirection().x,
				cam.getDirection().z);

		worldNode.setLocalTranslation(worldNode
				.getLocalTranslation()
				.addLocal(
						new Vector3f(-cz * transVector.x, 0f, -cz
								* transVector.y))
				.addLocal(-cx * transVector.y, 0f, cx * transVector.x)
				.addLocal(0f, -cy, 0f));
		
		double xpos = -(worldNode.getLocalTranslation().x);
		double ypos = -(worldNode.getLocalTranslation().y);
		double zpos = -(worldNode.getLocalTranslation().z);
		int xchk = (int)Math.floor(xpos / (CHUNK_SIZE));
		int ychk = (int)Math.floor(ypos / (CHUNK_SIZE));
		int zchk = (int)Math.floor(zpos / (CHUNK_SIZE));
		double xoffset = (xpos % CHUNK_SIZE)/BLOCK_SIZE;
		double yoffset = (ypos % CHUNK_SIZE)/BLOCK_SIZE;
		double zoffset = (zpos % CHUNK_SIZE)/BLOCK_SIZE;
		
		synchronized(player.antiCheatDataLock){
			player.setChunkPosition (xchk, ychk, zchk);
			player.setPositionOffsets (xoffset, yoffset, zoffset);
		}
	}

	/**
	 * Rotate camera.
	 *
	 * @param value the value
	 * @param axis the axis
	 */
	private void rotateCamera(float value, Vector3f axis) {

		Matrix3f mat = new Matrix3f();
		mat.fromAngleNormalAxis(ROTATION_SPEED * value, axis);

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

	private boolean isNodeVisible (int[][][] chunk, int i, int j, int k) {
		if (i == 0 || j == 0 || k == 0 || i == chunk.length-1 || j == chunk[0].length-1 || k == chunk[0][0].length-1) {
			return true;
		}
		return (chunk[i+1][j][k] == 0 || chunk[i][j+1][k] == 0 || chunk[i][j][k+1] == 0 ||
			chunk[i-1][j][k] == 0 || chunk[i][j-1][k] == 0 || chunk[i][j][k-1] == 0);
	}

	private void acquireLock () {
		MosstestSecurityManager.instance.lock(renderKey, null);
	}
	
	private void releaseLock () {
		MosstestSecurityManager.instance.unlock(renderKey);
	}
	
	/**
	 * Inits the key bindings.
	 */
	private void initKeyBindings() {
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_LSHIFT));
		inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
		inputManager.addMapping("TestFeature", new KeyTrigger(KeyInput.KEY_P));

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
		inputManager.addListener(actionListener, "TestFeature");
	}

	/** The analog listener. */
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
	
	/** The action listener. */
	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (name.equals("Jump") && keyPressed/* && jumpSPEED == 0 */) {
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
				locChanges[0] = SPEED;
			} else if (name.equals("Left") && !keyPressed
					&& locChanges[0] == SPEED) {
				locChanges[0] = 0;
			}

			if (name.equals("Right") && keyPressed) {
				locChanges[0] = -SPEED;
			} else if (name.equals("Right") && !keyPressed
					&& locChanges[0] == -SPEED) {
				locChanges[0] = 0;
			}

			if (name.equals("Forward") && keyPressed) {
				locChanges[2] = SPEED;
			} else if (name.equals("Forward") && !keyPressed
					&& locChanges[2] == SPEED) {
				locChanges[2] = 0;
			}

			if (name.equals("Back") && keyPressed) {
				locChanges[2] = -SPEED;
			} else if (name.equals("Back") && !keyPressed
					&& locChanges[2] == -SPEED) {
				locChanges[2] = 0;
			}
			
			if (name.equals("TestFeature") && keyPressed) {
				System.err.println("\nDEBUGGING FEATURE\n");
			}
		}
	};
}
