package com.example.SE2.service;

import com.example.SE2.service.scraper.*;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

/**
 * NovelScraperService - Cào nội dung truyện từ nhiều trang web.
 *
 * Hỗ trợ: truyenfull.vn, metruyencv.com, royalroad.com, 69shu, biquge.
 * Mỗi site được tách thành 1 class riêng trong package {@code scraper}.
 */
public class NovelScraperService {

    private static final Logger log = Logger.getLogger(NovelScraperService.class.getName());
    private static final long POLITE_DELAY_MS = 1_500;

    // =========================================================================
    //  DATA MODELS
    // =========================================================================

    public record NovelInfo(
            String title,
            String author,
            String description,
            List<ChapterLink> chapterList,
            List<String> genres,
            String coverImgUrl,
            String status
    ) {
        /** Backward-compatible constructor (no genres/cover/status) */
        public NovelInfo(String title, String author, String description, List<ChapterLink> chapterList) {
            this(title, author, description, chapterList, List.of(), null, null);
        }
    }

    public record ChapterLink(
            int       index,
            String    title,
            String    url,
            LocalDate publishDate
    ) {
        public ChapterLink(int index, String title, String url) {
            this(index, title, url, null);
        }
    }

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
            String content
    ) {}

    // =========================================================================
    //  STRATEGY REGISTRY
    // =========================================================================

    private final List<SiteStrategy> strategies = List.of(
            new TruyenFullScraper(),
            new MeTruyenCVScraper(),
            new RoyalRoadScraper(),
            new Shu69Scraper(),
            new BiqugeScraper()
    );

    private SiteStrategy resolve(String url) {
        return strategies.stream()
                .filter(s -> s.supports(url))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Chưa hỗ trợ trang: " + url));
    }

    private SiteStrategy resolveByName(String siteName) {
        String lower = siteName.toLowerCase();
        return strategies.stream()
                .filter(s -> s.baseUrl().toLowerCase().contains(lower)
                        || s.getClass().getSimpleName().toLowerCase().contains(lower))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Không tìm thấy site: " + siteName));
    }

    // =========================================================================
    //  PUBLIC API
    // =========================================================================

    /** Lấy danh sách truyện mới cập nhật từ một site (phân trang). */
    public List<NovelUpdate> getLatestUpdates(String siteName, int page) throws IOException {
        return resolveByName(siteName).fetchLatestUpdates(page);
    }

    /** Lấy thông tin truyện + danh sách chương. */
    public NovelInfo getNovelInfo(String novelUrl) throws IOException {
        return resolve(novelUrl).fetchNovelInfo(novelUrl);
    }

    /** Cào nội dung 1 chương. */
    public ChapterContent getChapter(ChapterLink link) throws IOException {
        return resolve(link.url()).fetchChapter(link);
    }

    /** Cào nhiều chương liên tiếp (from → to, 1-indexed, inclusive). */
    public List<ChapterContent> getChapters(String novelUrl, int from, int to)
            throws IOException, InterruptedException {
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

    /** Cào toàn bộ chương. */
    public List<ChapterContent> getAllChapters(String novelUrl) throws IOException, InterruptedException {
        NovelInfo info = getNovelInfo(novelUrl);
        return getChapters(novelUrl, 1, info.chapterList().size());
    }

    /** Xuất nội dung chương ra file text. */
    public void exportToTextFile(NovelInfo info, List<ChapterContent> chapters, String filePath)
            throws IOException {
        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            w.println("═══════════════════════════════════════════════════");
            w.println("  " + info.title());
            w.println("  Tác giả: " + info.author());
            w.println("═══════════════════════════════════════════════════");
            w.println();
            if (!info.description().isBlank()) {
                w.println("Mô tả:");
                w.println(info.description());
                w.println();
            }
            for (int i = 0; i < chapters.size(); i++) {
                ChapterContent ch = chapters.get(i);
                w.println("───────────────────────────────────────────────────");
                w.println("  Chương " + (i + 1) + ": " + ch.title());
                w.println("───────────────────────────────────────────────────");
                w.println();
                w.println(ch.content());
                w.println();
            }
            w.println("═══════════════════════════════════════════════════");
            w.println("  HẾT");
            w.println("═══════════════════════════════════════════════════");
        }
        log.info("Đã xuất " + chapters.size() + " chương ra file: " + file.getAbsolutePath());
    }
}
