package net.mosstest.netcommand;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.*;

/**
 * Created by hexafraction on 8/25/14.
 */
public class ToServerAuth extends ToServerCommand {
    final byte[] authData;
public static final int COMMAND_ID=0x02;
    /**
     * Both the deserializing constructor and POJO constructor here would have been, ToServerAuth(byte[]).
     * Since serialized data is now moving around as ByteBuf's, this is no longer the case.
     * While this may appear to be a problem, it is not, since the packet is the same serialized and deserialized.
     * This packet class is only provided for consistency.
     *
     */
    public ToServerAuth(byte[] authData) {
        this.authData = authData;
    }

    /**
     * This constructor is provided for deserialization. The caller is responsible for freeing <code>authData</code>.
     * @param authData The authentication data, whose semantics is defined at a higher level than this class.
     */
    public ToServerAuth(ByteBuf authData) {
        this.authData = authData.array();
    }

    @Override
    public ByteBuf toBytes() {
        return unmodifiableBuffer(wrappedBuffer(authData));
    }

    public byte[] getAuthData() {
        return authData;
    }
}
