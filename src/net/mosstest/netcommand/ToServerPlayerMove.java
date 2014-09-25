package net.mosstest.netcommand;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.buffer;
/**
 * Created by hexafraction on 8/29/14.
 */
public class ToServerPlayerMove extends ToServerCommand{
    public static final int COMMAND_ID = 0x04;

    public final double deltaX, deltaY, deltaZ, velocityX, velocityY, velocityZ;


    public ToServerPlayerMove(double deltaX,
                              double deltaY,
                              double deltaZ,
                              double velocityX,
                              double velocityY,
                              double velocityZ) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }

    /**
     * Deserializing constructor. Does not explicitly release buf.
     * @param buf
     */
    public ToServerPlayerMove(ByteBuf buf){
        this.deltaX = buf.readDouble();
        this.deltaY = buf.readDouble();
        this.deltaZ = buf.readDouble();
        this.velocityX = buf.readDouble();
        this.velocityY = buf.readDouble();
        this.velocityZ = buf.readDouble();
    }

    @Override
    public ByteBuf toBytes() {
        // 48 bytes comprised of 6 double values
        ByteBuf buf = buffer(48);
        buf.writeDouble(deltaX)
                .writeDouble(deltaY)
                .writeDouble(deltaZ)
                .writeDouble(velocityX)
                .writeDouble(velocityY)
                .writeDouble(velocityZ);
        return buf;


    }
}
