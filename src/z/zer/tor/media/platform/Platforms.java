package z.zer.tor.media.platform;

import java.io.File;

public final class Platforms {

    private static Platform platform;

    private Platforms() {
    }

    public static Platform get() {
        if (platform == null) {
            throw new IllegalStateException("Platform can't be null");
        }
        return platform;
    }

    public static void set(Platform p) {
        if (p == null) {
            throw new IllegalArgumentException("Platform can't be set to null");
        }
        platform = p;
    }

    /**
     * Shortcut to current platform file system.
     *
     * @return
     */
    public static FileSystem fileSystem() {
        return get().fileSystem();
    }

    /**
     * Shortcut to current platform application settings.
     *
     * @return
     */
    public static AppSettings appSettings() {
        return get().appSettings();
    }

    /**
     * Shortcut to current platform file system data method.
     *
     * @return
     */
    public static File data() {
        return get().systemPaths().data();
    }

    /**
     * Shortcut to current platform file system torrents method.
     *
     * @return
     */
    public static File torrents() {
        return get().systemPaths().torrents();
    }

    /**
     * Shortcut to current platform file system temp method.
     *
     * @return
     */
    public static File temp() {
        return get().systemPaths().temp();
    }
}
