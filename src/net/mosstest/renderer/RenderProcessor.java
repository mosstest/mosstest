package net.mosstest.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

import jme3tools.optimize.GeometryBatchFactory;
import jme3tools.optimize.TextureAtlas;
import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.MapNode;
import net.mosstest.scripting.Player;
import net.mosstest.scripting.Position;
import net.mosstest.servercore.INodeManager;
import net.mosstest.servercore.IRenderPreparator;
import net.mosstest.servercore.LocalAssetLocator;
import net.mosstest.servercore.MapGeneratorException;
import net.mosstest.servercore.MossRenderChunkEvent;
import net.mosstest.servercore.MossRenderEvent;
import net.mosstest.servercore.MossRenderStopEvent;

import org.apache.log4j.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import com.jme3.ui.Picture;

public class RenderProcessor extends SimpleApplication {

	static Logger logger = Logger.getLogger(RenderProcessor.class);
	public static final float NODE_SIZE = 20f;
	public static final float CHUNK_SIZE = 16*NODE_SIZE;
	public static final double NODE_OFFSET_FROM_CENTER = 8 * NODE_SIZE;
	public static final double CHUNK_OFFSET = 8 * NODE_SIZE;
	public static TextureAtlas textures;
	private Node worldNode;
	private PositionManager positionManager;
	
