package z.zer.tor.media.util;

import static java.util.logging.Level.INFO;

import java.util.logging.Level;

public final class Logger {

    private final java.util.logging.Logger jul;
    private final String name;

    private Logger(java.util.logging.Logger jul) {
        this.jul = jul;
        this.name = jul.getName();
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(java.util.logging.Logger.getLogger(clazz.getSimpleName()));
    }

    public String getName() {
        return name;
    }

    public void info(String msg, boolean showCallingMethodInfo) {
        jul.logp(INFO, name, "", ((showCallingMethodInfo) ? appendCallingMethodInfo(msg) : msg));
    }

    public void info(String msg) {
        info(msg, false);
    }

    public void info(String msg, Throwable e, boolean showCallingMethodInfo) {
        jul.logp(Level.INFO, name, "", ((showCallingMethodInfo) ? appendCallingMethodInfo(msg) : msg), e);
    }

    public void info(String msg, Throwable e) {
        info(msg, e, false);
    }

    public void warn(String msg, boolean showCallingMethodInfo) {
        jul.logp(INFO, name, "", ((showCallingMethodInfo) ? appendCallingMethodInfo(msg) : msg));
    }

    public void warn(String msg) {
        warn(msg, false);
    }

    public void warn(String msg, Throwable e, boolean showCallingMethodInfo) {
        jul.logp(Level.INFO, name, "", ((showCallingMethodInfo) ? appendCallingMethodInfo(msg) : msg), e);
    }

    public void warn(String msg, Throwable e) {
        warn(msg, e, false);
    }

    public void error(String msg, boolean showCallingMethodInfo) {
        jul.logp(INFO, name, "", ((showCallingMethodInfo) ? appendCallingMethodInfo(msg) : msg));
    }

    public void error(String msg) {
        error(msg, false);
    }

    public void error(String msg, Throwable e, boolean showCallingMethodInfo) {
        jul.logp(Level.INFO, name, "", ((showCallingMethodInfo) ? appendCallingMethodInfo(msg) : msg), e);
    }

    public void error(String msg, Throwable e) {
        error(msg, e, false);
    }

    public void debug(String msg, boolean showCallingMethodInfo) {
        jul.logp(INFO, name, "", ((showCallingMethodInfo) ? appendCallingMethodInfo(msg) : msg));
    }

    public void debug(String msg) {
        debug(msg, false);
    }

    public void debug(String msg, Throwable e, boolean showCallingMethodInfo) {
        jul.logp(Level.INFO, name, "", ((showCallingMethodInfo) ? appendCallingMethodInfo(msg) : msg), e);
    }

    public void debug(String msg, Throwable e) {
        debug(msg, e, false);
    }

    private static String getCallingMethodInfo() {
        Thread currentThread = Thread.currentThread();
        StackTraceElement[] stackTrace = currentThread.getStackTrace();
        String caller = " - <Thread not scheduled yet>";
        if (stackTrace.length >= 5) {
            StackTraceElement stackElement = stackTrace[5];
            caller = " - Called from <" + stackElement.getFileName() + "::" + stackElement.getMethodName() + ":" + stackElement.getLineNumber() + " on thread:" + currentThread.getName() + "(tid=" + currentThread.getId() + ")>";
        }
        return caller;
    }

    private static String appendCallingMethodInfo(String msg) {
        return msg + getCallingMethodInfo();
    }
}
