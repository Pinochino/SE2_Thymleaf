package com.example.SE2.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * NovelScraperService - Cào nội dung truyện từ nhiều trang web
 *
 * Hỗ trợ:
 *   - truyenfull.vn   (Tiếng Việt)
 *   - metruyencv.com  (Tiếng Việt)
 *   - royalroad.com   (Tiếng Anh)
 *   - novelupdates.com / fanfiction sites
 *
 * Dependency (Maven):
 *   <dependency>
 *     <groupId>org.jsoup</groupId>
 *     <artifactId>jsoup</artifactId>
 *     <version>1.17.2</version>
 *   </dependency>
 */
public class NovelScraperService {

    private static final Logger log = Logger.getLogger(NovelScraperService.class.getName());

    // ── Timeout & politeness ──────────────────────────────────────────────────
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 15_000;
    private static final long POLITE_DELAY_MS   = 1_500;   // tránh bị ban IP

    // ── User-Agent giả trình duyệt thật ──────────────────────────────────────
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36";

    // =========================================================================
    //  PUBLIC DATA MODELS
    // =========================================================================

    public record NovelInfo(
            String title,
            String author,
            String description,
            List<ChapterLink> chapterList
    ) {}

    public record ChapterLink(
            int       index,
            String    title,
            String    url,
            LocalDate publishDate   // ngày đăng/cập nhật, null nếu không parse được
    ) {
        /** Constructor tương thích ngược (không có ngày) */
        public ChapterLink(int index, String title, String url) {
            this(index, title, url, null);
        }
    }

    /** Thông tin truyện mới cập nhật trên trang chủ / trang danh sách */
    public record NovelUpdate(
            String    title,
            String    novelUrl,
            String    latestChapter,
            String    latestChapterUrl,
            LocalDate updateDate
    ) {}

    public record ChapterContent(
            String title,
            String url,
            String content          // full text, newlines preserved
    ) {}

    // =========================================================================
    //  SITE STRATEGY INTERFACE
    // =========================================================================

    private interface SiteStrategy {
        boolean supports(String url);
        NovelInfo          fetchNovelInfo(String novelUrl)       throws IOException;
        ChapterContent     fetchChapter(ChapterLink link)       throws IOException;
        /** Lấy danh sách truyện mới cập nhật (trang chủ / trang danh sách mới) */
        List<NovelUpdate>  fetchLatestUpdates(int page)         throws IOException;
        /** Base URL của site, dùng cho fetchLatestUpdates */
        String             baseUrl();
    }

    // =========================================================================
    //  STRATEGY: truyenfull.vn
    // =========================================================================

    private static class TruyenFullStrategy implements SiteStrategy {

        @Override
        public boolean supports(String url) {
            return url.contains("truyenfull.vn");
        }

        @Override
        public String baseUrl() {
            return "https://truyenfull.vn";
        }

        @Override
        public NovelInfo fetchNovelInfo(String novelUrl) throws IOException {
            Document doc = fetch(novelUrl);

            String title  = text(doc, "h3.title");
            String author = text(doc, "div.info-holder a[href*=tac-gia]");
            String desc   = text(doc, "div.desc-text");

            List<ChapterLink> chapters = new ArrayList<>();
            Elements items = doc.select("ul.list-chapter li");
            for (int i = 0; i < items.size(); i++) {
                Element li = items.get(i);
                Element a  = li.selectFirst("a");
                if (a == null) continue;
                // Truyenfull hiển thị ngày cập nhật trong <span class="text-muted">
                LocalDate date = parseDate(textEl(li, "span.text-muted"));
                chapters.add(new ChapterLink(i + 1, a.text(), a.absUrl("href"), date));
            }
            log.info("[TruyenFull] \"" + title + "\" – " + chapters.size() + " chương (trang 1)");
            return new NovelInfo(title, author, desc, chapters);
        }

        @Override
        public ChapterContent fetchChapter(ChapterLink link) throws IOException {
            Document doc     = fetch(link.url());
            String   title   = text(doc, "a.chapter-title");
            doc.select("div#chapter-content script, div.ads-chapter").remove();
            String content = doc.select("div#chapter-content").text();
            return new ChapterContent(title.isBlank() ? link.title() : title,
                                      link.url(), content);
        }

