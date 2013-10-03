package net.mosstest.servercore;

public class MossRenderAddAssetPath extends MossRenderEvent {
	String path;
	public MossRenderAddAssetPath (String newpath) {
		path = newpath;
	}
	public String getPath () {
		return path;
	}
}
