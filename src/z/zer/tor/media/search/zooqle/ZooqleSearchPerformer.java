/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2018, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package z.zer.tor.media.search.zooqle;

import z.zer.tor.media.search.CrawlableSearchResult;
import z.zer.tor.media.search.SearchMatcher;
import z.zer.tor.media.search.torrent.TorrentRegexSearchPerformer;

public final class ZooqleSearchPerformer extends TorrentRegexSearchPerformer<ZooqleSearchResult> {
    //private static final Logger LOG = Logger.getLogger(ZooqleSearchPerformer.class);
    private static final int MAX_RESULTS = 30;
    private static final String PRELIMINARY_RESULTS_REGEX = "(?is)<i class=\".*?text-muted2 zqf-small pad-r2\"></i><a class=\".*?small\" href=\"/(?<detailPath>.*?).html\">.*?</a>";
    private static final String HTML_DETAIL_REGEX = "(?is)" + "<h4 id=\"torname\">(?<filename>.*?)<span class=\"text-muted4 pad-r2\">.torrent</span>.*" + "title=\"Torrent cloud statistics\"></i><div class=\"progress prog trans..\" title=\"Seeders: (?<seeds>\\d+).*" + "<i class=\"zqf zqf-files text-muted3 pad-r2 trans80\"(?<sizedata>.*?)</span><span class=\"spacer\">.*" + "<i class=\"zqf zqf-time text-muted3 pad-r2 trans80\" title=\"Date indexed\"></i>(?<month>.{3}) (?<day>\\d{1,2}), (?<year>\\d{4}) <span class=\"small pad-l\".*" + "<a rel=\"nofollow\" href=\"magnet:\\?xt=urn:btih:(?<magnet>.*)\"><i class=\"spr dl-magnet pad-r2\"></i>Magnet.*?";

    public ZooqleSearchPerformer(String domainName, long token, String keywords, int timeout) {
        super(domainName, token, keywords, timeout, 1, 2 * MAX_RESULTS, MAX_RESULTS, PRELIMINARY_RESULTS_REGEX, HTML_DETAIL_REGEX);
    }

    @Override
    public CrawlableSearchResult fromMatcher(SearchMatcher matcher) {
        return new ZooqleTempSearchResult(getDomainName(), matcher.group("detailPath") + ".html");
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "https://" + getDomainName() + "/search?pg=" + page + "&q=" + encodedKeywords + "&s=ns&v=t&sd=d";
    }

    @Override
    protected ZooqleSearchResult fromHtmlMatcher(CrawlableSearchResult sr, SearchMatcher matcher) {
        return new ZooqleSearchResult(sr.getDetailsUrl(), "https://" + getDomainName(), matcher);
    }

    @Override
    protected int preliminaryHtmlPrefixOffset(String page) {
        return page.indexOf("<i class=\"spr feed\"></i>");
    }

    @Override
    protected int preliminaryHtmlSuffixOffset(String page) {
        int offset = page.indexOf("Time:");
        if (offset == -1) {
            return super.preliminaryHtmlSuffixOffset(page);
        }
        return offset;
    }

    @Override
    protected int htmlPrefixOffset(String html) {
        int offset = html.indexOf("<h4 id=\"torname\">");
        if (offset == -1) {
            return super.htmlPrefixOffset(html);
        }
        return offset - 20;
    }

    @Override
    protected int htmlSuffixOffset(String html) {
        int offset = html.indexOf("Language:");
        if (offset == -1) {
            return super.htmlSuffixOffset(html);
        }
        return offset;
    }

    @Override
    protected boolean isValidHtml(String html) {
        return html != null && !html.contains("Cloudflare");
    }
}
