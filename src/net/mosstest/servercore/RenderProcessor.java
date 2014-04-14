package net.mosstest.servercore;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

import jme3tools.optimize.GeometryBatchFactory;
import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.Player;
import net.mosstest.scripting.Position;

import org.apache.log4j.Logger;

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
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;


public class RenderProcessor extends SimpleApplication {

	static Logger logger = Logger.getLogger(RenderProcessor.class);
	private final float SPEED = 3f;
	private final float BLOCK_SIZE = 20f;
	private final float CHUNK_SIZE = 16*BLOCK_SIZE;
	private final float ROTATION_SPEED = 1f;
	private final double BLOCK_OFFSET_FROM_CENTER = 8 * BLOCK_SIZE;
	private final double CHUNK_OFFSET = 8 * BLOCK_SIZE;
	private float[] locChanges = { 0, 0, 0 };
	private double lastTime;
	private boolean invertY = false;
	private Vector3f initialUpVec;
	private Object renderKey;
	private Node worldNode;
	private SpotLight spot;
	private PointLight lamp;
	private DirectionalLight sun;
	//private HashMap<Position, RenderMapChunk> allChunks = new HashMap<Position, RenderMapChunk>();
	public INodeManager nManager;
	public IRenderPreparator rPreparator;
	public Player player;
	public ArrayBlockingQueue<MossRenderEvent> renderEventQueue = new ArrayBlockingQueue<>(
			24000, false);

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
		setupAssetManager();
		setupPlayer();
		preparatorChunkTest();
		//blankChunkTest();
		flyCam.setEnabled(false);
		initialUpVec = cam.getUp().clone();
		initKeyBindings();
	}

	@Override
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

	public void getChunk (Position pos) {
		MapChunk maybe = null;
		try {maybe = rPreparator.requestChunk(pos);} 
		catch (MapGeneratorException e) {e.printStackTrace();} 
		catch (InterruptedException e) {e.printStackTrace();}
		if (maybe != null) {renderChunk(maybe, pos);}
	}
	
	public void renderChunk(MapChunk chk, Position pos) {
		int vertexIndexCounter = 0;
		Mesh completeMesh = new Mesh ();
		FloatBuffer vertices = getDirectFloatBuffer(950000);
        FloatBuffer tex = getDirectFloatBuffer(950000);
		FloatBuffer normals = getDirectFloatBuffer(950000);
		IntBuffer indices = getDirectIntBuffer(950000);
		//RenderNode[][][] renderNodes = new RenderNode[16][16][16];
		for (byte i = 0; i < 16; i++) {
			for (byte j = 0; j < 16; j++) {
				for (byte k = 0; k < 16; k++) {
					if (isNodeVisible(chk.getNodes(), i, j, k)) {
						
						int nVal = chk.getNodeId(i, j, k);
						//MapNode node = nManager.getNode((short) nVal);
						//Material mat = getMaterial((short) nVal);
						if (nVal == 0) {}
						
						else {
							//z and y are switched on purpose.
							float x = (float) ((pos.x + (CHUNK_SIZE * pos.x)) - BLOCK_OFFSET_FROM_CENTER + CHUNK_OFFSET + (i * BLOCK_SIZE));
							float z = (float) ((pos.y - (CHUNK_SIZE * pos.y)) - BLOCK_OFFSET_FROM_CENTER + CHUNK_OFFSET + (j * BLOCK_SIZE));
							float y = (float) ((pos.z + (CHUNK_SIZE * pos.z)) - BLOCK_OFFSET_FROM_CENTER + CHUNK_OFFSET + (k * BLOCK_SIZE));
							
							/**
							 * Vertices start at the top left corner and go clockwise around the face.
							 */
							vertices.put(x).put(y).put(z); //FRONT
							vertices.put(x + BLOCK_SIZE).put(y).put(z);
							vertices.put(x + BLOCK_SIZE).put(y).put(z - BLOCK_SIZE);
							vertices.put(x).put(y).put(z - BLOCK_SIZE);
							tex.put(0).put(0);
							tex.put(0).put(1);
							tex.put(1).put(1);
							tex.put(1).put(0);
							
							vertices.put(x).put(y + BLOCK_SIZE).put(z); //TOP
							vertices.put(x + BLOCK_SIZE).put(y + BLOCK_SIZE).put(z);
							vertices.put(x + BLOCK_SIZE).put(y).put(z);
							vertices.put(x).put(y).put(z);
							tex.put(0).put(0);
							tex.put(0).put(1);
							tex.put(1).put(1);
							tex.put(1).put(0);
							
							vertices.put(x + BLOCK_SIZE).put(y + BLOCK_SIZE).put(z);//BACK	
							vertices.put(x).put(y + BLOCK_SIZE).put(z);
							vertices.put(x).put(y + BLOCK_SIZE).put(z - BLOCK_SIZE);
							vertices.put(x + BLOCK_SIZE).put(y + BLOCK_SIZE).put(z - BLOCK_SIZE);
							tex.put(0).put(0);
							tex.put(0).put(1);
							tex.put(1).put(1);
							tex.put(1).put(0);
							
							vertices.put(x + BLOCK_SIZE).put(y + BLOCK_SIZE).put(z - BLOCK_SIZE);
							vertices.put(x).put(y + BLOCK_SIZE).put(z - BLOCK_SIZE);
							vertices.put(x).put(y).put(z - BLOCK_SIZE);
							vertices.put(x + BLOCK_SIZE).put(y).put(z - BLOCK_SIZE);
							tex.put(0).put(0);
							tex.put(0).put(1);
							tex.put(1).put(1);
							tex.put(1).put(0);
							
							vertices.put(x).put(y + BLOCK_SIZE).put(z); //LEFT
							vertices.put(x).put(y).put(z);
							vertices.put(x).put(y).put(z - BLOCK_SIZE);
							vertices.put(x).put(y + BLOCK_SIZE).put(z - BLOCK_SIZE);
							tex.put(0).put(0);
							tex.put(0).put(1);
							tex.put(1).put(1);
							tex.put(1).put(0);
							
							vertices.put(x + BLOCK_SIZE).put(y).put(z); //RIGHT
							vertices.put(x + BLOCK_SIZE).put(y + BLOCK_SIZE).put(z);
							vertices.put(x + BLOCK_SIZE).put(y + BLOCK_SIZE).put(z - BLOCK_SIZE);
							vertices.put(x + BLOCK_SIZE).put(y).put(z - BLOCK_SIZE);
							tex.put(0).put(0);
							tex.put(0).put(1);
							tex.put(1).put(1);
							tex.put(1).put(0);
							
							for(int m=0; m<24; m++) {
								normals.put(2).put(3).put(5);
							}
							
							indices.put(vertexIndexCounter + 0).put(vertexIndexCounter + 2).put(vertexIndexCounter + 1);//front
							indices.put(vertexIndexCounter + 0).put(vertexIndexCounter + 3).put(vertexIndexCounter + 2);
							
							indices.put(vertexIndexCounter + 4).put(vertexIndexCounter + 6).put(vertexIndexCounter + 5);//top
							indices.put(vertexIndexCounter + 4).put(vertexIndexCounter + 7).put(vertexIndexCounter + 6);
							
							indices.put(vertexIndexCounter + 8).put(vertexIndexCounter + 10).put(vertexIndexCounter + 9);//back
							indices.put(vertexIndexCounter + 8).put(vertexIndexCounter + 11).put(vertexIndexCounter + 10);
							
							indices.put(vertexIndexCounter + 12).put(vertexIndexCounter + 14).put(vertexIndexCounter + 13);//bottom
							indices.put(vertexIndexCounter + 12).put(vertexIndexCounter + 15).put(vertexIndexCounter + 14);
							
							indices.put(vertexIndexCounter + 16).put(vertexIndexCounter + 18).put(vertexIndexCounter + 17);//left
							indices.put(vertexIndexCounter + 16).put(vertexIndexCounter + 19).put(vertexIndexCounter + 18);
							
							indices.put(vertexIndexCounter + 20).put(vertexIndexCounter + 22).put(vertexIndexCounter + 21);//right
							indices.put(vertexIndexCounter + 20).put(vertexIndexCounter + 23).put(vertexIndexCounter + 22);
							//RenderNode geom = new RenderNode(mat, loc, BLOCK_SIZE, NodeManager.getNode((short)nVal)null);
							//renderNodes[i][j][k] = geom;
							vertexIndexCounter += 24;
						}
					}
				}
			}
		}
		Material mat = getMaterial((short) 1);
		completeMesh.setBuffer(Type.Position, 3, vertices);
		completeMesh.setBuffer(Type.Normal, 3, normals);
		completeMesh.setBuffer(Type.Index, 3, indices);
        completeMesh.setBuffer(Type.TexCoord, 2, tex);
		completeMesh.updateBound();
		Geometry geom = new Geometry("chunkMesh", completeMesh);
		geom.setMaterial(mat);
		worldNode.attachChild(geom);
		//RenderMapChunk currentChunk = new RenderMapChunk(renderNodes);
		//allChunks.put(pos, currentChunk);
    }
	
	private Material getMaterial(short nodeType) {
		Material mat = null;
		switch (nodeType) {
		case 1:
			mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
			Texture tx = assetManager.loadTexture("default/grass.png");
			tx.setMagFilter(Texture.MagFilter.Nearest);
			mat.setTexture("DiffuseMap", tx);
		}
		return mat;
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
	
	private void blankChunkTest () {
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
		
		renderEventQueue.add(e1);
		renderEventQueue.add(e2);
		//renderChunk(c2, p2);
		
	}
	
	private FloatBuffer getDirectFloatBuffer (int size) {
		ByteBuffer temp = ByteBuffer.allocateDirect(size);
		return temp.asFloatBuffer();
	}	

	private IntBuffer getDirectIntBuffer (int size) {
		ByteBuffer temp = ByteBuffer.allocateDirect(size);
		return temp.asIntBuffer();
	}

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
	
	private void setupSunlight () {
		sun = new DirectionalLight();
		sun.setColor(ColorRGBA.White);
		sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
		rootNode.addLight(sun);
	}
	
	private void setupLamplight () {
		lamp = new PointLight();
		lamp.setColor(ColorRGBA.Yellow);
		lamp.setRadius(4f);
		lamp.setPosition(cam.getLocation());
		rootNode.addLight(lamp);
	}
	
	private void setupWorldNode () {
		worldNode = new Node("world");
		rootNode.attachChild(worldNode);
	}

	private void setupPlayer () {
		player = new Player ("Test Guy");
		player.setPositionOffsets (0,5,0);
		player.setChunkPosition(0,0,0);
		cam.setLocation(new Vector3f(0, 0, 0));
	}
	
	private void setupAssetManager () {
		assetManager.registerLocator("scripts", LocalAssetLocator.class);
	}
	
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
