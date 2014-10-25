package net.mosstest.renderer;

import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class LightingManager {
	public static SpotLight getFlashlight (Vector3f loc, Vector3f dir, float range) {
		SpotLight light = new SpotLight();
		light.setSpotRange(300f);
		light.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD);
		light.setSpotOuterAngle(35f * FastMath.DEG_TO_RAD);
		light.setColor(ColorRGBA.White.mult(3f));
		light.setPosition(loc);
		light.setDirection(dir);
		return light;
	}
	
	public static DirectionalLight getDirectionalLight (ColorRGBA color, Vector3f dir) {
		DirectionalLight light = new DirectionalLight();
		light.setColor(color);
		light.setDirection(dir.normalizeLocal());
		return light;
	}
	
	public static PointLight getPointLight (ColorRGBA color, float r, Vector3f loc) {
		PointLight light = new PointLight();
		light.setColor(color);
		light.setRadius(r);
		light.setPosition(loc);
		return light;
	}
}
