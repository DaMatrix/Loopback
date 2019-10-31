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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.loopback.server.Server;

import static net.daporkchop.loopback.util.Constants.PASSWORD_BYTES;

/**
 * Identifies the type of management channel for incoming connections.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class BackendChannelIdentifier extends ChannelInboundHandlerAdapter {
    @NonNull
    protected final Server server;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (!(msg instanceof ByteBuf)) throw new IllegalArgumentException(msg == null ? "null" : msg.getClass().getCanonicalName());

            ByteBuf buf = (ByteBuf) msg;
            if (buf.readableBytes() != PASSWORD_BYTES + 1) throw new IllegalArgumentException(String.format("Identification message is only %d bytes long!", buf.readableBytes()));

            byte[] password = this.server.password();
            for (int i = 0; i < PASSWORD_BYTES; i++)    {
                if (buf.getByte(i) != password[i])  {
                    throw new IllegalArgumentException("Invalid password!");
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}