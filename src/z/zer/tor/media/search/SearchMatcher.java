package z.zer.tor.media.search;


import z.zer.tor.media.regex.Matcher;

/**
 * <strong>A memory conscious Matcher</strong><br/>
 * Instead of using the groups() that reference the original HTML strings,
 * we just make copies of those substrings with this search matcher everytime
 * we invoke group(), this way the original HTML can be dereferenced and garbage collected.
 *
 * @author gubatron
 * @author aldenml
 */
public final class SearchMatcher {

    private final Matcher matcher;

    public static SearchMatcher from(Matcher matcher) {
        return new SearchMatcher(matcher);
    }

    public SearchMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    public boolean find() {
        return matcher.find();
    }

    public String group(int group) {
        return copy(matcher.group(group));
    }

    public String group(String group) {
        if (matcher.hasGroup(group)) {
            return copy(matcher.group(group));
        }
        return null;
    }

    private String copy(String str) {
        if (str == null) {
            return null;
        }
        return new String(str.toCharArray());
    }
}
