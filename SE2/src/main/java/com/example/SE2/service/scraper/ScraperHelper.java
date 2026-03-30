package com.example.SE2.service.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

/**
 * Utility dùng chung cho tất cả các SiteStrategy: fetch HTML, parse text, parse date.
 */
public final class ScraperHelper {

    private static final Logger log = Logger.getLogger(ScraperHelper.class.getName());

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 15_000;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36";

    private ScraperHelper() {}

    // ── Fetch ────────────────────────────────────────────────────────────────

    /** GET với header browser hợp lệ (UTF-8 mặc định) */
    public static Document fetch(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7,zh-CN;q=0.6,zh;q=0.5")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .referrer("https://www.google.com")
                .timeout(CONNECT_TIMEOUT_MS + READ_TIMEOUT_MS)
                .get();
    }

    /** GET cho trang Trung Quốc (hỗ trợ charset GBK / custom) */
    public static Document fetchWithCharset(String url, String charset) throws IOException {
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

    // ── Text extraction ─────────────────────────────────────────────────────

    /** Lấy text từ Document theo CSS selector */
    public static String text(Document doc, String cssSelector) {
        Element el = doc.selectFirst(cssSelector);
        return el != null ? el.text().strip() : "";
    }

    /** Lấy text từ Element con theo CSS selector */
    public static String textEl(Element parent, String cssSelector) {
        Element el = parent.selectFirst(cssSelector);
        return el != null ? el.text().strip() : "";
    }

    // ── Date parsing ────────────────────────────────────────────────────────

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
     * Parse chuỗi ngày từ nhiều format phổ biến (VN, CN, EN, ISO).
     * @return LocalDate hoặc null nếu không parse được
     */
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String cleaned = raw.strip();

        // ISO datetime → cắt phần time
        if (cleaned.contains("T")) {
            cleaned = cleaned.substring(0, cleaned.indexOf('T'));
        }

        // Tiếng Việt
        if (cleaned.contains("hôm nay") || cleaned.contains("Hôm nay")) return LocalDate.now();
        if (cleaned.contains("hôm qua") || cleaned.contains("Hôm qua")) return LocalDate.now().minusDays(1);
        if (cleaned.contains("ngày trước")) return parseDaysAgo(cleaned);
        if (cleaned.contains("giờ trước") || cleaned.contains("phút trước")) return LocalDate.now();

        // Tiếng Trung
        if (cleaned.contains("今天")) return LocalDate.now();
        if (cleaned.contains("昨天")) return LocalDate.now().minusDays(1);
        if (cleaned.contains("天前")) return parseDaysAgo(cleaned);

        // Standard formats
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {}
        }

        log.fine("Không parse được ngày: \"" + raw + "\"");
        return null;
    }

    private static LocalDate parseDaysAgo(String text) {
        try {
            int days = Integer.parseInt(text.replaceAll("[^0-9]", ""));
            return LocalDate.now().minusDays(days);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
