package z.zer.tor.media.frostclick;

public class Slide {

    /**
     * length of time this slide will be shown
     */
    public long duration;

    /**
     * url of torrent file that should be opened if user clicks on this slide
     */
    public String torrent;

    public String httpDownloadURL;

    /**
     * language (optional filter) = Can be given in the forms of:
     * *
     * en
     * en_US
     */
    public String language;

    /**
     * os (optional filter) = Can be given in the forms of:
     * windows
     * mac
     * linux
     */
    public String os;

    /**
     * Title of the promotion
     */
    public String title;

    public String author;

    /**
     * Download method
     * 0 - Torrent
     * 1 - HTTP
     */
    public int method;

    public String md5;

    public String twitter;

    public int flags;

    public String uri;


    /**
     * Total size in bytes
     */
    public long size;
}
