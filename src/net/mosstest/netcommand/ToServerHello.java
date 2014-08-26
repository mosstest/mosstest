package net.mosstest.netcommand;

import io.netty.buffer.ByteBuf;
import net.mosstest.servercore.MosstestFatalDeathException;
import net.mosstest.util.BufferUtilities;
import org.apache.log4j.Logger;

import java.text.MessageFormat;

import static io.netty.buffer.Unpooled.buffer;

/**
 * Created by hexafraction on 4/27/14.
 */
public class ToServerHello extends ToServerCommand {
    public static final int COMMAND_ID = 0x01;
    private static final Logger logger = Logger.getLogger(ToServerHello.class);
    private final String username;
    private final int protocolVersion;
    private final int minScriptApi;
    private final int maxScriptApi;

    /**
     * Constructs a ToServerHello POJO by deserializing the contents of a bytebuffer.
     * The caller is responsible for freeing the buffer.
     *
     * @param buf The ByteBuf whose contents should be deserialized.
     * @throws MalformedPacketException if the packet could not be deserialized properly.
     */
    public ToServerHello(ByteBuf buf) throws
            MalformedPacketException {
        try {
            this.protocolVersion = buf.readUnsignedShort();
            this.minScriptApi = buf.readUnsignedShort();
            this.maxScriptApi = buf.readUnsignedShort();
        } catch (IndexOutOfBoundsException e) {
            // apparently we overran the end of the actual packet while trying to parse it. Let's raise an exception to
            // notify the caller (e.g. so they can disconnect the peer)
            throw new MalformedPacketException(MessageFormat.format(Messages.getString("PACKET_TOO_SHORT"), "ToServerHello"), e);
        }
        try {
            this.username = BufferUtilities.readUTF(buf);
        } catch (IndexOutOfBoundsException e) {
            throw new MalformedPacketException(MessageFormat.format(Messages.getString("STRING_READ_FAILED"), "ToServerHello"), e);
        }
        if (buf.readableBytes() > 0) {
            // apparently the packet is longer than we expect. It's unsafe to assume that all will be fine.
            // Either the structure changed in a future version, or a string read failure occured.
            throw new MalformedPacketException(MessageFormat.format(Messages.getString("PACKET_TOO_LONG"), "ToServerHello"));
        }

        // The caller is responsible for releasing buf.
    }

    public ToServerHello(String username, int protocolVersion, int minScriptApi, int maxScriptApi) {
        this.username = username;
        this.protocolVersion = protocolVersion;
        this.minScriptApi = minScriptApi;
        this.maxScriptApi = maxScriptApi;
    }

    @Override
    public ByteBuf toBytes() {
        // conservative initial size, as only one of these should actually be constructed per game session.
        ByteBuf buf = buffer(8);
        try {
            buf.writeShort(protocolVersion);
            buf.writeShort(minScriptApi);
            buf.writeShort(maxScriptApi);

            BufferUtilities.writeUTF(buf, this.username);
        } catch (IndexOutOfBoundsException e) {
            // this shouldn't occur, the buffer should be able to grow as needed.
            throw new MosstestFatalDeathException(MessageFormat.format(Messages.getString("BUF_CONSTR_FAIL"), "ToServerHello"), e);
        }
        return buf;

    }
}