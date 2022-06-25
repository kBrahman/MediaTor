package z.zer.tor.media.util;

/**
 * Provides static methods to split, check for substrings, change case and
 * compare strings, along with additional string utility methods.
 */
public class Utils {

    public static final String ADMOB_NATIVE_ID = "ca-app-pub-8761730220693010/5747642740";

    /**
     * Check if a String is null or empty (the length is null).
     *
     * @param s the string to check
     * @return true if it is null or empty
     */
    public static boolean isNullOrEmpty(String s, boolean trim) {
        return s == null || (trim ? s.trim().length() == 0 : s.length() == 0);
    }

    public static boolean isNullOrEmpty(String s) {
        return isNullOrEmpty(s, false);
    }

    public static String removeDoubleSpaces(String s) {
        return s != null ? s.replaceAll("\\s+", " ") : null;
    }

}
