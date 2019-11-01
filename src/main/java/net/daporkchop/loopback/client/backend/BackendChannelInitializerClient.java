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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.loopback.client.Client;
import net.daporkchop.loopback.client.ClientChannelInitializer;
import net.daporkchop.loopback.common.CommonHandler;

import javax.net.ssl.SSLException;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public final class BackendChannelInitializerClient extends ClientChannelInitializer {
    private static final long CLIENT_CONTROL_CHANNEL_OFFSET = PUnsafe.pork_getOffset(Client.class, "controlChannel");

    private static final SslContext SSL_CONTEXT;

    static {
        SslContext context = null;
        try {
            context = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        } finally {
            SSL_CONTEXT = context;
        }
    }

    protected final ClientTransportHandler transport;

    public BackendChannelInitializerClient(@NonNull Client client) {
        super(client);

        this.transport = new ClientTransportHandler(client);
    }

    @Override
    protected synchronized void initChannel(SocketChannel channel) throws Exception {
        super.initChannel(channel);

        channel.pipeline().addLast("ssl", new SslHandler(SSL_CONTEXT.newEngine(channel.alloc()), false));

        if (PUnsafe.compareAndSwapObject(this.client, CLIENT_CONTROL_CHANNEL_OFFSET, null, channel)) {
            channel.closeFuture().addListener((ChannelFutureListener) f -> PUnsafe.compareAndSwapObject(this.client, CLIENT_CONTROL_CHANNEL_OFFSET, f.channel(), null));
            //the new channel should be a control channel
            channel.attr(ATTR_LOG).get().debug("initChannel (control)");

            channel.config().setAutoRead(true);
            channel.pipeline().addLast("handle", new ClientControlHandler(this.client));
        } else {
            //the new channel should be a normal data channel
            channel.attr(ATTR_LOG).get().debug("initChannel (data)");

            channel.pipeline().addLast("handle", this.transport);
        }

        channel.pipeline().addLast("common", CommonHandler.INSTANCE);
    }
}
