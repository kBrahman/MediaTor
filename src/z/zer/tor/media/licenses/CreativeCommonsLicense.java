package z.zer.tor.media.licenses;

import java.util.Locale;

public final class CreativeCommonsLicense extends License {

    private final String acronym;

    /**
     * To use with raw input
     *
     * @param name
     * @param url
     * @param acronym
     */
    CreativeCommonsLicense(String name, String url, String acronym) {
        super(name, url);
        this.acronym = acronym;
    }

    public String acronym() {
        return acronym;
    }

    static CreativeCommonsLicense standard(String name, String acronym, String version) {
        String fullName = "Creative Commons " + name + " " + version;
        String url = "http://creativecommons.org/licenses/" + acronym.toLowerCase(Locale.US) + "/" + version + "/";
        String fullAcronym = "CC " + acronym.toUpperCase(Locale.US) + " " + version;

        return new CreativeCommonsLicense(fullName, url, fullAcronym);
    }
}
