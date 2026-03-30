package com.example.SE2.service;

import com.example.SE2.service.NovelScraperService.*;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * NovelUpdateService - Chuyên cào truyện mới cập nhật & batch scraping.
 *
 * Hai nhiệm vụ chính:
 *   1. Cào ban đầu (initial): lấy 50 truyện phổ biến/mới từ trang danh sách
 *   2. Cào cập nhật (update): lấy truyện/chapter mới theo khoảng thời gian
 *      (hôm nay, tuần này, tháng này, tuỳ chỉnh)
 */
public class NovelUpdateService {

    private static final Logger log = Logger.getLogger(NovelUpdateService.class.getName());

    private final NovelScraperService scraper;

    /** Giới hạn tối đa truyện trong 1 lần batch */
    private static final int MAX_BATCH_SIZE = 50;

    /** Delay giữa mỗi request (ms) */
    private static final long POLITE_DELAY_MS = 1_500;

    public NovelUpdateService() {
        this.scraper = new NovelScraperService();
    }

    public NovelUpdateService(NovelScraperService scraper) {
        this.scraper = scraper;
    }

    // =========================================================================
    //  ENUM: khoảng thời gian
    // =========================================================================

    public enum TimePeriod {
        TODAY(0),
        LAST_3_DAYS(3),
        LAST_WEEK(7),
        LAST_2_WEEKS(14),
        LAST_MONTH(30);

        private final int days;

        TimePeriod(int days) { this.days = days; }

        public LocalDate sinceDate() {
            return this == TODAY ? LocalDate.now() : LocalDate.now().minusDays(days);
        }
    }

    // =========================================================================
    //  1. CÀO BAN ĐẦU – lấy N truyện từ trang danh sách mới cập nhật
    // =========================================================================

    /**
     * Cào ban đầu: lấy tối đa {@code count} truyện mới cập nhật từ site.
     * Duyệt qua nhiều trang cho đến khi đủ số lượng hoặc hết trang.
     *
     * @param siteName tên site: "truyenfull", "metruyencv", "royalroad", "69shu", "biquge"
     * @param count    số truyện muốn lấy (tối đa {@value MAX_BATCH_SIZE})
     * @return danh sách NovelUpdate
     */
    public List<NovelUpdate> scrapeInitial(String siteName, int count) throws IOException, InterruptedException {
        int target = Math.min(count, MAX_BATCH_SIZE);
        List<NovelUpdate> result = new ArrayList<>();
        int page = 1;
        int maxPages = 10; // giới hạn để tránh cào vô hạn

        while (result.size() < target && page <= maxPages) {
            try {
                List<NovelUpdate> updates = scraper.getLatestUpdates(siteName, page);
                if (updates.isEmpty()) break; // hết dữ liệu

                result.addAll(updates);
                log.info("[Initial] " + siteName + " trang " + page + " → " + updates.size()
                        + " truyện (tổng: " + result.size() + "/" + target + ")");
                page++;
                if (result.size() < target) Thread.sleep(POLITE_DELAY_MS);
            } catch (Exception e) {
                log.warning("[Initial] Lỗi trang " + page + ": " + e.getMessage());
                break;
            }
        }

        return result.size() > target ? result.subList(0, target) : result;
    }

    /**
     * Cào ban đầu mặc định: lấy 50 truyện.
     */
    public List<NovelUpdate> scrapeInitial(String siteName) throws IOException, InterruptedException {
        return scrapeInitial(siteName, MAX_BATCH_SIZE);
    }

    // =========================================================================
    //  2. CÀO CẬP NHẬT – lấy truyện/chapter mới theo khoảng thời gian
    // =========================================================================

    /**
     * Lấy truyện mới cập nhật trong khoảng thời gian chỉ định.
     * Duyệt nhiều trang và dừng khi gặp truyện cũ hơn khoảng thời gian.
     */
    public List<NovelUpdate> scrapeUpdates(String siteName, TimePeriod period) throws IOException, InterruptedException {
        return scrapeUpdatesSince(siteName, period.sinceDate());
    }

