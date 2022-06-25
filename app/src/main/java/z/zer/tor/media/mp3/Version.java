package z.zer.tor.media.mp3;

public class Version {
	private static final int MAJOR_VERSION = 0;
	private static final int MINOR_VERSION = 6;
	private static final String URL = "http://github.com/mpatric/mp3agic";

	public static String getVersion() {
		return MAJOR_VERSION + "." + MINOR_VERSION;
	}
	public static String getUrl() {
		return URL;
	}
}
