package net.mosstest.scripting;

import net.mosstest.servercore.serialization.IByteArrayWritable;

public abstract class AbstractMapChunk implements IByteArrayWritable {
    int CHUNK_DIMENSION = 16;

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract int getNodeId(byte x, byte y, byte z);

    public abstract void setNode(byte x, byte y, byte z, short node);

    public abstract int[][][] getNodes();

    public abstract byte[] writeLight(boolean compressed);

    public abstract void compact();
}
