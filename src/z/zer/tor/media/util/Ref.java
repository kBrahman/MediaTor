package z.zer.tor.media.util;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public final class Ref {

    private Ref() {
    }

    public static <T> WeakReference<T> weak(T obj) {
        return new WeakReference<>(obj);
    }

    public static <T> boolean alive(Reference<T> ref) {
        return ref != null && ref.get() != null;
    }

    public static void free(Reference<?> ref) {
        ref.clear();
    }
}
