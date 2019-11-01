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
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.loopback.client.backend.BackendChannelInitializerClient;
import net.daporkchop.loopback.util.Endpoint;

import java.net.InetSocketAddress;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public final class Client implements Endpoint {
    public static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("localhost", 59989);

    protected ChannelGroup channels;
    protected Bootstrap bootstrap;

    @Getter(AccessLevel.NONE)
    private volatile SocketChannel controlChannel;

    @Override
    public synchronized void start() {
        if (this.channels != null) throw new IllegalStateException();

        this.channels = new DefaultChannelGroup(GROUP.next(), true);

        this.bootstrap = new Bootstrap().group(GROUP)
                .channelFactory(CLIENT_CHANNEL_FACTORY)
                .handler(new BackendChannelInitializerClient(this))
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .remoteAddress(SERVER_ADDRESS);

        this.bootstrap.connect().syncUninterruptibly();
    }

    @Override
    public synchronized Future<Void> close() {
        if (this.channels == null) throw new IllegalStateException();

        this.bootstrap = null;

        return this.channels.close().addListener(f -> this.channels = null);
    }
}
