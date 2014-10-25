package net.mosstest.renderer;

import com.jme3.input.controls.AnalogListener;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class RotationListener implements AnalogListener {
	private final float ROTATION_SPEED;
	private Vector3f axis;
	private boolean invertY;
	private Camera cam;
	
	public RotationListener (Vector3f v, boolean invert, Camera c, float rSpeed) {
		axis = v;
		System.out.println("AXIS: "+axis);
		invertY = invert;
		cam = c;
		ROTATION_SPEED = rSpeed;
	}
	
	public void onAnalog(String name, float value, float tpf) {
		if (name.equals("CAM_Left")) {
			rotateCamera(value, axis);
		} else if (name.equals("CAM_Right")) {
			rotateCamera(-value, axis);
		} else if (name.equals("CAM_Up")) {
			rotateCamera(-value * (invertY ? -1 : 1), cam.getLeft());
		} else if (name.equals("CAM_Down")) {
			rotateCamera(value * (invertY ? -1 : 1), cam.getLeft());
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
        if (up.angleBetween(Vector3f.UNIT_Y) <= FastMath.HALF_PI) {
            cam.setAxes(q);
        }
	}
}
