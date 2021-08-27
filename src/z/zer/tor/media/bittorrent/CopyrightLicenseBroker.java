package z.zer.tor.media.bittorrent;

import com.frostwire.jlibtorrent.Entry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import z.zer.tor.media.licenses.License;
import z.zer.tor.media.licenses.Licenses;

public class CopyrightLicenseBroker implements Mappable {

    public enum LicenseCategory {
        CreativeCommons("creative-commons"), OpenSource("open-source"), PublicDomain("public-domain"), NoLicense("no-license");

        private String name;

        LicenseCategory(String stringName) {
            name = stringName;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final LicenseCategory licenseCategory;

    public final License license;
    public final String attributionTitle;
    public final String attributionAuthor;
    public final String attributionUrl;

    public static final List<String> validLicenseUrls;

    public static final Map<String, License> urlToLicense;

    static {
        validLicenseUrls = new ArrayList<String>();
        validLicenseUrls.add(Licenses.CC_BY_4.getUrl());
        validLicenseUrls.add(Licenses.CC_BY_SA_4.getUrl());
        validLicenseUrls.add(Licenses.CC_BY_ND_4.getUrl());
        validLicenseUrls.add(Licenses.CC_BY_NC_4.getUrl());
        validLicenseUrls.add(Licenses.CC_BY_NC_SA_4.getUrl());
        validLicenseUrls.add(Licenses.CC_BY_NC_ND_4.getUrl());

        urlToLicense = new HashMap<String, License>();
        urlToLicense.put(Licenses.CC_BY_4.getUrl(), Licenses.CC_BY_4);
        urlToLicense.put(Licenses.CC_BY_SA_4.getUrl(), Licenses.CC_BY_SA_4);
        urlToLicense.put(Licenses.CC_BY_ND_4.getUrl(), Licenses.CC_BY_ND_4);
        urlToLicense.put(Licenses.CC_BY_NC_4.getUrl(), Licenses.CC_BY_NC_4);
        urlToLicense.put(Licenses.CC_BY_NC_SA_4.getUrl(), Licenses.CC_BY_NC_SA_4);
        urlToLicense.put(Licenses.CC_BY_NC_ND_4.getUrl(), Licenses.CC_BY_NC_ND_4);

        urlToLicense.put(Licenses.APACHE.getUrl(), Licenses.APACHE);
        urlToLicense.put(Licenses.BSD_2_CLAUSE.getUrl(), Licenses.BSD_2_CLAUSE);
        urlToLicense.put(Licenses.BSD_3_CLAUSE.getUrl(), Licenses.BSD_3_CLAUSE);
        urlToLicense.put(Licenses.GPL3.getUrl(), Licenses.GPL3);
        urlToLicense.put(Licenses.LGPL.getUrl(), Licenses.LGPL);
        urlToLicense.put(Licenses.MIT.getUrl(), Licenses.MIT);
        urlToLicense.put(Licenses.MOZILLA.getUrl(), Licenses.MOZILLA);
        urlToLicense.put(Licenses.CDDL.getUrl(), Licenses.CDDL);
        urlToLicense.put(Licenses.ECLIPSE.getUrl(), Licenses.ECLIPSE);

        urlToLicense.put(Licenses.PUBLIC_DOMAIN_MARK.getUrl(), Licenses.PUBLIC_DOMAIN_MARK);
        urlToLicense.put(Licenses.PUBLIC_DOMAIN_CC0.getUrl(), Licenses.PUBLIC_DOMAIN_CC0);
    }

    public CopyrightLicenseBroker(boolean shareAlike, boolean nonCommercial, boolean noDerivatives, String attributionTitle, String attributionAuthor, String attributionURL) {
        licenseCategory = LicenseCategory.CreativeCommons;
        final String licenseUrl = getCreativeCommonsLicenseUrl(shareAlike, nonCommercial, noDerivatives);
        if (!isInvalidLicense(licenseUrl)) {
            this.license = urlToLicense.get(licenseUrl);
            this.attributionTitle = attributionTitle;
            this.attributionAuthor = attributionAuthor;
            this.attributionUrl = attributionURL;
        } else {
            throw new IllegalArgumentException("The given license string is invalid.");
        }
    }

    /**
     * Deserialization constructor
     *
     * @param map
     */
    public CopyrightLicenseBroker(Map<String, Entry> map) {
        if (map.containsKey("creative-commons")) {
            licenseCategory = LicenseCategory.CreativeCommons;
        } else if (map.containsKey("open-source")) {
            licenseCategory = LicenseCategory.OpenSource;
        } else if (map.containsKey("public-domain")) {
            licenseCategory = LicenseCategory.PublicDomain;
        } else {
            licenseCategory = LicenseCategory.NoLicense;
        }

        if (licenseCategory != LicenseCategory.NoLicense) {
            Map<String, Entry> innerMap = map.get(licenseCategory.toString()).dictionary();
            String licenseUrl = innerMap.get("licenseUrl").string();
            this.license = urlToLicense.get(licenseUrl);
            this.attributionTitle = innerMap.get("attributionTitle").string();
            this.attributionAuthor = innerMap.get("attributionAuthor").string();
            this.attributionUrl = innerMap.get("attributionUrl").string();
        } else {
            this.license = null;
            this.attributionTitle = null;
            this.attributionAuthor = null;
            this.attributionUrl = null;
        }
    }

    public CopyrightLicenseBroker(LicenseCategory category, License license, String title, String author, String attributionUrl) {
        licenseCategory = category;
        this.license = license;
        this.attributionTitle = title;
        this.attributionAuthor = author;
        this.attributionUrl = attributionUrl;
    }

    public String getLicenseName() {
        return license != null ? license.getName() : null;
    }

    private static boolean isInvalidLicense(String licenseStr) {
        return licenseStr == null || licenseStr.isEmpty() || !validLicenseUrls.contains(licenseStr);
    }

    /**
     * This method makes sure you input a valid license, even if you make a mistake combining these parameters
     */
    public static String getCreativeCommonsLicenseUrl(boolean shareAlike, boolean nonCommercial, boolean noDerivatives) {
        if (nonCommercial && shareAlike) {
            noDerivatives = false;
        } else if (nonCommercial && noDerivatives) {
            shareAlike = false;
        } else if (shareAlike) {
            noDerivatives = false;
        }

        String licenseShortCode = "by-" + (nonCommercial ? "nc-" : "") + (shareAlike ? "sa" : "") + (noDerivatives ? "-nd" : "");
        licenseShortCode = licenseShortCode.replace("--", "-");
        if (licenseShortCode.endsWith("-")) {
            licenseShortCode = licenseShortCode.substring(0, licenseShortCode.length() - 1);
        }
        return "http://creativecommons.org/licenses/" + licenseShortCode + "/" + "4.0" + "/";
    }
}