	//private HashMap<Position, RenderMapChunk> allChunks = new HashMap<Position, RenderMapChunk>();
	public INodeManager nodeManager;
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
		app.start();
		return app;
	}
	
	private void initNodeManager (INodeManager manager) {
		nodeManager = manager;
	}

	private void initPreparator(IRenderPreparator prep) {
		rPreparator = prep;
		rPreparator.setRenderProcessor(this);
		rPreparator.start();
	}
	
	@Override
	public void simpleInitApp() {
		worldNode = new Node("world");
		
		assetManager.registerLocator("scripts", LocalAssetLocator.class);
		buildTextureAtlas();
		flyCam.setEnabled(false);
		inputManager.setCursorVisible(false);
		
		player = PlayerManager.makePlayer();
		cam.setLocation(new Vector3f(0, 0, 0));
        setupHud();
		
        positionManager = new PositionManager(inputManager, this);
		positionManager.initListeners(cam);
		positionManager.initKeyBindings();

		RenderTester.preparatorChunkTest(this);
		
		rootNode.attachChild(worldNode);
		rootNode.addLight(LightingFactory.makeFlashlight(cam.getLocation(), cam.getDirection(), 300f));
		rootNode.addLight(LightingFactory.makeDirectionalLight(ColorRGBA.White, new Vector3f(-.5f, -.5f, -.5f)));
		rootNode.addLight(LightingFactory.makePointLight(ColorRGBA.Yellow, 4f, cam.getLocation()));
	}
    
	private void setupHud() {
        Picture pic = new Picture("Crosshair");
        pic.setImage(assetManager, "builtins/crosshair.png", true);
        pic.setWidth(32);
        pic.setHeight(32);
        pic.setPosition(settings.getWidth()/2-16, settings.getHeight()/2-16);
        guiNode.attachChild(pic);
    }
	
	@Override
	public void simpleUpdate(float tpf) {
		positionManager.updatePosition();
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
//
	public void renderChunk(MapChunk chk, Position pos) {
		Mesh mesh = new Mesh ();
		NodeFaceRenderer.initialize();
		//RenderNode[][][] renderNodes = new RenderNode[16][16][16];
		for (byte i = 0; i < 16; i++) {
			for (byte j = 0; j < 16; j++) {
				for (byte k = 0; k < 16; k++) {
					int[][][] nodes = chk.getNodes();
					if (isNodeVisible(nodes, i, j, k)) {
						int nVal = chk.getNodeId(i, j, k);
						//MapNode node = nManager.getNode((short) nVal);
						//Material mat = getMaterial((short) nVal);
						if (nVal != 0) {
							//z and y are switched on purpose.
							float x = (float) ((pos.x + (CHUNK_SIZE * pos.x)) - NODE_OFFSET_FROM_CENTER + CHUNK_OFFSET + (i * NODE_SIZE));
							float z = (float) ((pos.y - (CHUNK_SIZE * pos.y)) - NODE_OFFSET_FROM_CENTER + CHUNK_OFFSET + (j * NODE_SIZE));
							float y = (float) ((pos.z + (CHUNK_SIZE * pos.z)) - NODE_OFFSET_FROM_CENTER + CHUNK_OFFSET + (k * NODE_SIZE));
							
							//TextureAtlasTile texture = RenderProcessor.textures.getAtlasTile(texture);
							for (NodeFace face : NodeFace.values()) {
								if (NodeFaceRenderer.isNodeFaceVisible(face, nodes, i, j, k)) {
									NodeFaceRenderer.populateBuffers(face, x, y, z, NODE_SIZE);
								}
							}
							//RenderNode geom = new RenderNode(mat, loc, NODE_SIZE, NodeManager.getNode((short)nVal)null);
							//renderNodes[i][j][k] = geom;
						}
					}
				}
			}
		}
		FloatBuffer vertices = NodeFaceRenderer.getVertices();
		FloatBuffer tex = NodeFaceRenderer.getTextureCoordinates();
		FloatBuffer normals = NodeFaceRenderer.getNormals();
		IntBuffer indices = NodeFaceRenderer.getIndices();
		
		mesh.setBuffer(Type.Position, 3, vertices);
		mesh.setBuffer(Type.Normal, 3, normals);
		mesh.setBuffer(Type.Index, 3, indices);
        mesh.setBuffer(Type.TexCoord, 2, tex);
		mesh.updateBound();
		
		Geometry geom = new Geometry("chunkMesh", mesh);
		Material mat = getMaterial((short) 1);
		geom.setMaterial(mat);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
		worldNode.attachChild(geom);
		//RenderMapChunk currentChunk = new RenderMapChunk(renderNodes);
		//allChunks.put(pos, currentChunk);
    }
	
	private Material getMaterial(short nodeType) {
		Material mat = null;
		switch (nodeType) {
		case 1:
			mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            //Texture tx = assetManager.loadTexture("grass.png");
			//Texture tx = assetManager.loadTexture("default/item_torch.png");
			//tx.setMagFilter(Texture.MagFilter.Nearest);
			//mat.setTexture("DiffuseMap", tx);
            //mat.setBoolean("UseAlpha", true);
            //mat.getAdditionalRenderState().setAlphaTest(true);
            //mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		}
		return mat;
	}
	
	public void getChunk (Position pos) {
		MapChunk maybe = null;
		try {maybe = rPreparator.requestChunk(pos);} 
		catch (MapGeneratorException e) {e.printStackTrace();} 
		catch (InterruptedException e) {e.printStackTrace();}
		if (maybe != null) {renderChunk(maybe, pos);}
	}
	
	public void move(float cx, float cy, float cz) {
		Vector2f transVector = new Vector2f(cam.getDirection().x,
				cam.getDirection().z).normalizeLocal();
 
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
		double xoffset = (xpos % CHUNK_SIZE)/NODE_SIZE;
		double yoffset = (ypos % CHUNK_SIZE)/NODE_SIZE;
		double zoffset = (zpos % CHUNK_SIZE)/NODE_SIZE;
		
		synchronized(player.antiCheatDataLock){
			player.setChunkPosition (xchk, ychk, zchk);
			player.setPositionOffsets (xoffset, yoffset, zoffset);
		}
	}
	
	
	
	private void buildTextureAtlas () {
		textures = new TextureAtlas(1024, 1024);
		HashSet<String> uniqueTextures = new HashSet<String>();
		List<MapNode> defs = nodeManager.getNodeDefinitions();
		for (MapNode m : defs) {
			for (String textureLink : m.texture) {
				uniqueTextures.add(textureLink);
			}
		}
		
		Iterator<String> it = uniqueTextures.iterator();
		while (it.hasNext()) {
			String textureLink = it.next();
			try {
				textures.addTexture(assetManager.loadTexture(textureLink), textureLink);
				System.out.println("Renderer loaded: "+textureLink);
			} catch (Exception e) {
				System.out.println("Renderer unable to find: "+textureLink);
			}
		}
	}

	private boolean isNodeVisible (int[][][] chunk, int i, int j, int k) {
		if (i == 0 || j == 0 || k == 0 || i == chunk.length-1 || j == chunk[0].length-1 || k == chunk[0][0].length-1) {
			return true;
		}
		return (chunk[i+1][j][k] == 0 || chunk[i][j+1][k] == 0 || chunk[i][j][k+1] == 0 ||
			chunk[i-1][j][k] == 0 || chunk[i][j-1][k] == 0 || chunk[i][j][k-1] == 0);
	}
}