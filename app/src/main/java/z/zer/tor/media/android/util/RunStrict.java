package z.zer.tor.media.android.util;

import android.os.StrictMode;

import z.zer.tor.media.android.gui.services.Engine;

public interface RunStrict<R> {

    R run();

    /**
     * Enable the most strict form of {@link StrictMode} possible,
     * with log and death as penalty. When {@code enable} is {@code false}, the
     * default more relaxed (LAX) policy is used.
     * <p>
     * This method only perform an actual action if the application is
     * in debug mode.
     *
     * @param enable {@code true} activate the most strict policy
     */
    static void setStrictPolicy(boolean enable) {
        if (!Debug.isEnabled()) {
            return; // no debug mode, do nothing
        }

        // by default, the LAX policy
        StrictMode.ThreadPolicy threadPolicy = StrictMode.ThreadPolicy.LAX;
        StrictMode.VmPolicy vmPolicy = StrictMode.VmPolicy.LAX;

        if (enable) {
            threadPolicy = new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build();
            vmPolicy = new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .setClassInstanceLimit(Engine.class, 1)
                    .build();
        }

        StrictMode.setThreadPolicy(threadPolicy);
        StrictMode.setVmPolicy(vmPolicy);
    }

    /**
     * Runs the runnable code under strict policy.
     *
     * @param r the runnable to execute r.run()
     */
    static void runStrict(Runnable r) {
        try {
            setStrictPolicy(true);
            r.run();
        } finally {
            setStrictPolicy(false);
        }
    }

    /**
     * Runs the lambda code under strict policy.
     *
     * @param r   the lambda to run
     * @param <R> the type of return
     * @return the return value
     */
    static <R> R runStrict(RunStrict<R> r) {
        try {
            setStrictPolicy(true);
            return r.run();
        } finally {
            setStrictPolicy(false);
        }
    }
}
