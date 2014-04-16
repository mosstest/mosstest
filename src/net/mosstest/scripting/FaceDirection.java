package net.mosstest.scripting;

public class FaceDirection {
    /**
     * The how much to yaw (rotate about the vertical axis) the node's face direction. 0 represents front toward world yaw 0 (north). FIXME doc
     */
    public final byte yaw;
    public final byte pitch;
    public final byte roll;

    public FaceDirection(byte yaw, byte pitch, byte roll) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }
}
