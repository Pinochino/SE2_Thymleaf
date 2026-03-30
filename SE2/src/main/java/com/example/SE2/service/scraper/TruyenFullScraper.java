package com.example.SE2.service.scraper;

import com.example.SE2.service.NovelScraperService.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.example.SE2.service.scraper.ScraperHelper.*;

/**
 * Scraper cho truyenfull.vn (Tiếng Việt).
 */
public class TruyenFullScraper implements SiteStrategy {

    private static final Logger log = Logger.getLogger(TruyenFullScraper.class.getName());

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

        // Genres
        List<String> genres = new ArrayList<>();
        for (Element a : doc.select("div.info-holder a[href*=the-loai]")) {
            if (!a.text().isBlank()) genres.add(a.text().trim());
        }

        // Cover image
        Element imgEl = doc.selectFirst("div.book img[src]");
        String coverUrl = imgEl != null ? imgEl.absUrl("src") : null;

        // Status
        String status = text(doc, "div.info-holder span.text-success, div.info-holder span.text-primary");

        List<ChapterLink> chapters = new ArrayList<>();
        Elements items = doc.select("ul.list-chapter li");
        for (int i = 0; i < items.size(); i++) {
            Element li = items.get(i);
            Element a  = li.selectFirst("a");
            if (a == null) continue;
            LocalDate date = parseDate(textEl(li, "span.text-muted"));
            chapters.add(new ChapterLink(i + 1, a.text(), a.absUrl("href"), date));
        }
        log.info("[TruyenFull] \"" + title + "\" – " + chapters.size() + " chương (trang 1)");
        return new NovelInfo(title, author, desc, chapters, genres, coverUrl, status);
    }

    @Override
    public ChapterContent fetchChapter(ChapterLink link) throws IOException {
        Document doc   = fetch(link.url());
        String   title = text(doc, "a.chapter-title");
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
