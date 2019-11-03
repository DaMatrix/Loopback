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

package net.daporkchop.loopback;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.SctpChannel;
import io.netty.channel.sctp.SctpMessage;
import io.netty.channel.sctp.nio.NioSctpChannel;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import io.netty.util.ReferenceCountUtil;
import lombok.NonNull;
import net.daporkchop.lib.logging.Logging;
import net.daporkchop.loopback.util.Addr;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class Test {
    public static void startClient(@NonNull Addr addr) {
        Channel channel = new Bootstrap().group(new NioEventLoopGroup(1))
                .channelFactory(NioSctpChannel::new)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SctpChannel>() {
                    @Override
                    protected void initChannel(SctpChannel ch) throws Exception {
                        ch.pipeline().addLast("handle", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                Logging.logger.info("New connection from %s", ctx.channel().remoteAddress());

                                ctx.channel().writeAndFlush(new SctpMessage(0, 0, Unpooled.wrappedBuffer("Hello World!".getBytes())));

                                super.channelActive(ctx);
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                try {
                                    Logging.logger.info("New message from %s: %s", ctx.channel().remoteAddress(), msg);

                                    ctx.channel().close();
                                } finally {
                                    ReferenceCountUtil.release(msg);
                                }
                            }
                        });
                    }
                })
                .connect(addr.host(), addr.port()).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        Logging.logger.success("Connected to %s!", addr);
                    } else {
                        Logging.logger.error("Couldn't connect to server...");
                        f.channel().close().addListener(f1 -> System.exit(addr.port()));
                    }
                }).channel();

        channel.closeFuture().syncUninterruptibly();
    }

    public static void startServer(int port) {
        Channel channel = new ServerBootstrap().group(new NioEventLoopGroup(1))
                .channelFactory(NioSctpServerChannel::new)
                .childHandler(new ChannelInitializer<SctpChannel>() {
                    @Override
                    protected void initChannel(SctpChannel ch) throws Exception {
                        ch.pipeline().addLast("handle", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                Logging.logger.info("New connection from %s", ctx.channel().remoteAddress());

                                super.channelActive(ctx);
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                try {
                                    if (msg instanceof SctpMessage) ctx.channel().writeAndFlush(new SctpMessage(1, 0, ((SctpMessage) msg).content().copy()));

                                    Logging.logger.info("New message from %s: %s", ctx.channel().remoteAddress(), msg);
                                } finally {
                                    ReferenceCountUtil.release(msg);
                                }
                            }
                        });
                    }
                })
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .bind(port).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        Logging.logger.success("Bound to port %d!", port);
                    } else {
                        Logging.logger.error("Couldn't bind to port...");
                        if (f.cause() != null) Logging.logger.alert(f.cause());
                        f.channel().close().addListener(f1 -> System.exit(port));
                    }
                }).channel();

        channel.closeFuture().syncUninterruptibly();
    }
}
