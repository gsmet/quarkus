package io.quarkus.test.leak;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * Debug facility to track class loader leaks in a test run.
 * <p>
 * Enabled by setting the {@code io.quarkus.test.heap-dump-on-exit} system property to a file path. At JVM exit, after
 * the whole JUnit launcher session has been closed, it forces garbage collection and writes a live-objects heap dump
 * to that path. Anything still present in the dump is reachable, so closed Quarkus class loaders found in it are
 * leaks.
 * <p>
 * The work is done in a shutdown hook rather than in {@link #launcherSessionClosed(LauncherSession)} so that it runs
 * after every other session listener has released its own references. Surefire's
 * {@code forkedProcessExitTimeoutInSeconds} must be raised accordingly, as writing the dump can take a while.
 * <p>
 * The listener is also discovered by the JUnit launcher that dev mode runs inside the application class loaders, so
 * it only acts when loaded by the system class loader, and the shutdown hook thread is explicitly given the system
 * class loader as context class loader, so that the tooling itself does not retain anything.
 */
public class ClassLoaderLeakReportListener implements LauncherSessionListener {

    private static final String DUMP_PATH = "io.quarkus.test.heap-dump-on-exit";
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        String path = System.getProperty(DUMP_PATH);
        if (path == null || path.isBlank()) {
            return;
        }
        if (getClass().getClassLoader() != ClassLoader.getSystemClassLoader()) {
            return;
        }
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        Thread hook = new Thread(new DumpTask(Path.of(path)), "heap-dump-on-exit");
        hook.setContextClassLoader(ClassLoader.getSystemClassLoader());
        Runtime.getRuntime().addShutdownHook(hook);
    }

    private static final class DumpTask implements Runnable {

        private final Path dump;

        private DumpTask(Path dump) {
            this.dump = dump;
        }

        @Override
        public void run() {
            PrintStream out = System.err;
            try {
                if (dump.getParent() != null) {
                    Files.createDirectories(dump.getParent());
                }
                Files.deleteIfExists(dump);
                forceGc();
                long start = System.nanoTime();
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).dumpHeap(dump.toString(), true);
                out.println("[heap-dump-on-exit] heap dump written to " + dump + " in "
                        + (System.nanoTime() - start) / 1_000_000 + " ms");
            } catch (IOException | RuntimeException e) {
                out.println("[heap-dump-on-exit] failed");
                e.printStackTrace(out);
            }
        }

        private static void forceGc() {
            for (int i = 0; i < 5; i++) {
                System.gc();
                System.runFinalization();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