    /**
     * Lấy truyện mới cập nhật từ ngày {@code since} trở đi.
     * Tự động duyệt nhiều trang, dừng khi gặp truyện cũ hơn.
     */
    public List<NovelUpdate> scrapeUpdatesSince(String siteName, LocalDate since) throws IOException, InterruptedException {
        List<NovelUpdate> result = new ArrayList<>();
        int page = 1;
        int maxPages = 20;

        while (page <= maxPages) {
            try {
                List<NovelUpdate> updates = scraper.getLatestUpdates(siteName, page);
                if (updates.isEmpty()) break;

                boolean hasOldEntries = false;
                for (NovelUpdate u : updates) {
                    if (u.updateDate() == null || !u.updateDate().isBefore(since)) {
                        result.add(u);
                    } else {
                        hasOldEntries = true;
                    }
                }

                log.info("[Update] " + siteName + " trang " + page + " → "
                        + result.size() + " truyện từ " + since);

                // Nếu trang này đã có entry cũ hơn since → không cần duyệt tiếp
                if (hasOldEntries) break;

                page++;
                Thread.sleep(POLITE_DELAY_MS);
            } catch (Exception e) {
                log.warning("[Update] Lỗi trang " + page + ": " + e.getMessage());
                break;
            }
        }
        return result;
    }