        @Override
        public List<NovelUpdate> fetchLatestUpdates(int page) throws IOException {
            String url = baseUrl() + "/danh-sach/truyen-moi-cap-nhat/trang-" + page + "/";
            Document doc = fetch(url);
            List<NovelUpdate> updates = new ArrayList<>();

            Elements rows = doc.select("div.list-truyen .row");
            for (Element row : rows) {
                Element titleEl = row.selectFirst("h3.truyen-title a");
                if (titleEl == null) continue;

                String novelTitle = titleEl.text();
                String novelUrl   = titleEl.absUrl("href");

                Element chapEl = row.selectFirst("div.col-xs-2 a");
                String latestChap    = chapEl != null ? chapEl.text() : "";
                String latestChapUrl = chapEl != null ? chapEl.absUrl("href") : "";

                LocalDate date = parseDate(textEl(row, "div.col-xs-2 span, div.text-info"));
                updates.add(new NovelUpdate(novelTitle, novelUrl, latestChap, latestChapUrl, date));
            }
            log.info("[TruyenFull] Trang " + page + " – " + updates.size() + " truyện mới cập nhật");
            return updates;
        }
    }

    // =========================================================================
    //  STRATEGY: metruyencv.com
    // =========================================================================

    private static class MeTruyenCVStrategy implements SiteStrategy {

        @Override
        public boolean supports(String url) {
            return url.contains("metruyencv.com") || url.contains("metruyenvip.com");
        }

        @Override
        public String baseUrl() {
            return "https://metruyencv.com";
        }

        @Override
        public NovelInfo fetchNovelInfo(String novelUrl) throws IOException {
            Document doc = fetch(novelUrl);

            String title  = text(doc, "h1.h3");
            String author = text(doc, "a[itemprop=author]");
            String desc   = text(doc, "div.content");

            List<ChapterLink> chapters = new ArrayList<>();
            Elements items = doc.select("ul.list-chapter li");
            for (int i = 0; i < items.size(); i++) {
                Element li = items.get(i);
                Element a  = li.selectFirst("a");
                if (a == null) continue;
                LocalDate date = parseDate(textEl(li, "span.chapter-time, span.text-muted"));
                chapters.add(new ChapterLink(i + 1, a.text(), a.absUrl("href"), date));
            }
            log.info("[MeTruyenCV] \"" + title + "\" – " + chapters.size() + " chương");
            return new NovelInfo(title, author, desc, chapters);
        }

        @Override
        public ChapterContent fetchChapter(ChapterLink link) throws IOException {
            Document doc   = fetch(link.url());
            String   title = text(doc, "h2");
            doc.select("script, .ads, .box-notice").remove();
            String content = doc.select("div#article").text();
            return new ChapterContent(title.isBlank() ? link.title() : title,
                                      link.url(), content);
        }

        @Override
        public List<NovelUpdate> fetchLatestUpdates(int page) throws IOException {
            String url = baseUrl() + "/danh-sach/truyen-moi-cap-nhat?page=" + page;
            Document doc = fetch(url);
            List<NovelUpdate> updates = new ArrayList<>();

            Elements items = doc.select("div.media, div.story-item, li.story-item");
            for (Element item : items) {
                Element titleEl = item.selectFirst("h3 a, a.story-title");
                if (titleEl == null) continue;

                String novelTitle = titleEl.text();
                String novelUrl2  = titleEl.absUrl("href");

                Element chapEl = item.selectFirst("a.chapter-latest, span.chapter-latest a");
                String latestChap    = chapEl != null ? chapEl.text() : "";
                String latestChapUrl = chapEl != null ? chapEl.absUrl("href") : "";

                LocalDate date = parseDate(textEl(item, "time, span.time, span.text-muted"));
                updates.add(new NovelUpdate(novelTitle, novelUrl2, latestChap, latestChapUrl, date));
            }
            log.info("[MeTruyenCV] Trang " + page + " – " + updates.size() + " truyện mới cập nhật");
            return updates;
        }
    }

