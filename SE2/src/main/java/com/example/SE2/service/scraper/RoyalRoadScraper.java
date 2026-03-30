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
 * Scraper cho royalroad.com (Tiếng Anh).
 */
public class RoyalRoadScraper implements SiteStrategy {

    private static final Logger log = Logger.getLogger(RoyalRoadScraper.class.getName());

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

        String title  = text(doc, "h1.font-white, h1.fic-title, h1[property=name]");
        String author = text(doc, "h4.font-white a, a[href*=/profile/], span[property=name]");
        String desc   = text(doc, "div.description, div.hidden-content");

        // Genres (tags)
        List<String> genres = new ArrayList<>();
        for (Element tag : doc.select("span.tags a.fiction-tag, a.tag")) {
            if (!tag.text().isBlank()) genres.add(tag.text().trim());
        }

        // Cover image
        Element imgEl = doc.selectFirst("div.fic-header img[src], img.thumbnail");
        String coverUrl = imgEl != null ? imgEl.absUrl("src") : null;

        // Status
        String status = text(doc, "span.label-default, span.fiction-status");

        List<ChapterLink> chapters = new ArrayList<>();
        Elements rows = doc.select("table#chapters tbody tr");
        int i = 1;
        for (Element row : rows) {
            Element a = row.selectFirst("td a[href]");
            if (a != null) {
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
        return new NovelInfo(title, author, desc, chapters, genres, coverUrl, status);
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
