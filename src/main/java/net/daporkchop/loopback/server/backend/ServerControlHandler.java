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

package net.daporkchop.loopback.server.backend;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.loopback.server.Server;

import java.net.InetSocketAddress;
import java.util.Iterator;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * Handles messages for control channels.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public final class ServerControlHandler extends ChannelInboundHandlerAdapter {
    @NonNull
    protected final Server server;

    protected final ServerBackendTransportHandler transportHandler = new ServerBackendTransportHandler(this);

    private   ChannelGroup waitingChannels;
    protected Channel      channel;
    protected long         id;
    protected long childIdCounter = 0L;

    @Override
    public synchronized void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (this.channel != null) throw new IllegalStateException("Channel already set!");

        this.channel = ctx.channel();
        this.id = this.server.addControlChannel(this);

        this.waitingChannels = new DefaultChannelGroup(this.channel.eventLoop(), true);

        this.channel.writeAndFlush(ctx.alloc().ioBuffer(8).writeLong(this.id)); //send self channel ID to remote server
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        this.waitingChannels.close();
        this.waitingChannels = null;
    }

    public synchronized void backendChannel(@NonNull Channel channel) {
        channel.pipeline().replace("handle", "handle", this.transportHandler);
    }

    public synchronized void backendChannelReady(@NonNull Channel channel, long id) {
        for (Iterator<Channel> iter = this.waitingChannels.iterator(); iter.hasNext(); ) {
            Channel next = iter.next();
            if (next.attr(ATTR_ID).get() == id) {
                this.bindChannels(channel, next);
                iter.remove();
                return;
            }
        }
    }

    public synchronized void incomingChannel(@NonNull Channel channel) {
        long id = this.childIdCounter++;
        channel.attr(ATTR_ID).set(id);
        this.channel.writeAndFlush(this.channel.alloc().ioBuffer(8 + 2)
                        .writeLong(id)
                        .writeShort(((InetSocketAddress) channel.localAddress()).getPort()));

        this.waitingChannels.add(channel);
    }

    protected void bindChannels(@NonNull Channel backend, @NonNull Channel incoming) {
        backend.attr(ATTR_PAIR).set(incoming);
        incoming.attr(ATTR_PAIR).set(backend);
        backend.read();
    }
}
