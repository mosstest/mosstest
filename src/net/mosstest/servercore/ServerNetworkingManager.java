package net.mosstest.servercore;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import net.mosstest.netcommand.*;
import net.mosstest.scripting.authentication.AccessDenied;
import net.mosstest.scripting.authentication.AuthChallenge;
import net.mosstest.scripting.authentication.MossAuthenticator;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ConnectException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Server networking manager. Now using Netty.
 *
 * @author hexafraction
 */
@ThreadSafe
public class ServerNetworkingManager {
    public static final int PROTOCOL_VERSION = 1;
    public static final int SERVER_SCRIPTAPI_VERSION = 1;

    private static final Logger logger = Logger.getLogger(ServerNetworkingManager.class);
    // Surpressed for now, as we don't use the field yet.
    @SuppressWarnings("FieldCanBeLocal")
    private final MossWorld world;
    private final int port;

    @GuardedBy("lock")
    private EventLoopGroup acceptGroup, workerGroup;

    @GuardedBy("lock")
    private ServerBootstrap bootstrapper;


    private final AtomicBoolean alreadyRunning = new AtomicBoolean(false);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @GuardedBy("lock")
    private Channel primaryChannel;

    // TODO actually use this class
    @SuppressWarnings("WeakerAccess")
    public ServerNetworkingManager(int port, MossWorld world) {
        this.port = port;
        this.world = world;
    }


    public void start() throws ConnectException {
        lock.writeLock().lock();
        try {
            boolean alreadyRunning = this.alreadyRunning.getAndSet(true);
            if (alreadyRunning) {
                throw new ConnectException("The server network process has already been started and cannot be started again.");
            }
            acceptGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            try {
                bootstrapper = new ServerBootstrap();
                // Once pending buffer is up to 32K, hold off until it drops to 8K or less.
                // TODO benchmarks over LAN and WAN to find optimal values.
                // Client might try to request settings based on the address used to contact the server
                // (e.g. 192.168.0.0/16 uses a LAN-style limit).
                bootstrapper.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
                bootstrapper.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);
                bootstrapper.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
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
                                        .addLast(new ConnectionStateMessageDecoder())
                                        .addLast(new ToServerHelloHandler());
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);
                ChannelFuture f = bootstrapper.bind(port).sync();
                primaryChannel = f.channel();
                logger.info(Messages.getString("BIND_SUCCESS"));

            } catch (InterruptedException e) {
                logger.fatal(Messages.getString("INTERRUPTED_BIND_PORT"));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Prevents a join() while shutting down. This lock should be fair.
    private final ReentrantLock joinLock = new ReentrantLock(true);


    public void join() throws InterruptedException {
        if (joinLock.isLocked()) {
            // A shutdown is already in progress. Let's try to acquire the lock, so we can return after shutdown is complete.
            joinLock.lock();
            joinLock.unlock();
        }
        // If we get to here, then there isn't a shutdown in progress. We can just monitor the channel.
        if (!alreadyRunning.get()) {
            return;
        }
        primaryChannel.closeFuture().sync();

        // The stop() call will shutdown loops.
    }

    private void shutdownLoopsGracefully() throws IOException, InterruptedException {

        if (!alreadyRunning.get()) {
            throw new IOException("The server process is not running.");
        }
        acceptGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
    }

    public void stop() throws IOException, InterruptedException {
        lock.writeLock().lock();
        try {
            if (!alreadyRunning.get()) {
                throw new IOException("The server process is not running.");
            }
            alreadyRunning.set(false);
            shutdownLoopsGracefully();

        } finally {
            lock.writeLock().unlock();
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

    public static void main(String[] args) throws ConnectException {
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
            switch (msg.command) {
                case ToServerHello.COMMAND_ID:
                    ToServerHello helloCmd = new ToServerHello(msg.frame);
                    out.add(helloCmd);
                    break;
                case ToServerAuth.COMMAND_ID:
                    ToServerAuth authCmd = new ToServerAuth(msg.frame);
                    out.add(authCmd);
                    break;
                case ToServerFileRequest.COMMAND_ID:
                    ToServerFileRequest fileRequestCmd = new ToServerFileRequest(msg.frame);
                    out.add(fileRequestCmd);
            }

            // command pojo constructors do not free the buffer themselves
            msg.frame.release();
        }


    }

    private class ToServerHelloHandler extends MessageToMessageDecoder<ToServerHello> {

        @Override
        protected void decode(ChannelHandlerContext ctx, ToServerHello msg, List<Object> out) throws Exception {
            // We have a ToServerHello here. Let's call a helper method to get
            // the connection state object for this connection.
            ServerConnectionState st = getConnectionState(ctx).get();
            if (st != null) {
                // the connection state MUST have been null, since a HELLO must be the first message sent.
                // Something went wrong.
                logger.error(Messages.getString("STRAY_HELLO"));
                ToClientErrorOccurred response = new ToClientErrorOccurred(Messages.getString("STRAY_HELLO"));
                ctx.writeAndFlush(response).sync();
                ctx.close().sync();
            } else {
                synchronized (getConnectionState(ctx)) {
                    ServerConnectionState newState = new ServerConnectionState();
                    getConnectionState(ctx).set(newState);
                    MossAuthenticator authenticator = world.getMossEnv().getAuthenticator(msg.getUsername());
                    AuthChallenge challenge = authenticator.getChallenge(msg.getUsername());
                    newState.setChallenge(challenge);
                    ToClientAuthRequested response = new ToClientAuthRequested(challenge.type, challenge.challenge);
                    // we want to flush as this packet should be seen on its own by the remote endpoint.
                    ctx.writeAndFlush(response);
                }
            }
        }
    }

    private class ToServerAuthHandler extends MessageToMessageDecoder<ToServerAuth> {

        @Override
        protected void decode(ChannelHandlerContext ctx, ToServerAuth msg, List<Object> out) throws Exception {
            ServerConnectionState st = getConnectionState(ctx).get();
            assertStateNotNull(st, ctx);
            AuthChallenge challenge = st.getChallenge();
            MossAuthenticator authenticator = world.getMossEnv().getAuthenticator(challenge.username);
            try {
                authenticator.checkLogon(challenge, msg.getAuthData());
            } catch (AccessDenied e){
                ToClientAuthDenied response = new ToClientAuthDenied(e.reason);
                ctx.writeAndFlush(response);
                ctx.close();
            }
        }
    }

    private void assertStateNotNull(ServerConnectionState st, ChannelHandlerContext ctx) throws InterruptedException {
        if(st==null){
            logger.error(Messages.getString("MISSING_HELLO"));
            ToClientErrorOccurred response = new ToClientErrorOccurred(Messages.getString("MISSING_HELLO"));
            ctx.writeAndFlush(response).sync();
            ctx.close().sync();
            throw new BadConnectionStateException(Messages.getString("MISSING_HELLO"));

        }
    }

    private static final AttributeKey<ServerConnectionState> CONN_STATE_KEY
            = AttributeKey.<ServerConnectionState>valueOf(ServerConnectionState.class, "CONN_STATE");

    private static Attribute<ServerConnectionState> getConnectionState(ChannelHandlerContext ctx) {
        return ctx.channel().attr(CONN_STATE_KEY);
    }


}
