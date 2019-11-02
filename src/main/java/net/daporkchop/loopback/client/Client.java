/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2019-2019 DaPorkchop_ and contributors
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it. Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.loopback.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.loopback.client.backend.BackendChannelInitializerClient;
import net.daporkchop.loopback.client.target.TargetChannelInitializer;
import net.daporkchop.loopback.util.Addr;
import net.daporkchop.loopback.util.Endpoint;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public final class Client implements Endpoint {
    private static final Pattern PATTERN_ADD_COMMAND = Pattern.compile("^add ([0-9]+) ([^:]+):([0-9]+)$");
    public static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("localhost", 59989);

    @NonNull
    protected final byte[] password;

    protected ChannelGroup channels;
    protected Bootstrap bootstrap;
    protected Bootstrap targetBootstrap;

    private volatile SocketChannel controlChannel;

    //private final Queue<Channel> readyChannels = new ConcurrentLinkedQueue<>();

    private IntObjectMap<Addr> targetAddresses;

    @Override
    public synchronized void start() {
        if (this.channels != null) throw new IllegalStateException();

        this.channels = new DefaultChannelGroup(GROUP.next());
        this.targetAddresses = new IntObjectHashMap<>();

        this.bootstrap = new Bootstrap().group(GROUP)
                .channelFactory(CLIENT_CHANNEL_FACTORY)
                .handler(new BackendChannelInitializerClient(this))
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .attr(ATTR_LOG, DEFAULT_CHANNEL_LOGGER)
                .remoteAddress(SERVER_ADDRESS);

        this.bootstrap.connect().syncUninterruptibly();

        this.targetBootstrap = this.bootstrap.clone()
                .remoteAddress(null)
                .handler(new TargetChannelInitializer(this));
    }

    @Override
    public synchronized Future<Void> close() {
        if (this.channels == null) throw new IllegalStateException();

        this.targetBootstrap = this.bootstrap = null;
        this.targetAddresses = null;

        return this.channels.close().addListener(f -> this.channels = null);
    }

    @Override
    public synchronized boolean handleCommand(@NonNull String command) {
        if (this.channels == null) throw new IllegalStateException();

        Matcher matcher = PATTERN_ADD_COMMAND.matcher(command);
        if (matcher.find()) {
            int sourcePort = Integer.parseInt(matcher.group(1));
            String dstAddress = matcher.group(2);
            int dstPort = Integer.parseInt(matcher.group(3));

            this.targetAddresses.put(sourcePort, new Addr(dstAddress, dstPort));
            this.controlChannel.writeAndFlush(this.controlChannel.alloc().ioBuffer(3).writeByte(COMMAND_OPEN).writeShort(sourcePort));
        }
        return Endpoint.super.handleCommand(command);
    }

    public synchronized void handleConnectionRequest(long remoteId, int srcPort)  {
        Addr dst = this.targetAddresses.get(srcPort);
        if (dst == null) throw new IllegalArgumentException(Integer.toUnsignedString(srcPort));

        //ChannelFuture toServer = this.bootstrap.connect(SERVER_ADDRESS);
        //ChannelFuture toDst = this.targetBootstrap.connect(dst.host(), dst.port());
        this.targetBootstrap.connect(dst.host(), dst.port()).addListener((ChannelFutureListener) dstFuture -> {
            if (dstFuture.isSuccess())  {
                this.bootstrap.connect(SERVER_ADDRESS).addListener((ChannelFutureListener) serverFuture -> {
                    serverFuture.channel().pipeline().get(SslHandler.class).handshakeFuture().addListener(f -> {
                        serverFuture.channel().writeAndFlush(serverFuture.channel().alloc().ioBuffer(8).writeLong(remoteId));
                        bindChannels(dstFuture.channel(), serverFuture.channel());
                    });
                });
            } else {
                //the connection will time out on the server by itself
                System.err.printf("unable to connect to %s!\n", dst);
            }
        });
    }
}
