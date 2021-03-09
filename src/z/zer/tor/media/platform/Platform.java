package z.zer.tor.media.platform;

public interface Platform {

    FileSystem fileSystem();

    SystemPaths systemPaths();

    AppSettings appSettings();

    VPNMonitor vpn();
}