    // =========================================================================
    //  STRATEGY: royalroad.com  (Tiếng Anh)
    // =========================================================================

    private static class RoyalRoadStrategy implements SiteStrategy {

        @Override
        public boolean supports(String url) {
            return url.contains("royalroad.com");
        }

        @Override
        public String baseUrl() {
            return "https://www.royalroad.com";
        }

        @Override
        public NovelInfo fetchNovelInfo(String novelUrl) throws IOException {
            Document doc = fetch(novelUrl);

            String title  = text(doc, "h1[property=name]");
            String author = text(doc, "span[property=name]");
            String desc   = text(doc, "div.description");

            List<ChapterLink> chapters = new ArrayList<>();
            Elements rows = doc.select("table#chapters tbody tr");
            int i = 1;
            for (Element row : rows) {
                Element a = row.selectFirst("td a[href]");
                if (a != null) {
                    // RoyalRoad có <time datetime="..."> trong cột ngày
                    LocalDate date = null;
                    Element timeEl = row.selectFirst("td time[datetime]");
                    if (timeEl != null) {
                        date = parseDate(timeEl.attr("datetime"));
                    }
                    chapters.add(new ChapterLink(i++, a.text(),
                            baseUrl() + a.attr("href"), date));
                }
            }
            log.info("[RoyalRoad] \"" + title + "\" – " + chapters.size() + " chapters");
            return new NovelInfo(title, author, desc, chapters);
        }

        @Override
        public ChapterContent fetchChapter(ChapterLink link) throws IOException {
            Document doc   = fetch(link.url());
            String   title = text(doc, "h1");
            doc.select("div.author-note-portlet").remove();
            String content = doc.select("div.chapter-content").text();
            return new ChapterContent(title.isBlank() ? link.title() : title,
                                      link.url(), content);
        }

        @Override
        public List<NovelUpdate> fetchLatestUpdates(int page) throws IOException {
            String url = baseUrl() + "/fictions/latest-updates?page=" + page;
            Document doc = fetch(url);
            List<NovelUpdate> updates = new ArrayList<>();

            Elements items = doc.select("div.fiction-list-item");
            for (Element item : items) {
                Element titleEl = item.selectFirst("h2.fiction-title a");
                if (titleEl == null) continue;

                String novelTitle = titleEl.text();
                String novelUrl2  = baseUrl() + titleEl.attr("href");

                Element chapEl = item.selectFirst("li.list-group-item a");
                String latestChap    = chapEl != null ? chapEl.text() : "";
                String latestChapUrl = chapEl != null ? (baseUrl() + chapEl.attr("href")) : "";

                LocalDate date = null;
                Element timeEl = item.selectFirst("li.list-group-item time[datetime]");
                if (timeEl != null) {
                    date = parseDate(timeEl.attr("datetime"));
                }
                updates.add(new NovelUpdate(novelTitle, novelUrl2, latestChap, latestChapUrl, date));
            }
            log.info("[RoyalRoad] Page " + page + " – " + updates.size() + " latest updates");
            return updates;
        }
    }

    // =========================================================================
    //  STRATEGY: 69shu.top (Trung Quốc – tiểu thuyết mạng)
    // =========================================================================

    private static class Shu69Strategy implements SiteStrategy {

        @Override
        public boolean supports(String url) {
            return url.contains("69shu.top") || url.contains("69shu.com")
                    || url.contains("69shuba.com");
        }

        @Override
        public String baseUrl() {
            return "https://www.69shu.top";
        }