    /**
     * Lấy truyện cập nhật đúng ngày chỉ định.
     */
    public List<NovelUpdate> scrapeUpdatesByDate(String siteName, LocalDate date) throws IOException, InterruptedException {
        List<NovelUpdate> all = scrapeUpdatesSince(siteName, date);
        return all.stream()
                .filter(u -> date.equals(u.updateDate()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    //  3. BATCH – cào info/chapter mới của nhiều truyện
    // =========================================================================

    /**
     * Cào thông tin nhiều truyện cùng lúc (tối đa {@value MAX_BATCH_SIZE}).
     * @return map URL → NovelInfo (bỏ qua URL lỗi)
     */
    public Map<String, NovelInfo> batchGetNovelInfo(List<String> novelUrls) throws InterruptedException {
        List<String> urls = novelUrls.size() > MAX_BATCH_SIZE
                ? novelUrls.subList(0, MAX_BATCH_SIZE) : novelUrls;

        Map<String, NovelInfo> result = new LinkedHashMap<>();
        for (String url : urls) {
            try {
                result.put(url, scraper.getNovelInfo(url));
                log.info("[Batch] OK: " + url);
            } catch (Exception e) {
                log.warning("[Batch] Lỗi: " + url + " – " + e.getMessage());
            }
            Thread.sleep(POLITE_DELAY_MS);
        }
        return result;
    }

    /**
     * Lấy chapter mới của nhiều truyện (từ ngày chỉ định).
     */
    public Map<String, List<ChapterLink>> batchGetNewChapters(List<String> novelUrls, LocalDate since)
            throws InterruptedException {
        List<String> urls = novelUrls.size() > MAX_BATCH_SIZE
                ? novelUrls.subList(0, MAX_BATCH_SIZE) : novelUrls;

        Map<String, List<ChapterLink>> result = new LinkedHashMap<>();
        for (String url : urls) {
            try {
                NovelInfo info = scraper.getNovelInfo(url);
                List<ChapterLink> newChapters = info.chapterList().stream()
                        .filter(ch -> ch.publishDate() != null && !ch.publishDate().isBefore(since))
                        .collect(Collectors.toList());
                result.put(url, newChapters);
                log.info("[Batch] " + url + " → " + newChapters.size() + " chapter mới từ " + since);
            } catch (Exception e) {
                log.warning("[Batch] Lỗi: " + url + " – " + e.getMessage());
            }
            Thread.sleep(POLITE_DELAY_MS);
        }
        return result;
    }

    /**
     * Lấy chapter mới theo khoảng thời gian.
     */
    public Map<String, List<ChapterLink>> batchGetNewChapters(List<String> novelUrls, TimePeriod period)
            throws InterruptedException {
        return batchGetNewChapters(novelUrls, period.sinceDate());
    }

    /**
     * Cào nội dung chapter mới của nhiều truyện.
     */
    public Map<String, List<ChapterContent>> batchScrapeNewChapters(List<String> novelUrls, LocalDate since)
            throws InterruptedException {
        Map<String, List<ChapterLink>> newChaptersMap = batchGetNewChapters(novelUrls, since);
        Map<String, List<ChapterContent>> result = new LinkedHashMap<>();

        for (var entry : newChaptersMap.entrySet()) {
            List<ChapterContent> contents = new ArrayList<>();
            for (ChapterLink link : entry.getValue()) {
                try {
                    contents.add(scraper.getChapter(link));
                    Thread.sleep(POLITE_DELAY_MS);
                } catch (Exception e) {
                    log.warning("[Batch] Lỗi cào chapter " + link.title() + ": " + e.getMessage());
                }
            }
            result.put(entry.getKey(), contents);
        }
        return result;
    }

    // =========================================================================
    //  XUẤT KẾT QUẢ RA FILE TXT
    // =========================================================================

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    /**
     * Xuất danh sách truyện cập nhật ra file txt.
     * @param updates  danh sách truyện
     * @param label    nhãn mô tả (vd: "initial_royalroad", "update_1_thang")
     * @return đường dẫn file đã xuất
     */
    public String exportUpdatesToFile(List<NovelUpdate> updates, String label) throws IOException {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String safeLabel = label.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String filePath = "output/" + safeLabel + "_" + timestamp + ".txt";

        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            w.println("═══════════════════════════════════════════════════════════════");
            w.printf("  %s – %s%n", label, timestamp);
            w.printf("  Tổng: %d truyện%n", updates.size());
            w.println("═══════════════════════════════════════════════════════════════");
            w.println();

            int i = 1;
            for (NovelUpdate u : updates) {
                w.printf("%3d. [%s] %s%n", i++, u.updateDate() != null ? u.updateDate() : "N/A", u.title());
                w.printf("     URL:     %s%n", u.novelUrl());
                if (!u.latestChapter().isBlank()) {
                    w.printf("     Chapter: %s%n", u.latestChapter());
                    if (!u.latestChapterUrl().isBlank()) {
                        w.printf("     Ch. URL: %s%n", u.latestChapterUrl());
                    }
                }
                w.println();
            }

            w.println("═══════════════════════════════════════════════════════════════");
        }

        log.info("Đã xuất " + updates.size() + " truyện ra: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    /**
     * Xuất kết quả batch chapter mới ra file txt.
     */
    public String exportNewChaptersToFile(Map<String, List<ChapterLink>> chaptersMap, String label) throws IOException {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String safeLabel = label.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String filePath = "output/" + safeLabel + "_" + timestamp + ".txt";

        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        int totalChapters = chaptersMap.values().stream().mapToInt(List::size).sum();

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            w.println("═══════════════════════════════════════════════════════════════");
            w.printf("  %s – %s%n", label, timestamp);
            w.printf("  %d truyện, %d chapter mới%n", chaptersMap.size(), totalChapters);
            w.println("═══════════════════════════════════════════════════════════════");
            w.println();

            for (var entry : chaptersMap.entrySet()) {
                w.println("───────────────────────────────────────────────────────────────");
                w.println("  " + entry.getKey());
                w.printf("  %d chapter mới%n", entry.getValue().size());
                w.println("───────────────────────────────────────────────────────────────");
                for (ChapterLink ch : entry.getValue()) {
                    w.printf("  [%s] %s%n", ch.publishDate() != null ? ch.publishDate() : "N/A", ch.title());
                    w.printf("         %s%n", ch.url());
                }
                w.println();
            }

            w.println("═══════════════════════════════════════════════════════════════");
        }

        log.info("Đã xuất " + totalChapters + " chapter mới ra: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    // =========================================================================
    //  CÀO CONTENT CHAPTER CHO TỪNG TRUYỆN
    // =========================================================================

    /** Kết quả cào gồm thông tin truyện + nội dung chapter mới nhất */
    public record NovelWithContent(
            NovelUpdate update,
            NovelInfo   info,
            List<ChapterContent> chapters
    ) {}

    /**
     * Cào danh sách truyện cập nhật, sau đó lấy nội dung chapter mới nhất của từng truyện.
     * @param siteName   tên site
     * @param period     khoảng thời gian
     * @param maxNovels  giới hạn số truyện sẽ cào content (tránh cào quá nhiều)
     * @param chaptersPerNovel số chapter mới nhất cần lấy content cho mỗi truyện
     */
    public List<NovelWithContent> scrapeUpdatesWithContent(
            String siteName, TimePeriod period, int maxNovels, int chaptersPerNovel)
            throws IOException, InterruptedException {
        return scrapeUpdatesWithContentSince(siteName, period.sinceDate(), maxNovels, chaptersPerNovel);
    }

    /**
     * Cào danh sách truyện cập nhật từ ngày {@code since}, kèm nội dung chapter.
     */
    public List<NovelWithContent> scrapeUpdatesWithContentSince(
            String siteName, LocalDate since, int maxNovels, int chaptersPerNovel)
            throws IOException, InterruptedException {

        List<NovelUpdate> updates = scrapeUpdatesSince(siteName, since);
        int limit = Math.min(updates.size(), Math.min(maxNovels, MAX_BATCH_SIZE));

        List<NovelWithContent> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            NovelUpdate u = updates.get(i);
            try {
                log.info("[Content] " + (i + 1) + "/" + limit + " Đang cào: " + u.title());
                NovelInfo info = scraper.getNovelInfo(u.novelUrl());
                Thread.sleep(POLITE_DELAY_MS);

                // Lấy N chapter cuối cùng (mới nhất)
                List<ChapterLink> allChapters = info.chapterList();
                int from = Math.max(0, allChapters.size() - chaptersPerNovel);
                List<ChapterContent> contents = new ArrayList<>();

                for (int j = from; j < allChapters.size(); j++) {
                    ChapterLink ch = allChapters.get(j);
                    try {
                        contents.add(scraper.getChapter(ch));
                        Thread.sleep(POLITE_DELAY_MS);
                    } catch (Exception e) {
                        log.warning("[Content] Lỗi cào chapter \"" + ch.title() + "\": " + e.getMessage());
                    }
                }

                result.add(new NovelWithContent(u, info, contents));
                log.info("[Content] OK: " + u.title() + " – " + contents.size() + " chapter");
            } catch (Exception e) {
                log.warning("[Content] Lỗi truyện \"" + u.title() + "\": " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Xuất danh sách truyện kèm nội dung chapter ra file txt.
     */
    public String exportUpdatesWithContentToFile(List<NovelWithContent> novels, String label)
            throws IOException {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String safeLabel = label.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String filePath = "output/" + safeLabel + "_" + timestamp + ".txt";

        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        int totalChapters = novels.stream().mapToInt(n -> n.chapters().size()).sum();

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            w.println("═══════════════════════════════════════════════════════════════");
            w.printf("  %s – %s%n", label, timestamp);
            w.printf("  %d truyện, %d chapter (có nội dung)%n", novels.size(), totalChapters);
            w.println("═══════════════════════════════════════════════════════════════");
            w.println();

            int idx = 1;
            for (NovelWithContent n : novels) {
                NovelUpdate u = n.update();
                NovelInfo info = n.info();

                w.println("▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
                w.printf("  %d. %s%n", idx++, info != null ? info.title() : u.title());
                if (info != null) {
                    w.printf("  Tác giả: %s%n", info.author());
                    w.printf("  Tổng chương: %d%n", info.chapterList().size());
                }
                w.printf("  Ngày cập nhật: %s%n", u.updateDate() != null ? u.updateDate() : "N/A");
                w.printf("  URL: %s%n", u.novelUrl());
                w.println("▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");

                if (info != null && !info.description().isBlank()) {
                    w.println();
                    w.println("  Mô tả: " + info.description());
                }
                w.println();

                if (n.chapters().isEmpty()) {
                    w.println("  (Không lấy được nội dung chapter)");
                    w.println();
                    continue;
                }

                for (ChapterContent ch : n.chapters()) {
                    w.println("───────────────────────────────────────────────────────────────");
                    w.println("  " + ch.title());
                    w.println("  " + ch.url());
                    w.println("───────────────────────────────────────────────────────────────");
                    w.println();
                    w.println(ch.content());
                    w.println();
                }
            }

            w.println("═══════════════════════════════════════════════════════════════");
            w.println("  HẾT – " + novels.size() + " truyện, " + totalChapters + " chapter");
            w.println("═══════════════════════════════════════════════════════════════");
        }

        log.info("Đã xuất " + novels.size() + " truyện (" + totalChapters + " chapter) ra: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    // =========================================================================
    //  MAIN – demo
    // =========================================================================

    public static void main(String[] args) throws Exception {
        NovelUpdateService updateSvc = new NovelUpdateService();

        String[] sites = {"royalroad", "truyenfull", "metruyencv", "biquge", "69shu"};

        // ── Cào 20 truyện, mỗi truyện 2 chapter mới nhất ──
        for (String site : sites) {
            System.out.println("\n=== [Crawl] 20 truyện + 2 chapter từ " + site + " ===");
            try {
                List<NovelWithContent> novels = updateSvc.scrapeUpdatesWithContent(
                        site, TimePeriod.LAST_MONTH, 20, 2);
                printNovelsSummary(novels);
                if (!novels.isEmpty()) {
                    String path = updateSvc.exportUpdatesWithContentToFile(novels, "crawl_20_" + site);
                    System.out.println("  >> Đã xuất ra: " + path);
                }
                break;
            } catch (Exception e) {
                System.out.println("  Lỗi: " + e.getMessage());
            }
        }
    }

    private static void printNovelsSummary(List<NovelWithContent> novels) {
        for (NovelWithContent n : novels) {
            System.out.printf("  %s (%d chapter)%n", n.update().title(), n.chapters().size());
            for (ChapterContent ch : n.chapters()) {
                String preview = ch.content().length() > 150
                        ? ch.content().substring(0, 150) + "…" : ch.content();
                System.out.println("    >> " + ch.title());
                System.out.println("       " + preview);
            }
        }
    }
}
