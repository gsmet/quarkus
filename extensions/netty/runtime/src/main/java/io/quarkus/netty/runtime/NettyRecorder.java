package io.quarkus.netty.runtime;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.quarkus.core.StartContext;
import io.quarkus.netty.util.JBossNettyLoggerFactory;

public class NettyRecorder {

    private static final Logger log = Logger.getLogger(NettyRecorder.class);

    public static void setNettyLoggerFactory() {
        InternalLoggerFactory.setDefaultFactory(new JBossNettyLoggerFactory());
    }

    /**
     * Clear the JVM-internal backtrace from exceptions stored in Netty's
     * {@code PlatformDependent} and {@code PlatformDependent0} static fields.
     * <p>
     * When Unsafe is unavailable (e.g. JDK 25+), these classes store
     * {@code UnsupportedOperationException} instances whose backtrace captures
     * class references from the call stack at creation time. Since these are
     * parent-first classes on the AppCL, the backtrace permanently pins
     * whichever per-test classloader was on the stack during initialization.
     * <p>
     * We clear the backtrace via reflection on {@code Throwable.backtrace}
     * (JVM-internal field). {@code setStackTrace(new StackTraceElement[0])}
     * alone is not sufficient as it only clears the public stack trace, not
     * the internal {@code Object[]} that holds class references for GC purposes.
     */
    public static void clearUnsafeUnavailabilityCauseBacktrace() {
        clearBacktraceChain(PlatformDependent.getUnsafeUnavailabilityCause());
        clearStaticThrowableFields("io.netty.util.internal.PlatformDependent0",
                "EXPLICIT_NO_UNSAFE_CAUSE", "UNSAFE_UNAVAILABILITY_CAUSE");
        clearStaticThrowableFields("org.h2.message.DbException", "OOME");
    }

    private static void clearStaticThrowableFields(String className, String... fieldNames) {
        try {
            Class<?> clazz = Class.forName(className, true, ClassLoader.getSystemClassLoader());
            for (String fieldName : fieldNames) {
                try {
                    Field f = clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object value = f.get(null);
                    if (value instanceof Throwable) {
                        clearBacktraceChain((Throwable) value);
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            log.debug("Failed to clear backtraces on " + className, e);
        }
    }

    private static void clearBacktraceChain(Throwable throwable) {
        while (throwable != null) {
            clearBacktrace(throwable);
            throwable = throwable.getCause();
        }
    }

    private static void clearBacktrace(Throwable throwable) {
        throwable.setStackTrace(new StackTraceElement[0]);
        try {
            Field backtrace = Throwable.class.getDeclaredField("backtrace");
            backtrace.setAccessible(true);
            backtrace.set(throwable, null);
        } catch (Exception ignored) {
        }
    }

    public static EventLoopGroup createEventLoop(StartContext ctx, int nThreads) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(nThreads);
        ctx.onStop(() -> eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS));
        return eventLoopGroup;
    }
}
