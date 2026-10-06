package io.quarkus.netty.runtime;

import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.quarkus.core.StartContext;
import io.quarkus.netty.util.JBossNettyLoggerFactory;

public class NettyRecorder {

    public static void setNettyLoggerFactory() {
        InternalLoggerFactory.setDefaultFactory(new JBossNettyLoggerFactory());
    }

    public static EventLoopGroup createEventLoop(StartContext ctx, int nThreads) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(nThreads);
        ctx.onStop(() -> eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS));
        return eventLoopGroup;
    }
}
