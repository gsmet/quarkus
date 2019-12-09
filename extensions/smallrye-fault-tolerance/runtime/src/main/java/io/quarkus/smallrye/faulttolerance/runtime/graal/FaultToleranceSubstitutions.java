package io.quarkus.smallrye.faulttolerance.runtime.graal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.concurrent.ManagedThreadFactory;

import org.apache.commons.configuration.AbstractConfiguration;
import org.jboss.logging.Logger;

import com.netflix.config.jmx.ConfigMBean;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class FaultToleranceSubstitutions {
}

@TargetClass(className = "com.netflix.config.jmx.ConfigJMXManager")
final class Target_com_netflix_config_jmx_ConfigJMXManager {

    @Substitute
    public static ConfigMBean registerConfigMbean(AbstractConfiguration config) {
        return null;
    }

    @Substitute
    public static void unRegisterConfigMBean(AbstractConfiguration config, ConfigMBean mbean) {

    }
}

@TargetClass(className = "io.smallrye.faulttolerance.DefaultMethodFallbackProvider")
final class Target_io_smallrye_faulttolerance_DefaultMethodFallbackProvider {

    @TargetClass(className = "io.smallrye.faulttolerance.ExecutionContextWithInvocationContext")
    static final class Target_io_smallrye_faulttolerance_ExecutionContextWithInvocationContext {

    }

    @Substitute
    static Object getFallback(Method fallbackMethod,
            Target_io_smallrye_faulttolerance_ExecutionContextWithInvocationContext ctx)
            throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException,
            Throwable {
        throw new RuntimeException("Not implemented in native mode");
    }
}

@TargetClass(className = "io.smallrye.faulttolerance.DefaultHystrixConcurrencyStrategy")
final class Target_io_smallrye_faulttolerance_DefaultHystrixConcurrencyStrategy {

    @Alias
    private static Logger LOGGER;

    @Alias
    ManagedThreadFactory managedThreadFactory;

    @Alias
    ThreadFactory threadFactory;

    @Substitute
    public void initTreadManagerFactory() {
        if (managedThreadFactory != null) {
            threadFactory = managedThreadFactory;
            LOGGER.debug("### Managed Thread Factory used ###");
        } else {
            threadFactory = new ContextThreadFactory();
            LOGGER.debug("### Context Thread Factory used ###");
        }
    }

    /**
     * Stripped down version of the Executors.privilegedThreadFactory(), getting rid of everything security manager related and
     * in particular FilePermission instances.
     * <p>
     * https://github.com/quarkusio/quarkus/issues/6024
     */
    private static class ContextThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final ClassLoader ccl;

        ContextThreadFactory() {
            group = Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
            this.ccl = Thread.currentThread().getContextClassLoader();
        }

        @Override
        public Thread newThread(final Runnable r) {
            Runnable setContextClassLoaderRunnable = new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setContextClassLoader(ccl);
                    r.run();
                }
            };

            Thread t = new Thread(group, setContextClassLoaderRunnable,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
