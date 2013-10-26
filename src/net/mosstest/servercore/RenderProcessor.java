package net.mosstest.servercore;

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
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.math.ColorRGBA;

import java.util.Arrays;

import net.mosstest.scripting.INodeParams;
import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.MapNode;
import net.mosstest.scripting.Position;
public class RenderProcessor extends SimpleApplication {
	
	private float speed = 3f;
	private final float blockSize = 10f;
	private float[] locChanges = {0,0,0};
	private final float playerHeight = 25;
	private double lastTime;
	private boolean invertY = false;
	private HashMap<Position, RenderMapChunk> allChunks = new HashMap<Position, RenderMapChunk> ();
	private Vector3f initialUpVec;
	private float rotationSpeed = 1f;
	private Node worldNode;
	private SpotLight spot = new SpotLight();
	
	public NodeManager nManager;
	public NodeCache nCache;
	public ArrayBlockingQueue<MossRenderEvent> renderEventQueue = new ArrayBlockingQueue<>(24000, false);
	
	
	public static RenderProcessor init (NodeManager manager, NodeCache cache) {
		RenderProcessor app = new RenderProcessor ();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(800, 600);
		settings.setSamples(2);
		app.setSettings(settings);
		app.setShowSettings(false);
		app.initNodeThings(manager, cache);
		app.start();
		return app;
	}
	
	
	private void initNodeThings(NodeManager manager, NodeCache cache) {
		nManager = manager;
		nCache = cache;
	}
	
	@Override
	/**
	 * Constant running loop that's built into SimpleApplication.
	 * Looks for new events in the renderEventQueue, moves if necessary.
	 */
	public void simpleUpdate (float tpf) {
		if (lastTime + 10 < System.currentTimeMillis()) {
			moveWorld(locChanges[0], locChanges[1], locChanges[2]);
			lastTime = System.currentTimeMillis();
		}
		
		
		inputManager.setCursorVisible(false); 
		MossRenderEvent myEvent = renderEventQueue.poll();
		if (myEvent instanceof MossRenderStopEvent) {
			System.out.println("Thread shutting down");
		}
		else if (myEvent instanceof MossRenderChunkEvent) {
			int x = ((MossRenderChunkEvent) myEvent).getX();
			int y = ((MossRenderChunkEvent) myEvent).getY();
			int z = ((MossRenderChunkEvent) myEvent).getZ();
			double offset = 16*blockSize - blockSize;
			Vector3f home = new Vector3f (x, y, z);
			RenderNode[][][] nodesInChunk = new RenderNode[16][16][16];
			
			for(byte i=0; i<16; i++) {
				for(byte j=0; j<1; j++) {
					for(byte k=0; k<16; k++) {
						int nVal = ((MossRenderChunkEvent) myEvent).getNodeId(i, j, k);
						Material mat = getMaterial((short)nVal);
						switch (nVal) {
						case 0: break;
						case 1:
							float xLocation = (float)((home.x-offset) + (i * 2 * blockSize));
							float yLocation = (float)((home.y-playerHeight) - (j * 2 * blockSize));
							float zLocation = (float)((home.z-offset) + (k * 2 * blockSize));
							Vector3f loc = new Vector3f(xLocation, yLocation, zLocation);
						    RenderNode geom = new RenderNode (mat, loc, blockSize, /*NodeManager.getNode((short) nVal)*/ null);
						    nodesInChunk[i][j][k] = geom;
						    worldNode.attachChild(geom);
						    break;
						}
						
					}
				}
			}
			
			RenderMapChunk thisChunk = new RenderMapChunk(nodesInChunk, x, y, z);
			allChunks.put(((MossRenderChunkEvent) myEvent).getPos(), thisChunk);
			
		}
		else if (myEvent instanceof MossNodeAddEvent) {
			int x = ((MossNodeAddEvent) myEvent).getX();
			int y = ((MossNodeAddEvent) myEvent).getY();
			int z = ((MossNodeAddEvent) myEvent).getZ();
			Position pos = ((MossNodeAddEvent) myEvent).getPosition();

			short defRef = ((MossNodeAddEvent) myEvent).getDef();
			MapNode def = /*NodeManager.getNode(defRef);*/null;
			Material mat = getMaterial(defRef);
			allChunks.get(pos).addNode(def, mat, blockSize, x, y, z);
			Vector3f loc = allChunks.get(pos).getNodeLoc(x, y, z, blockSize); 
			RenderNode geom = new RenderNode (mat, loc, blockSize, def);
			worldNode.attachChild(geom);
			System.out.println("ADDED A NODE");
		}
		else if (myEvent instanceof MossRenderAddAssetPath) {
			String path = ((MossRenderAddAssetPath) myEvent).getPath();
			assetManager.registerLocator(path, com.jme3.asset.plugins.FileLocator.class);
		}
	}
	
