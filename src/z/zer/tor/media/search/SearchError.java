package z.zer.tor.media.search;


public final class SearchError {

    private final int code;

    public SearchError(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
