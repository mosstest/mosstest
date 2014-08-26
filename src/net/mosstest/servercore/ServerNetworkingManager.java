package net.mosstest.servercore;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;// TODO: Auto-generated Javadoc
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;
import net.mosstest.netcommand.ToServerHello;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.List;

/**
 * Server networking manager. Now using Netty.
 *
 * @author hexafraction
 */
public class ServerNetworkingManager {
    private static final Logger logger = Logger.getLogger(ServerNetworkingManager.class);
    // Surpressed for now, as we don't use the field yet.
    @SuppressWarnings("FieldCanBeLocal")
    private final MossWorld world;
    private final int port;

    // TODO actually use this class
    @SuppressWarnings("WeakerAccess")
    public ServerNetworkingManager(int port, MossWorld world) {
        this.port = port;
        this.world = world;
    }

    public void start() {
        EventLoopGroup acceptGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrapper = new ServerBootstrap();
            bootstrapper.group(acceptGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel sch) throws Exception {
                            logger.info(MessageFormat.format(Messages.getString("SCH_INITIALIZED"), sch.remoteAddress()));

                            /*
                             * The following subsystem handlers will be included:
                             * Connection state
                             * Event processor actions
                             * Relayable actions (e.g. player move)
                             * File requests
                             * Script requests
                             */
                            sch.pipeline()
                                    .addLast(new StreamToPacketDecoder())
                                    .addLast(new ConnectionStateMessageDecoder());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = bootstrapper.bind(port).sync();
            logger.info(Messages.getString("BIND_SUCCESS"));
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.fatal(Messages.getString("INTERRUPTED_BIND_PORT"));
        } finally {
            acceptGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public enum DecoderState {
        READ_LENGTH,
        READ_CONTENT,
        READ_COMMAND,
        READ_MAGIC
    }

    private class StreamToPacketDecoder extends ReplayingDecoder<DecoderState> {
        int length;
        int command;

        StreamToPacketDecoder() {
            super(DecoderState.READ_LENGTH);


        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            switch (state()) {
                case READ_MAGIC:
                    int magic = in.readInt();
                    if (magic != CommonNetworking.MAGIC) {
                        logger.error(MessageFormat.format(Messages.getString("BAD_MAGIC_RMT"), ctx.channel().remoteAddress()));
                        ctx.close();
                    }
                    checkpoint(DecoderState.READ_LENGTH);
                    // Deliberate lack of break
                case READ_LENGTH:
                    length = in.readInt();
                    checkpoint(DecoderState.READ_COMMAND);
                    // deliberate lack of a break
                case READ_COMMAND:
                    command = in.readUnsignedByte();
                    checkpoint(DecoderState.READ_CONTENT);
                    // no break
                case READ_CONTENT:
                    ByteBuf frame = in.readBytes(length);
                    out.add(new RawMossPacket(command, frame));
                    checkpoint(DecoderState.READ_MAGIC);
            }
        }


    }

    private class RawMossPacket {
        // these should really be final for immutability.

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RawMossPacket that = (RawMossPacket) o;

            if (command != that.command) return false;
            if (!frame.equals(that.frame)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = command;
            result = 31 * result + frame.hashCode();
            return result;
        }

        final int command;
        @NotNull
        final ByteBuf frame;

        public RawMossPacket(int command, @NotNull ByteBuf frame) {
            this.command = command;
            this.frame = frame;
        }
    }

    public static void main(String[] args) {
        // test only
        ServerNetworkingManager snm = new ServerNetworkingManager(16511, null);
        snm.start();
    }

    /**
     * Handles connection state messages and outputs their respective POJOs.
     */
    private class ConnectionStateMessageDecoder extends MessageToMessageDecoder<RawMossPacket> {

        @Override
        public boolean acceptInboundMessage(Object msg) throws Exception {
            if ((msg instanceof RawMossPacket)) {
                if (((RawMossPacket) msg).command == ToServerHello.COMMAND_ID)
                    return true;
            }
            return false;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, RawMossPacket msg, List<Object> out) throws Exception {
            switch(msg.command){
                case ToServerHello.COMMAND_ID:
                    ToServerHello cmd = new ToServerHello(msg.frame);
                    out.add(cmd);
                    break;
            }
            msg.frame.release();
        }


    }
}
