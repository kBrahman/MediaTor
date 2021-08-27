package z.zer.tor.media.uxstats;

import java.util.UUID;

public final class UXStatsConf {

    private final String url;
    private final String guid;
    private final String os;
    private final String fwversion;
    private final String fwbuild;
    private final int period;
    private final int minEntries;
    private final int maxEntries;

    public UXStatsConf(String url, String os, String fwversion, String fwbuild, int period, int minEntries, int maxEntries) {
        this.url = url;
        this.guid = UUID.randomUUID().toString();
        this.os = os;
        this.fwversion = fwversion;
        this.fwbuild = fwbuild;
        this.period = period;
        this.minEntries = minEntries;
        this.maxEntries = maxEntries;
    }

    public String getUrl() {
        return url;
    }

    public String getGuid() {
        return guid;
    }

    public String getOS() {
        return os;
    }

    public String getFwversion() {
        return fwversion;
    }

    public String getFwbuild() {
        return fwbuild;
    }

    public int getPeriod() {
        return period;
    }

    public int getMinEntries() {
        return minEntries;
    }

    public int getMaxEntries() {
        return maxEntries;
    }
}
