package z.zer.tor.media.android.gui.services;

import java.util.concurrent.ExecutorService;

public final class Engine {

    private static final ExecutorService MAIN_THREAD_POOL = new EngineThreadPool();
    private static final String TAG = Engine.class.getSimpleName();


    private Engine() {
    }

    private static class Loader {
        static final Engine INSTANCE = new Engine();
    }

    public static Engine instance() {
        return Engine.Loader.INSTANCE;
    }



    public ExecutorService getThreadPool() {
        return MAIN_THREAD_POOL;
    }

}
