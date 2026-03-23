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
 * Scraper cho metruyencv.com / metruyenvip.com (Tiếng Việt).
 */
public class MeTruyenCVScraper implements SiteStrategy {

    private static final Logger log = Logger.getLogger(MeTruyenCVScraper.class.getName());

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
