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

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.NonNull;
import net.daporkchop.loopback.server.Server;
import net.daporkchop.loopback.server.ServerChannelInitializer;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * @author DaPorkchop_
 */
public final class BackendChannelInitializer extends ServerChannelInitializer {
    private static final SslContext CONTEXT;

    static {
        SslContext context = null;
        try {
            SelfSignedCertificate cert = new SelfSignedCertificate();
            cert.delete();
            context = SslContextBuilder.forServer(cert.key(), cert.cert()).build();
        } catch (CertificateException | SSLException e) {
            throw new RuntimeException(e);
        } finally {
            CONTEXT = context;
        }
    }

    protected final BackendChannelIdentifier identifier;

    public BackendChannelInitializer(@NonNull Server server) {
        super(server);

        this.identifier = new BackendChannelIdentifier(server);
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        super.initChannel(channel);

        channel.pipeline()
                .addLast("ssl", new SslHandler(CONTEXT.newEngine(channel.alloc())))
                .addLast("handle", this.identifier);
    }
}
