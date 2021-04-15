package z.zer.tor.media.android;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;

import com.frostwire.jlibtorrent.swig.libtorrent;
import com.frostwire.jlibtorrent.swig.posix_stat_t;
import com.frostwire.jlibtorrent.swig.posix_wrapper;

import java.io.File;

import z.zer.tor.media.android.gui.Librarian;
import z.zer.tor.media.platform.AbstractPlatform;
import z.zer.tor.media.platform.DefaultFileSystem;
import z.zer.tor.media.platform.FileSystem;
import z.zer.tor.media.platform.Platform;
import z.zer.tor.media.platform.Platforms;
import z.zer.tor.media.platform.VPNMonitor;
import z.zer.tor.media.util.Logger;

public final class AndroidPlatform extends AbstractPlatform {

    private static final Logger LOG = Logger.getLogger(AndroidPlatform.class);

    private static final int VERSION_CODE_LOLLIPOP = 21;

    public AndroidPlatform(Application app) {
        super(buildFileSystem(app), new AndroidPaths(app), new AndroidSettings());
    }

    @Override
    public VPNMonitor vpn() {
        return null;
    }

    public static boolean saf() {
        Platform p = Platforms.get();
        return p.fileSystem() instanceof LollipopFileSystem;
    }

    /**
     * This method determines if the file {@code f} is protected by
     * the SAF framework because it's in the real external SD card.
     *
     * @param f the file
     * @return if protected by SAF
     */
    public static boolean saf(File f) {
        Platform p = Platforms.get();

        if (!(p.fileSystem() instanceof LollipopFileSystem)) {
            return false;
        }

        if (f.getPath().contains("/Android/data/com.frostwire.android/")) {
            // private file, FUSE give us standard POSIX operations
            return false;
        }

        LollipopFileSystem fs = (LollipopFileSystem) p.fileSystem();

        return fs.getExtSdCardFolder(f) != null;
    }

    private static FileSystem buildFileSystem(Application app) {
        FileSystem fs;

        if (Build.VERSION.SDK_INT >= VERSION_CODE_LOLLIPOP) {
            LollipopFileSystem lfs = new LollipopFileSystem(app);
            PosixCalls w = new PosixCalls(lfs);
            w.swigReleaseOwnership();
            libtorrent.set_posix_wrapper(w);
            //LibTorrent.setPosixWrapper(new PosixCalls(lfs));
            fs = lfs;
        } else {
            fs = new DefaultFileSystem() {
                @Override
                public void scan(File file) {
                    Librarian.instance().scan(app, file);
                }
            };
        }

        return fs;
    }

    private static final class PosixCalls extends posix_wrapper {

        private final LollipopFileSystem fs;

        PosixCalls(LollipopFileSystem fs) {
            this.fs = fs;
        }

        @Override
        public int open(String path, int flags, int mode) {
            LOG.info("posix - open:" + path);

            int r = super.open(path, flags, mode);
            if (r >= 0) {
                return r;
            }

            r = fs.openFD(new File(path), "rw");
            if (r < 0) {
                LOG.info("posix wrapper failed to create native fd for: " + path);
            }

            return r;
        }

        @SuppressLint("SdCardPath")
        @Override
        public int stat(String path, posix_stat_t buf) {
            LOG.info("posix - stat:" + path);

            int r = super.stat(path, buf);
            if (r >= 0) {
                return r;
            }

            DocumentFile f = fs.getDocument(new File(path));
            if (f == null) {
                LOG.info("posix wrapper failed to stat file for: " + path);
                // this trick the posix layer to set the correct errno
                return super.stat("/data/data/com.frostwire.android/noexists.txt", buf);
            }

            int S_ISDIR = f.isDirectory() ? 0040000 : 0;
            int S_IFREG = 0100000;

            buf.setMode(S_ISDIR | S_IFREG);
            buf.setSize(f.length());
            int t = (int) (f.lastModified() / 1000);
            buf.setAtime(t);
            buf.setMtime(t);
            buf.setCtime(t);

            return 0;
        }

        @Override
        public int mkdir(String path, int mode) {
            LOG.info("posix - mkdir:" + path);
            int r = super.mkdir(path, mode);
            if (r >= 0) {
                return r;
            }

            r = fs.mkdirs(new File(path)) ? 0 : -1;
            if (r < 0) {
                LOG.info("posix wrapper failed to create dir: " + path);
            }

            return r;
        }

        @Override
        public int rename(String oldpath, String newpath) {
            LOG.info("posix - rename:" + oldpath + " -> " + newpath);
            int r = super.rename(oldpath, newpath);
            if (r >= 0) {
                return r;
            }

            File src = new File(oldpath);
            File dest = new File(newpath);

            if (fs.copy(src, dest)) {
                fs.delete(src);
                return 0;
            } else {
                LOG.info("posix wrapper failed to copy file: " + oldpath + " -> " + newpath);
                return -1;
            }
        }

        @Override
        public int remove(String path) {
            LOG.info("posix - remove:" + path);
            int r = super.remove(path);
            if (r >= 0) {
                return r;
            }

            r = fs.delete(new File(path)) ? 0 : -1;
            if (r < 0) {
                LOG.info("posix wrapper failed to delete file: " + path);
            }

            return r;
        }
    }
}