package net.mosstest.netcommand;

import io.netty.buffer.ByteBuf;

import static io.netty.buffer.Unpooled.*;

/**
 * Created by hexafraction on 8/25/14.
 */
public class ToServerRequestFile extends ToServerCommand{
    // sha256 has 256 bits, 8 bits in a byte
    private static final int HASH_LENGTH = 256 / 8;
    byte[] fileHash;

    /**
     * Both the deserializing constructor and POJO constructor here would have been, ToServerRequestFile(byte[]).
     * Since serialized data is now moving around as ByteBuf's, this is no longer the case.
     * While this may appear to be a problem, it is not, since the packet is the same serialized and deserialized.
     * This packet class is only provided for consistency.
     * The data must be exactly 32 bytes (256 bits) to match the hash length for a SHA-256 hash.
     */
    public ToServerRequestFile(byte[] fileHash) {
        if(fileHash.length!=(HASH_LENGTH))
        this.fileHash = fileHash;
    }

    /**
     * This constructor is provided for deserialization. The caller is responsible for freeing <code>fileHash</code>.
     * The data must be exactly 32 bytes (256 bits) to match the hash length for a SHA-256 hash.
     * @param fileHash
     */
    public ToServerRequestFile(ByteBuf fileHash) throws MalformedPacketException {
        if(fileHash.readableBytes()!=(HASH_LENGTH))
            throw new MalformedPacketException(Messages.getString("HASH_WRONG_LENGTH"));
        this.fileHash = fileHash.array();
    }

    @Override
    public ByteBuf toBytes() {
        return unmodifiableBuffer(wrappedBuffer(fileHash));
    }
}
