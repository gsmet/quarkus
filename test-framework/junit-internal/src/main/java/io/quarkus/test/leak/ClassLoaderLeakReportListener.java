package io.quarkus.test.leak;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    /**
     * Delay in milliseconds before collecting garbage and dumping, to let threads of the last application terminate.
     */
    private static final String DELAY = "io.quarkus.test.heap-dump-on-exit.delay";
    /**
     * Number of GC + finalization rounds before dumping.
     */
    private static final String GC_ROUNDS = "io.quarkus.test.heap-dump-on-exit.gc-rounds";
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
                long delay = Long.getLong(DELAY, 3000L);
                out.println("[heap-dump-on-exit] pid " + ProcessHandle.current().pid() + ", waiting " + delay + " ms");
                // the wait ends early when a file named like the dump with a .proceed suffix shows up, so that an
                // external tool can inspect the live JVM for as long as needed
                Path proceed = dump.resolveSibling(dump.getFileName() + ".proceed");
                for (long waited = 0; waited < delay && !Files.exists(proceed); waited += 250) {
                    Thread.sleep(250);
                }
                forceGc();
                Path threads = dump.resolveSibling(dump.getFileName() + ".threads.txt");
                Files.writeString(threads, threadReport());
                out.println("[heap-dump-on-exit] thread report written to " + threads);
                long start = System.nanoTime();
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).dumpHeap(dump.toString(), true);
                out.println("[heap-dump-on-exit] heap dump written to " + dump + " in "
                        + (System.nanoTime() - start) / 1_000_000 + " ms");
            } catch (IOException | RuntimeException e) {
                out.println("[heap-dump-on-exit] failed");
                e.printStackTrace(out);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * One block per live thread: name, state, daemon flag, the class loader of the thread's class, the context
         * class loader and the top frames. A thread whose class or context class loader is a closed Quarkus class
         * loader retains it.
         */
        private static String threadReport() {
            StringBuilder sb = new StringBuilder();
            Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
            List<Thread> threads = new ArrayList<>(stacks.keySet());
            threads.sort(Comparator.comparing(Thread::getName));
            for (Thread thread : threads) {
                sb.append(thread.getName()).append(" [").append(thread.getState()).append(thread.isDaemon() ? ", daemon" : "")
                        .append("]\n");
                sb.append("    class: ").append(thread.getClass().getName()).append(" loaded by ")
                        .append(thread.getClass().getClassLoader()).append('\n');
                sb.append("    context class loader: ").append(thread.getContextClassLoader()).append('\n');
                StackTraceElement[] stack = stacks.get(thread);
                for (int i = 0; i < Math.min(12, stack.length); i++) {
                    sb.append("        at ").append(stack[i]).append('\n');
                }
            }
            return sb.toString();
        }

        private static void forceGc() {
            int rounds = Integer.getInteger(GC_ROUNDS, 5);
            for (int i = 0; i < rounds; i++) {
                System.gc();
                System.runFinalization();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
