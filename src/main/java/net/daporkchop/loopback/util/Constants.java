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

package net.daporkchop.loopback.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.logging.Logger;
import net.daporkchop.lib.logging.Logging;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class Constants {
    public final long SERVER_CONNECTION_TIMEOUT = 10000L;

    public final EventLoopGroup GROUP = Epoll.isAvailable()
            ? new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors())
            : new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());

    public final ChannelFactory<Channel>       CLIENT_CHANNEL_FACTORY = Epoll.isAvailable() ? EpollSocketChannel::new : NioSocketChannel::new;
    public final ChannelFactory<ServerChannel> SERVER_CHANNEL_FACTORY = Epoll.isAvailable() ? EpollServerSocketChannel::new : NioServerSocketChannel::new;

    public final AttributeKey<Channel>    ATTR_PAIR  = AttributeKey.newInstance("loopback_pair");
    public final AttributeKey<Long>       ATTR_ID    = AttributeKey.newInstance("loopback_id");
    public final AttributeKey<Logger>     ATTR_LOG   = AttributeKey.newInstance("loopback_log");
    public final AttributeKey<Boolean>    ATTR_BOUND = AttributeKey.newInstance("loopback_bound");
    public final AttributeKey<AtomicLong> ATTR_IDLE  = AttributeKey.newInstance("loopback_idle");

    public final ChannelFutureListener DO_READ_HANDLER = future -> future.channel().attr(ATTR_PAIR).get().read();
    public final ChannelMatcher TIMEOUT_MATCHER = channel -> channel.hasAttr(ATTR_IDLE) && channel.attr(ATTR_IDLE).get().get() >= System.currentTimeMillis() + SERVER_CONNECTION_TIMEOUT;

    public final Logger DEFAULT_CHANNEL_LOGGER = Logging.logger.channel("Unknown Channel");

    public final int PASSWORD_BYTES = 256 >>> 3; // sha256 is 256 bits long

    public final int COMMAND_OPEN  = 0;
    public final int COMMAND_CLOSE = 1;

    public final int CLIENT_READY_SOCKETS = 3; //TODO: implement this

    @SuppressWarnings("deprecation")
    public void bindChannels(@NonNull Channel backend, @NonNull Channel incoming) {
        synchronized (backend) {
            synchronized (incoming) {
                backend.attr(ATTR_BOUND).set(Boolean.TRUE);
                incoming.attr(ATTR_BOUND).set(Boolean.TRUE);
                backend.attr(ATTR_PAIR).set(incoming);
                incoming.attr(ATTR_PAIR).set(backend);
                backend.read();
                incoming.read();
            }
        }
    }
}
