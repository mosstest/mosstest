package net.mosstest.servercore;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;// TODO: Auto-generated Javadoc
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.AttributeKey;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.List;

/**
 * Server networking manager. Now using Netty.
 *
 * @author rarkenin
 */
public class ServerNetworkingManager {
    private static final Logger logger = Logger.getLogger(ServerNetworkingManager.class);
    private final MossWorld world;
    private final int port;

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
                            // is passing sch to the SNVMessageHandler hacky? Could I get a context later?
                            sch.pipeline().addLast(new StreamToPacketDecoder());
                            sch.pipeline().addLast(new PacketToPojoDecoder());
                            sch.pipeline().addLast(new ApplicationLevelMessageHandler(sch));
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

    //TODO this class
    private class ApplicationLevelMessageHandler extends ChannelHandlerAdapter {
        public ApplicationLevelMessageHandler(SocketChannel sch) {
            ServerSession sess = new ServerSession();
            sch.attr(AttributeKey.<ServerSession>valueOf("session")).set(sess);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
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

        protected StreamToPacketDecoder() {
            super(DecoderState.READ_LENGTH);


        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            switch (state()) {
                case READ_MAGIC:
                    int magic = in.readInt();
                    if (magic != CommonNetworking.magic) {
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


        private class RawMossPacket {
            int command;
            ByteBuf frame;
            public RawMossPacket(int command, ByteBuf frame) {
                this.command = command;
                this.frame = frame;
            }
        }
    }

    public static void main(String[] args) {
        ServerNetworkingManager snm = new ServerNetworkingManager(16511, null);
        snm.start();
    }

    private class PacketToPojoDecoder extends ChannelHandlerAdapter {

    }
}
