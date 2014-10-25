package net.mosstest.renderer;
import com.jme3.input.controls.ActionListener;

public class ActivityListener implements ActionListener {
	private final float SPEED;
	private float[] velocities;
	
	public ActivityListener (float[] velocities, float speed) {
		this.velocities = velocities;
		SPEED = speed;
	}
	
	@Override
	public void onAction(String name, boolean keyPressed, float tpf) {
		if (name.equals("Jump") && keyPressed/* && jumpSPEED == 0 */) {
			velocities[1] = 2f;
		} else if (name.equals("Jump") && !keyPressed) {
			velocities[1] = 0f;
		}

		if (name.equals("Down") && keyPressed) {
			velocities[1] = -2f;
		} else if (name.equals("Down") && !keyPressed) {
			velocities[1] = 0f;
		}

		if (name.equals("Left") && keyPressed) {
			velocities[0] = SPEED;
		} else if (name.equals("Left") && !keyPressed
				&& velocities[0] == SPEED) {
			velocities[0] = 0;
		}

		if (name.equals("Right") && keyPressed) {
			velocities[0] = -SPEED;
		} else if (name.equals("Right") && !keyPressed
				&& velocities[0] == -SPEED) {
			velocities[0] = 0;
		}

		if (name.equals("Forward") && keyPressed) {
			velocities[2] = SPEED;
		} else if (name.equals("Forward") && !keyPressed
				&& velocities[2] == SPEED) {
			velocities[2] = 0;
		}

		if (name.equals("Back") && keyPressed) {
			velocities[2] = -SPEED;
		} else if (name.equals("Back") && !keyPressed
				&& velocities[2] == -SPEED) {
			velocities[2] = 0;
		}
		
		if (name.equals("TestFeature") && keyPressed) {
			System.err.println("\nDEBUGGING FEATURE\n");
		}
	}
	
}
