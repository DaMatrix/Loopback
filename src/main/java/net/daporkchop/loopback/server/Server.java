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

package net.daporkchop.loopback.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.daporkchop.loopback.util.Endpoint;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public final class Server implements Endpoint {
    protected ServerChannel channel;
    protected ChannelGroup  group;
    protected Bootstrap     clientBootstrap; //TODO

    @Override
    public synchronized ChannelFuture start() {
        if (this.channel != null || this.group != null) throw new IllegalStateException();

        this.group = new DefaultChannelGroup(GROUP.next());
        return new ServerBootstrap()
                .group(GROUP)
                .channelFactory(SERVER_CHANNEL_FACTORY)
                .childHandler(new ServerChannelInitializer.SSL(this))
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.AUTO_READ, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .bind(59989).addListener((ChannelFutureListener) f -> this.channel = (ServerChannel) f.channel());
    }

    @Override
    public synchronized ChannelFuture close() {
        if (this.channel == null || this.group == null) throw new IllegalStateException();

        this.clientBootstrap = null;

        return this.channel.close().addListener(f -> {
            this.group.close();
            this.group = null;
            this.channel = null;
        });
    }
}
