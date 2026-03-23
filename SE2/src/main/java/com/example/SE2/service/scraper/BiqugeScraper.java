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
 * Scraper cho biquge / xbiquge / ibiquge (Trung Quốc – các mirror biquge).
 */
public class BiqugeScraper implements SiteStrategy {

    private static final Logger log = Logger.getLogger(BiqugeScraper.class.getName());

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
