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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @NonNull
    protected final Server server;

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        this.server.group.add(channel); //add to channel group so that we can bulk-disconnect all channels when we shut down
    }

    /**
     * @author DaPorkchop_
     */
    public static final class SSL extends ServerChannelInitializer  {
        private static final SslContext CONTEXT;

        static {
            SslContext context = null;
            try {
                SelfSignedCertificate cert = new SelfSignedCertificate();
                cert.delete();
                context = SslContextBuilder.forServer(cert.key(), cert.cert()).build();
            } catch (CertificateException | SSLException e)    {
                throw new RuntimeException(e);
            } finally {
                CONTEXT = context;
            }
        }

        public SSL(@NonNull Server server) {
            super(server);
        }

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            super.initChannel(channel);

            channel.pipeline().addLast("ssl", new SslHandler(CONTEXT.newEngine(channel.alloc())));
        }
    }
}
