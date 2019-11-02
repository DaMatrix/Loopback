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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.loopback.server.backend.BackendChannelInitializer;
import net.daporkchop.loopback.server.backend.ServerControlHandler;
import net.daporkchop.loopback.util.Endpoint;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class Server implements Endpoint {
    protected ServerChannel backendListener;
    protected ChannelGroup  allChannels;

    protected LongObjectMap<ServerControlHandler> controlChannelsById;

    @Getter
    @NonNull
    protected final byte[] password;

    @Override
    public synchronized void start() {
        if (this.backendListener != null || this.allChannels != null) throw new IllegalStateException();

        this.allChannels = new DefaultChannelGroup(GROUP.next());
        this.controlChannelsById = new LongObjectHashMap<>();

        this.backendListener = (ServerChannel) new ServerBootstrap().group(GROUP)
                .channelFactory(SERVER_CHANNEL_FACTORY)
                .childHandler(new BackendChannelInitializer(this))
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childAttr(ATTR_LOG, DEFAULT_CHANNEL_LOGGER)
                .bind(59989).syncUninterruptibly().channel();
        this.allChannels.add(this.backendListener);
    }

    @Override
    public synchronized Future<Void> close() {
        if (this.backendListener == null || this.allChannels == null) throw new IllegalStateException();

        this.controlChannelsById = null;

        //close the channel group, the future will not be completed until every single channel has been closed
        return this.allChannels.close().addListener(f -> {
            this.allChannels = null;
            this.backendListener = null;
        });
    }

    public synchronized long addControlChannel(@NonNull ServerControlHandler channel) {
        if (this.backendListener == null || this.allChannels == null) throw new IllegalStateException();

        Random r = ThreadLocalRandom.current();
        long l;
        while (this.controlChannelsById.containsKey(l = r.nextLong())) ;
        this.controlChannelsById.put(l, channel);
        channel.channel().closeFuture().addListener(future -> this.controlChannelsById.remove(channel.id(), channel));
        return l;
    }

    public synchronized ServerControlHandler getControlChannel(long id) {
        if (this.backendListener == null || this.allChannels == null) throw new IllegalStateException();

        ServerControlHandler handler = this.controlChannelsById.get(id);
        if (handler == null) throw new IllegalArgumentException(Long.toUnsignedString(id));
        return handler;
    }
}
