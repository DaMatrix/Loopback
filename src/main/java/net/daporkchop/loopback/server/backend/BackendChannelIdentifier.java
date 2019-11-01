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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.logging.Logging;
import net.daporkchop.loopback.server.Server;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * Identifies the type of backend channel for incoming connections.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@ChannelHandler.Sharable
public final class BackendChannelIdentifier extends ChannelInboundHandlerAdapter {
    @NonNull
    protected final Server server;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (!(msg instanceof ByteBuf)) {
                ctx.channel().attr(ATTR_LOG).get().error("Received invalid message type: %s", msg == null ? "null" : msg.getClass().getCanonicalName());
                ctx.channel().close();
                return;
            }

            ByteBuf buf = (ByteBuf) msg;
            if (buf.readableBytes() != PASSWORD_BYTES && buf.readableBytes() != PASSWORD_BYTES + 8) {
                ctx.channel().attr(ATTR_LOG).get().error("Identification message is only %d bytes long!", buf.readableBytes());
                ctx.channel().close();
                return;
            }

            byte[] password = this.server.password();
            for (int i = 0; i < PASSWORD_BYTES; i++)    {
                if (buf.getByte(i) != password[i])  {
                    ctx.channel().attr(ATTR_LOG).get().error("Invalid password!");
                    ctx.channel().close();
                    return;
                }
            }

            switch (buf.readableBytes())    {
                case PASSWORD_BYTES: //client connection
                    ctx.channel().attr(ATTR_LOG).get().debug("valid password (control)");
                    ctx.channel().pipeline().replace("handle", "handle", new ServerControlHandler(this.server));
                    break;
                case PASSWORD_BYTES + 8:
                    this.server.getControlChannel(buf.getLong(PASSWORD_BYTES)).backendChannel(ctx.channel());
                    ctx.channel().attr(ATTR_LOG).get().debug("valid password+id (data)");
                    break;
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(ATTR_LOG).set(Logging.logger.channel(ctx.channel().remoteAddress().toString()));
        ctx.channel().attr(ATTR_LOG).get().debug("channelActive");

        super.channelActive(ctx);
    }
}
