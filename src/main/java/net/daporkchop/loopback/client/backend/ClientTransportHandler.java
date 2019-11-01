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

package net.daporkchop.loopback.client.backend;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.loopback.client.Client;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@ChannelHandler.Sharable
public final class ClientTransportHandler extends ChannelInboundHandlerAdapter {
    @NonNull
    protected final Client client;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            if (evt == SslHandshakeCompletionEvent.SUCCESS) {
                ctx.channel().attr(ATTR_LOG).get().debug("ssl handshake success (data)");
                ctx.channel().writeAndFlush(ctx.alloc().ioBuffer(PASSWORD_BYTES + 8)
                        .writeBytes(this.client.password())
                        .writeLong(this.client.controlChannel().attr(ATTR_ID).get()));
                ctx.channel().writeAndFlush(ctx.alloc().ioBuffer(8)
                        .writeLong(ctx.channel().attr(ATTR_ID).getAndSet(null)));
            } else {
                ctx.channel().attr(ATTR_LOG).get().alert(((SslHandshakeCompletionEvent) evt).cause());
            }
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!ctx.channel().hasAttr(ATTR_PAIR)) throw new IllegalStateException();

        ctx.channel().attr(ATTR_PAIR).get().writeAndFlush(msg).addListener(DO_READ_HANDLER);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().hasAttr(ATTR_PAIR)) ctx.channel().attr(ATTR_PAIR).get().close();
        ctx.channel().attr(ATTR_LOG).get().debug("connection closed (data)");

        super.channelUnregistered(ctx);
    }
}