        @Override
        public NovelInfo fetchNovelInfo(String novelUrl) throws IOException {
            Document doc = fetchWithCharset(novelUrl, "gbk");

            String title  = text(doc, "div.booknav2 h1, h1.bookTitle");
            String author = text(doc, "div.booknav2 p a, span.author");
            String desc   = text(doc, "div.navtxt p, div.bookIntro");

            // Trang danh sách chương thường ở /txt/{id}/
            String chapterListUrl = novelUrl;
            Element chapterPageLink = doc.selectFirst("a.more-btn, a[href*=txt]");
            if (chapterPageLink != null) {
                String href = chapterPageLink.absUrl("href");
                if (!href.isBlank()) chapterListUrl = href;
            }

            Document chapterDoc = chapterListUrl.equals(novelUrl) ? doc : fetchWithCharset(chapterListUrl, "gbk");
            List<ChapterLink> chapters = new ArrayList<>();
            Elements links = chapterDoc.select("ul.chapterlist li a, div.catalog ul li a");
            for (int i = 0; i < links.size(); i++) {
                Element a = links.get(i);
                chapters.add(new ChapterLink(i + 1, a.text(), a.absUrl("href"), null));
            }
            log.info("[69Shu] \"" + title + "\" – " + chapters.size() + " 章");
            return new NovelInfo(title, author, desc, chapters);
        }

        @Override
        public ChapterContent fetchChapter(ChapterLink link) throws IOException {
            Document doc   = fetchWithCharset(link.url(), "gbk");
            String   title = text(doc, "h1, h1.hide720");
            doc.select("script, div.ads, div.txtright").remove();
            String content = doc.select("div.txtnav, div.novelcontent").text();
            return new ChapterContent(title.isBlank() ? link.title() : title,
                                      link.url(), content);
        }

        @Override
        public List<NovelUpdate> fetchLatestUpdates(int page) throws IOException {
            String url = baseUrl() + "/paihangbang/lastupdate/" + page + ".htm";
            Document doc = fetchWithCharset(url, "gbk");
            List<NovelUpdate> updates = new ArrayList<>();

            Elements rows = doc.select("div.newbox ul li, ul.novellist li");
            for (Element row : rows) {
                Element titleEl = row.selectFirst("a.novalTitle, a:first-of-type");
                if (titleEl == null) continue;

                String novelTitle = titleEl.text();
                String novelUrl2  = titleEl.absUrl("href");

                Element chapEl = row.selectFirst("a.chapterTitle, a:nth-of-type(2)");
                String latestChap    = chapEl != null ? chapEl.text() : "";
                String latestChapUrl = chapEl != null ? chapEl.absUrl("href") : "";

                LocalDate date = parseDate(textEl(row, "span.updatetime, span.date, span:last-of-type"));
                updates.add(new NovelUpdate(novelTitle, novelUrl2, latestChap, latestChapUrl, date));
            }
            log.info("[69Shu] 页 " + page + " – " + updates.size() + " 最新更新");
            return updates;
        }
    }

    // =========================================================================
    //  STRATEGY: biquge5200.cc (Trung Quốc – biquge mirror)
    // =========================================================================

    private static class BiqugeStrategy implements SiteStrategy {

        @Override
        public boolean supports(String url) {
            return url.contains("biquge") || url.contains("xbiquge")
                    || url.contains("ibiquge") || url.contains("biquwu");
        }

        @Override
        public String baseUrl() {
            return "https://www.biquge5200.cc";
        }

        @Override
        public NovelInfo fetchNovelInfo(String novelUrl) throws IOException {
            Document doc = fetchWithCharset(novelUrl, "utf-8");

            String title  = text(doc, "div#info h1, h1.bookTitle");
            String author = text(doc, "div#info p:contains(作者), span.author");
            if (author.startsWith("作")) {
                author = author.replaceFirst("^作\\s*者[：:]?\\s*", "");
            }
            String desc = text(doc, "div#intro, div.bookIntro");

            List<ChapterLink> chapters = new ArrayList<>();
            Elements links = doc.select("div#list dd a, ul.chapter-list li a");
            for (int i = 0; i < links.size(); i++) {
                Element a = links.get(i);
                chapters.add(new ChapterLink(i + 1, a.text(), a.absUrl("href"), null));
            }
            log.info("[Biquge] \"" + title + "\" – " + chapters.size() + " 章");
            return new NovelInfo(title, author, desc, chapters);
        }

