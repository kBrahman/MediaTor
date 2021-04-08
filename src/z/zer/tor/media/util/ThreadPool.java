package z.zer.tor.media.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is not final, but it's not meant to be inherited but
 * only in very specific situations.
 */
public class ThreadPool extends ThreadPoolExecutor {

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String name;

    public ThreadPool(String name, int maximumPoolSize, BlockingQueue<Runnable> workQueue, boolean daemon) {
        super(maximumPoolSize, maximumPoolSize, 1L, TimeUnit.SECONDS, workQueue, new PoolThreadFactory(daemon));
        this.name = name;
    }

    public ThreadPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, BlockingQueue<Runnable> workQueue, boolean daemon) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue, new PoolThreadFactory(daemon));
        this.name = name;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        String threadName = null;
        if (r instanceof Thread) {
            Thread thread = (Thread) r;
            threadName = thread.getName();
        }

        t.setName(name + "-thread-" + threadNumber.getAndIncrement() + "-" + (threadName != null ? threadName : "@" + r.hashCode()));
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        Thread.currentThread().setName(name + "-thread-idle");
    }

    public static ExecutorService newThreadPool(String name, int maxThreads, boolean daemon) {
        ThreadPool pool = new ThreadPool(name, maxThreads, new LinkedBlockingQueue<>(), daemon);
        return Executors.unconfigurableExecutorService(pool);
    }

    public static ExecutorService newThreadPool(String name, int maxThreads) {
        return newThreadPool(name, maxThreads, false);
    }

    public static ExecutorService newThreadPool(String name, boolean daemon) {
        ThreadPool pool = new ThreadPool(name, Integer.MAX_VALUE, new LinkedBlockingQueue<>(), daemon);
        return Executors.unconfigurableExecutorService(pool);
    }

    public static ExecutorService newThreadPool(String name) {
        return newThreadPool(name, false);
    }

    private static final class PoolThreadFactory implements ThreadFactory {
        private final boolean daemon;

        public PoolThreadFactory(boolean daemon) {
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(daemon);

            return t;
        }
    }
}
