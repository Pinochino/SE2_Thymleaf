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
 * Scraper cho 69shu.top / 69shu.com (Trung Quốc – tiểu thuyết mạng).
 */
public class Shu69Scraper implements SiteStrategy {

    private static final Logger log = Logger.getLogger(Shu69Scraper.class.getName());

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

        // Genres
        List<String> genres = new ArrayList<>();
        for (Element a : doc.select("div.booknav2 a[href*=sort], a[href*=class]")) {
            if (!a.text().isBlank()) genres.add(a.text().trim());
        }

        // Cover image
        Element imgEl = doc.selectFirst("div.bookimg2 img[src], img.bookcover");
        String coverUrl = imgEl != null ? imgEl.absUrl("src") : null;

        // Status
        String status = text(doc, "div.booknav2 p:contains(状态), span.bookstatus");

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
        return new NovelInfo(title, author, desc, chapters, genres, coverUrl, status);
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