	/**
	 * Temporary testing method that just loads chunks into the renderEventQueue
	 */
	public void testChunkEvents () {
		Position pos = null;
		pos = new Position(0, 0, 0, 0);
		boolean[][][] testModified = new boolean[16][16][16];
		for(boolean[][] l1 : testModified) {
			for(boolean[] l2 : l1) {
				Arrays.fill(l2, false);
			}
		}
				
		int[][][] testNodes = new int[16][16][16];
		for(int[][] l1 : testNodes) {
			for(int[] l2 : l1) {
				Arrays.fill(l2, 1);
			}
		}
		
		testNodes[0][0][0] = 0;
		testNodes[0][0][1] = 0;
		//testNodes[0][0][2] = 0;
		testNodes[0][0][3] = 0;
		testNodes[0][0][5] = 0;
		
		MapChunk ch = new MapChunk(pos, testNodes, testModified);
		MossRenderChunkEvent evt = new MossRenderChunkEvent (ch);
		renderEventQueue.add(evt);

		renderEventQueue.add(new MossNodeAddEvent(0, 0, 0, new Position(0, 0, 0, 0), (short) 1));
		GeometryBatchFactory.optimize(worldNode);
	}
	public Material getMaterial (short nVal) {
		Material mat = null;
		switch(nVal) {
		case 1:
			mat = new Material(assetManager,
				    "Common/MatDefs/Light/Lighting.j3md");
				    mat.setBoolean("UseMaterialColors",true);
				    mat.setColor("Ambient", ColorRGBA.Green);
				    mat.setColor("Diffuse", ColorRGBA.Green);
		}
		return mat;
	}
	
	/**
	 * Looks for changes in position, moves in direction of camera.
	 * @param cx change in x
	 * @param cy change in y
	 * @param cz change in z
	 */
	private void moveWorld(float cx, float cy, float cz) {

		Vector2f transVector = new Vector2f(cam.getDirection().x,
				cam.getDirection().z);
	
		worldNode.setLocalTranslation(worldNode
						.getLocalTranslation()
						.addLocal(
								new Vector3f(-cz * transVector.x, 0f, -cz* transVector.y))
						.addLocal(-cx * transVector.y, 0, cx * transVector.x));
	}
	
	/**
	 * Runs when the mouse moves to look around.
	 */
	private void rotateCamera(float value, Vector3f axis){

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
	 * Starting everything up
	 */
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
		rootNode.addLight(spot);
		testChunkEvents();
		flyCam.setEnabled(false);
		initialUpVec = cam.getUp().clone();
		initKeyBindings();
	}
	
	/**
	 * Set up key bindings and event listeners for key bindings
	 */
	private void initKeyBindings () {
		inputManager.addMapping("Test", new KeyTrigger(KeyInput.KEY_P));
		inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
		
		inputManager.addMapping("CAM_Left", new MouseAxisTrigger(MouseInput.AXIS_X, true),
                new KeyTrigger(KeyInput.KEY_LEFT));

		inputManager.addMapping("CAM_Right", new MouseAxisTrigger(MouseInput.AXIS_X, false),
		                 new KeyTrigger(KeyInput.KEY_RIGHT));
		
		inputManager.addMapping("CAM_Up", new MouseAxisTrigger(MouseInput.AXIS_Y, false),
		              new KeyTrigger(KeyInput.KEY_UP));
		
		inputManager.addMapping("CAM_Down", new MouseAxisTrigger(MouseInput.AXIS_Y, true),
		                new KeyTrigger(KeyInput.KEY_DOWN));
		
		inputManager.addListener(actionListener, "Test");
		inputManager.addListener(actionListener, "Left");
		inputManager.addListener(actionListener, "Right");
		inputManager.addListener(actionListener, "Forward");
		inputManager.addListener(actionListener, "Back");
		inputManager.addListener(analogListener, "CAM_Left");
		inputManager.addListener(analogListener, "CAM_Right");
		inputManager.addListener(analogListener, "CAM_Up");
		inputManager.addListener(analogListener, "CAM_Down");	
	}
	private AnalogListener analogListener = new AnalogListener () {

		public void onAnalog (String name, float value, float tpf) {
			if (name.equals("CAM_Left")){rotateCamera(value, initialUpVec);}
		   	else if (name.equals("CAM_Right")){rotateCamera(-value, initialUpVec);}
		       else if (name.equals("CAM_Up")){rotateCamera(-value * (invertY ? -1 : 1), cam.getLeft());}
		   	else if (name.equals("CAM_Down")){rotateCamera(value * (invertY ? -1 : 1), cam.getLeft());}
		}
	};
	private ActionListener actionListener = new ActionListener() {
	    public void onAction(String name, boolean keyPressed, float tpf) {
	    	if (name.equals("Test") && !keyPressed) {
	    		//renderEventQueue.add(testEvent2);
	    	}
	    	if (name.equals("Left") && keyPressed) {locChanges[0] = speed;}
	    	else if (name.equals("Left") && !keyPressed && locChanges[0] == speed) {locChanges[0] = 0;}
	    	
	    	if (name.equals("Right") && keyPressed) {locChanges[0] = -speed;}
	    	else if (name.equals("Right") && !keyPressed && locChanges[0] == -speed) {locChanges[0] = 0;}
	    	
	    	if (name.equals("Forward") && keyPressed) {locChanges[2] = speed;}
	    	else if (name.equals("Forward") && !keyPressed && locChanges[2] == speed) {locChanges[2] = 0;}
	    	
	    	if (name.equals("Back") && keyPressed) {locChanges[2] = -speed;}
	    	else if (name.equals("Back") && !keyPressed && locChanges[2] == -speed) {locChanges[2] = 0;}
	    }
	};
	
	
}
