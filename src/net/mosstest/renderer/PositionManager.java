package net.mosstest.renderer;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class PositionManager {
	private InputManager inputManager;
	private final float SPEED = 3f;
	private final float ROTATION_SPEED = 1f;
	private float[] locChanges = { 0, 0, 0 };
	private boolean invertY = false;
	private double lastTime;
	private ActivityListener activityListener;
	private RotationListener rotationListener;
	private RenderProcessor renderer;
	
	public PositionManager (InputManager inputManager, RenderProcessor renderer) {
		lastTime = 0;
		this.inputManager = inputManager;
		this.renderer = renderer;
	}
	
	public void updatePosition () {
		if (lastTime + 10 < System.currentTimeMillis()) {
			renderer.move(locChanges[0], locChanges[1], locChanges[2]);
			lastTime = System.currentTimeMillis();
		}
		inputManager.setCursorVisible(false);
	}
	
	public void initListeners (Camera cam) {
		Vector3f upVector = cam.getUp().clone();
		rotationListener = new RotationListener (upVector, invertY, cam, ROTATION_SPEED);
		activityListener = new ActivityListener (locChanges, SPEED);
	}
	
	public void initKeyBindings() {
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

		inputManager.addListener(activityListener, "Jump");
		inputManager.addListener(activityListener, "Down");
		inputManager.addListener(activityListener, "Left");
		inputManager.addListener(activityListener, "Right");
		inputManager.addListener(activityListener, "Forward");
		inputManager.addListener(activityListener, "Back");
		inputManager.addListener(rotationListener, "CAM_Left");
		inputManager.addListener(rotationListener, "CAM_Right");
		inputManager.addListener(rotationListener, "CAM_Up");
		inputManager.addListener(rotationListener, "CAM_Down");
		inputManager.addListener(activityListener, "TestFeature");
	}
	
}