        @Override
        public ChapterContent fetchChapter(ChapterLink link) throws IOException {
            Document doc   = fetchWithCharset(link.url(), "utf-8");
            String   title = text(doc, "div.bookname h1, h1.wap_none");
            doc.select("script, div.ads, div.bottem").remove();
            String content = doc.select("div#content, div.showtxt").text();
            return new ChapterContent(title.isBlank() ? link.title() : title,
                                      link.url(), content);
        }

        @Override
        public List<NovelUpdate> fetchLatestUpdates(int page) throws IOException {
            String url = baseUrl() + "/lastupdate/p" + page + "/";
            Document doc = fetchWithCharset(url, "utf-8");
            List<NovelUpdate> updates = new ArrayList<>();

            Elements rows = doc.select("div#newscontent ul li, table tr, div.item");
            for (Element row : rows) {
                Element titleEl = row.selectFirst("span.s2 a, a.name, td:nth-child(1) a");
                if (titleEl == null) continue;

                String novelTitle = titleEl.text();
                String novelUrl2  = titleEl.absUrl("href");

                Element chapEl = row.selectFirst("span.s3 a, a.chapter, td:nth-child(2) a");
                String latestChap    = chapEl != null ? chapEl.text() : "";
                String latestChapUrl = chapEl != null ? chapEl.absUrl("href") : "";

                LocalDate date = parseDate(textEl(row, "span.s5, span.date, td:last-child"));
                updates.add(new NovelUpdate(novelTitle, novelUrl2, latestChap, latestChapUrl, date));
            }
            log.info("[Biquge] 页 " + page + " – " + updates.size() + " 最新更新");
            return updates;
        }
    }

    // =========================================================================
    //  STRATEGY REGISTRY & DISPATCHER
    // =========================================================================

    private final List<SiteStrategy> strategies = List.of(
            new TruyenFullStrategy(),
            new MeTruyenCVStrategy(),
            new RoyalRoadStrategy(),
            new Shu69Strategy(),
            new BiqugeStrategy()
    );

    private SiteStrategy resolve(String url) {
        return strategies.stream()
                .filter(s -> s.supports(url))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Chưa hỗ trợ trang: " + url));
    }

    /** Tìm strategy theo tên site (truyenfull, metruyencv, royalroad, 69shu, biquge) */
    private SiteStrategy resolveByName(String siteName) {
        String lower = siteName.toLowerCase();
        return strategies.stream()
                .filter(s -> s.baseUrl().toLowerCase().contains(lower)
                        || s.getClass().getSimpleName().toLowerCase().contains(lower))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Không tìm thấy site: " + siteName));
    }

    // =========================================================================
    //  PUBLIC API
    // =========================================================================

    /**
     * Lấy thông tin truyện + danh sách chương.
     */
    public NovelInfo getNovelInfo(String novelUrl) throws IOException {
        return resolve(novelUrl).fetchNovelInfo(novelUrl);
    }

    /**
     * Cào một chương duy nhất.
     */
    public ChapterContent getChapter(ChapterLink link) throws IOException {
        return resolve(link.url()).fetchChapter(link);
    }

    /**
     * Cào nhiều chương liên tiếp (from → to, 1-indexed, inclusive).
     * Có delay giữa mỗi request để tránh bị chặn.
     */
    public List<ChapterContent> getChapters(String novelUrl, int from, int to) throws IOException, InterruptedException {
        NovelInfo info = getNovelInfo(novelUrl);
        List<ChapterLink> all = info.chapterList();

        int start = Math.max(0, from - 1);
        int end   = Math.min(to, all.size());

        List<ChapterContent> result = new ArrayList<>();
        for (int i = start; i < end; i++) {
            ChapterLink link = all.get(i);
            log.info("Đang cào chương " + link.index() + "/" + end + ": " + link.title());
            result.add(getChapter(link));
            if (i < end - 1) Thread.sleep(POLITE_DELAY_MS);
        }
        return result;
    }

    /**
     * Cào toàn bộ chương của truyện.
     */
    public List<ChapterContent> getAllChapters(String novelUrl) throws IOException, InterruptedException {
        NovelInfo info = getNovelInfo(novelUrl);
        return getChapters(novelUrl, 1, info.chapterList().size());
    }

    // ── Lọc chapter theo ngày ────────────────────────────────────────────────

    /**
     * Lấy danh sách chapter được đăng/cập nhật vào đúng ngày chỉ định.
     */
    public List<ChapterLink> getChaptersByDate(String novelUrl, LocalDate date) throws IOException {
        NovelInfo info = getNovelInfo(novelUrl);
        return info.chapterList().stream()
                .filter(ch -> date.equals(ch.publishDate()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách chapter được đăng/cập nhật từ ngày {@code fromDate} trở đi (inclusive).
     */
    public List<ChapterLink> getChaptersSince(String novelUrl, LocalDate fromDate) throws IOException {
        NovelInfo info = getNovelInfo(novelUrl);
        return info.chapterList().stream()
                .filter(ch -> ch.publishDate() != null && !ch.publishDate().isBefore(fromDate))
                .collect(Collectors.toList());
    }

    // ── Truyện mới cập nhật (trang danh sách) ───────────────────────────────

    /**
     * Lấy danh sách truyện mới cập nhật từ một site cụ thể.
     * @param siteName tên site: "truyenfull", "metruyencv", "royalroad", "69shu", "biquge"
     * @param page     số trang (bắt đầu từ 1)
     */
    public List<NovelUpdate> getLatestUpdates(String siteName, int page) throws IOException {
        return resolveByName(siteName).fetchLatestUpdates(page);
    }

    /**
     * Lấy truyện mới cập nhật và lọc theo ngày.
     */
    public List<NovelUpdate> getLatestUpdatesByDate(String siteName, int page, LocalDate date) throws IOException {
        return resolveByName(siteName).fetchLatestUpdates(page).stream()
                .filter(u -> date.equals(u.updateDate()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy truyện mới cập nhật từ ngày {@code fromDate} trở đi.
     */
    public List<NovelUpdate> getLatestUpdatesSince(String siteName, int page, LocalDate fromDate) throws IOException {
        return resolveByName(siteName).fetchLatestUpdates(page).stream()
                .filter(u -> u.updateDate() != null && !u.updateDate().isBefore(fromDate))
                .collect(Collectors.toList());
    }

    // ── Batch: cào nhiều truyện cùng lúc ─────────────────────────────────────

    /** Giới hạn số truyện tối đa trong 1 lần batch */
    private static final int MAX_BATCH_SIZE = 20;

    /**
     * Cào thông tin nhiều truyện cùng lúc (tối đa {@value MAX_BATCH_SIZE} truyện).
     * @param novelUrls danh sách URL truyện
     * @return map URL → NovelInfo (bỏ qua URL lỗi, ghi log warning)
     */
    public Map<String, NovelInfo> batchGetNovelInfo(List<String> novelUrls) throws InterruptedException {
        List<String> urls = novelUrls.size() > MAX_BATCH_SIZE
                ? novelUrls.subList(0, MAX_BATCH_SIZE) : novelUrls;

        Map<String, NovelInfo> result = new LinkedHashMap<>();
        for (String url : urls) {
            try {
                result.put(url, getNovelInfo(url));
                log.info("[Batch] OK: " + url);
            } catch (Exception e) {
                log.warning("[Batch] Lỗi khi cào " + url + ": " + e.getMessage());
            }
            Thread.sleep(POLITE_DELAY_MS);
        }
        return result;
    }

    /**
     * Cào chapter mới (từ ngày chỉ định) của nhiều truyện cùng lúc.
     * @param novelUrls danh sách URL truyện
     * @param since     chỉ lấy chapter từ ngày này trở đi
     * @return map URL → danh sách ChapterLink mới
     */
    public Map<String, List<ChapterLink>> batchGetNewChapters(List<String> novelUrls, LocalDate since) throws InterruptedException {
        List<String> urls = novelUrls.size() > MAX_BATCH_SIZE
                ? novelUrls.subList(0, MAX_BATCH_SIZE) : novelUrls;

        Map<String, List<ChapterLink>> result = new LinkedHashMap<>();
        for (String url : urls) {
            try {
                List<ChapterLink> newChapters = getChaptersSince(url, since);
                result.put(url, newChapters);
                log.info("[Batch] " + url + " → " + newChapters.size() + " chapter mới từ " + since);
            } catch (Exception e) {
                log.warning("[Batch] Lỗi khi cào " + url + ": " + e.getMessage());
            }
            Thread.sleep(POLITE_DELAY_MS);
        }
        return result;
    }

    /**
     * Cào nội dung chapter mới (từ ngày chỉ định) của nhiều truyện.
     * @param novelUrls danh sách URL truyện (tối đa {@value MAX_BATCH_SIZE})
     * @param since     chỉ lấy chapter từ ngày này trở đi
     * @return map URL → danh sách ChapterContent
     */
    public Map<String, List<ChapterContent>> batchScrapeNewChapters(List<String> novelUrls, LocalDate since)
            throws InterruptedException {
        Map<String, List<ChapterLink>> newChaptersMap = batchGetNewChapters(novelUrls, since);
        Map<String, List<ChapterContent>> result = new LinkedHashMap<>();

        for (var entry : newChaptersMap.entrySet()) {
            List<ChapterContent> contents = new ArrayList<>();
            for (ChapterLink link : entry.getValue()) {
                try {
                    contents.add(getChapter(link));
                    Thread.sleep(POLITE_DELAY_MS);
                } catch (Exception e) {
                    log.warning("[Batch] Lỗi cào chapter " + link.title() + ": " + e.getMessage());
                }
            }
            result.put(entry.getKey(), contents);
        }
        return result;
    }

    /**
     * Xuất danh sách chương ra file text.
     */
    public void exportToTextFile(NovelInfo info, List<ChapterContent> chapters, String filePath) throws IOException {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            writer.println("═══════════════════════════════════════════════════");
            writer.println("  " + info.title());
            writer.println("  Tác giả: " + info.author());
            writer.println("═══════════════════════════════════════════════════");
            writer.println();
            if (!info.description().isBlank()) {
                writer.println("Mô tả:");
                writer.println(info.description());
                writer.println();
            }

            for (ChapterContent ch : chapters) {
                writer.println("───────────────────────────────────────────────────");
                writer.println("  Chương " + chapters.indexOf(ch) + ": " + ch.title());
                writer.println("───────────────────────────────────────────────────");
                writer.println();
                writer.println(ch.content());
                writer.println();
            }

            writer.println("═══════════════════════════════════════════════════");
            writer.println("  HẾT");
            writer.println("═══════════════════════════════════════════════════");
        }
        log.info("Đã xuất " + chapters.size() + " chương ra file: " + file.getAbsolutePath());
    }

    // =========================================================================
    //  HELPER
    // =========================================================================

    /** Jsoup GET với header browser hợp lệ */
    static Document fetch(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7,zh-CN;q=0.6,zh;q=0.5")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .referrer("https://www.google.com")
                .timeout(CONNECT_TIMEOUT_MS + READ_TIMEOUT_MS)
                .get();
    }

    /** Jsoup GET cho trang Trung Quốc (có thể dùng charset GBK) */
    static Document fetchWithCharset(String url, String charset) throws IOException {
        byte[] body = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .referrer("https://www.google.com")
                .timeout(CONNECT_TIMEOUT_MS + READ_TIMEOUT_MS)
                .execute()
                .bodyAsBytes();
        return Jsoup.parse(new String(body, charset), url);
    }

    static String text(Document doc, String cssSelector) {
        Element el = doc.selectFirst(cssSelector);
        return el != null ? el.text().strip() : "";
    }

    /** Lấy text từ một Element con (không phải Document) */
    static String textEl(Element parent, String cssSelector) {
        Element el = parent.selectFirst(cssSelector);
        return el != null ? el.text().strip() : "";
    }

    // ── Date parsing ─────────────────────────────────────────────────────────

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_DATE_TIME,
    };

    /**
     * Cố gắng parse chuỗi ngày từ nhiều format phổ biến.
     * Trả về null nếu không parse được.
     */
    static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Cắt bỏ phần time nếu có (vd: "2024-03-20T15:30:00Z" → "2024-03-20")
        String cleaned = raw.strip();
        // Xử lý ISO datetime
        if (cleaned.contains("T")) {
            cleaned = cleaned.substring(0, cleaned.indexOf('T'));
        }
        // Xử lý dạng "20 giờ trước", "3 ngày trước", "hôm nay", "hôm qua"
        if (cleaned.contains("hôm nay") || cleaned.contains("Hôm nay")) {
            return LocalDate.now();
        }
        if (cleaned.contains("hôm qua") || cleaned.contains("Hôm qua")) {
            return LocalDate.now().minusDays(1);
        }
        if (cleaned.contains("ngày trước")) {
            try {
                int days = Integer.parseInt(cleaned.replaceAll("[^0-9]", ""));
                return LocalDate.now().minusDays(days);
            } catch (NumberFormatException ignored) {}
        }
        if (cleaned.contains("giờ trước") || cleaned.contains("phút trước")) {
            return LocalDate.now();
        }
        // Xử lý tiếng Trung: 今天, 昨天, X天前
        if (cleaned.contains("今天")) return LocalDate.now();
        if (cleaned.contains("昨天")) return LocalDate.now().minusDays(1);
        if (cleaned.contains("天前")) {
            try {
                int days = Integer.parseInt(cleaned.replaceAll("[^0-9]", ""));
                return LocalDate.now().minusDays(days);
            } catch (NumberFormatException ignored) {}
        }

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        log.fine("Không parse được ngày: \"" + raw + "\"");
        return null;
    }

    // =========================================================================
    //  MAIN – demo chạy thử
    // =========================================================================

    public static void main(String[] args) throws Exception {
        NovelScraperService svc = new NovelScraperService();

        // ── Demo 1: Lấy truyện mới cập nhật hôm nay ─────────────────────────
        System.out.println("=== Truyện mới cập nhật trên TruyenFull ===");
        List<NovelUpdate> updates = svc.getLatestUpdates("truyenfull", 1);
        for (NovelUpdate u : updates) {
            System.out.printf("  [%s] %s → %s%n", u.updateDate(), u.title(), u.latestChapter());
        }

        // ── Demo 2: Lọc theo ngày hôm nay ───────────────────────────────────
        System.out.println("\n=== Chỉ lấy truyện cập nhật hôm nay ===");
        List<NovelUpdate> todayUpdates = svc.getLatestUpdatesByDate("truyenfull", 1, LocalDate.now());
        todayUpdates.forEach(u -> System.out.println("  " + u.title() + " → " + u.latestChapter()));

        // ── Demo 3: Batch cào nhiều truyện cùng lúc ──────────────────────────
        System.out.println("\n=== Batch: cào thông tin nhiều truyện ===");
        List<String> novelUrls = List.of(
                "https://truyenfull.vn/tieu-tho-yeu-ma/",
                "https://www.royalroad.com/fiction/21220/mother-of-learning"
        );
        Map<String, NovelInfo> batchInfo = svc.batchGetNovelInfo(novelUrls);
        for (var entry : batchInfo.entrySet()) {
            NovelInfo info = entry.getValue();
            System.out.printf("  %s – %s (%d chương)%n", info.title(), info.author(), info.chapterList().size());
        }

        // ── Demo 4: Lấy chapter mới từ 3 ngày trước ─────────────────────────
        System.out.println("\n=== Chapter mới từ 3 ngày trước ===");
        LocalDate since = LocalDate.now().minusDays(3);
        Map<String, List<ChapterLink>> newChapters = svc.batchGetNewChapters(novelUrls, since);
        for (var entry : newChapters.entrySet()) {
            System.out.println("  " + entry.getKey() + " → " + entry.getValue().size() + " chapter mới");
            entry.getValue().forEach(ch ->
                    System.out.printf("    [%s] %s%n", ch.publishDate(), ch.title()));
        }
    }
}
