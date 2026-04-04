package com.example.SE2.service;

import com.example.SE2.models.Chapter;
import com.example.SE2.models.Genre;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.ChapterRepository;
import com.example.SE2.repositories.NovelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * NovelDataExportService - Export dữ liệu Novel/Chapter từ DB ra file.
 *
 * Hỗ trợ export:
 *   - TXT (đọc được, dạng text thuần)
 *   - JSON (import lại hoặc dùng cho API)
 *   - CSV (dùng cho Excel/Google Sheets)
 */
@Service
public class NovelDataExportService {

    private static final Logger log = Logger.getLogger(NovelDataExportService.class.getName());
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;

    public NovelDataExportService(NovelRepository novelRepository,
                                  ChapterRepository chapterRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
    }

    // =========================================================================
    //  EXPORT TXT
    // =========================================================================

    /**
     * Export tất cả truyện (kèm chapter) ra file TXT.
     */
    @Transactional(readOnly = true)
    public String exportAllToTxt(String label) throws IOException {
        List<Novel> novels = novelRepository.findAll();
        return exportNovelsToTxt(novels, label);
    }

    /**
     * Export 1 truyện (kèm chapter) ra file TXT.
     */
    @Transactional(readOnly = true)
    public String exportNovelToTxt(Long novelId) throws IOException {
        Novel novel = novelRepository.findById(novelId).orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy truyện ID: " + novelId));
        return exportNovelsToTxt(List.of(novel), "novel_" + novel.getId());
    }

    @Transactional(readOnly = true)
    public String exportNovelsToTxt(List<Novel> novels, String label) throws IOException {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String safeLabel = label.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String filePath = "output/export_" + safeLabel + "_" + timestamp + ".txt";

        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        int totalChapters = 0;

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            w.println("═══════════════════════════════════════════════════════════════");
            w.printf("  EXPORT: %s — %s%n", label, timestamp);
            w.printf("  Tổng: %d truyện%n", novels.size());
            w.println("═══════════════════════════════════════════════════════════════");
            w.println();

            int idx = 1;
            for (Novel novel : novels) {
                List<Chapter> chapters = chapterRepository
                        .findByNovelIdOrderByChapterNumberAsc(novel.getId());

                // Genres
                Set<Genre> genres = novel.getGenres();
                String genreStr = genres.isEmpty() ? "N/A"
                        : genres.stream().map(Genre::getName).collect(Collectors.joining(", "));

                w.println("───────────────────────────────────────────────────────────");
                w.printf("  %d. %s%n", idx++, novel.getTitle());
                w.printf("  Tác giả: %s%n", novel.getAuthor() != null ? novel.getAuthor() : "N/A");
                w.printf("  Thể loại: %s%n", genreStr);
                w.printf("  Trạng thái: %s%n", novel.getStatus() != null ? novel.getStatus() : "N/A");
                w.printf("  Tổng chương: %d%n", chapters.size());
                if (novel.getCoverImgUrl() != null) {
                    w.printf("  Cover: %s%n", novel.getCoverImgUrl());
                }
                w.println("───────────────────────────────────────────────────────────");

                if (novel.getDescription() != null && !novel.getDescription().isBlank()) {
                    w.println();
                    w.println("  Mô tả: " + novel.getDescription());
                }
                w.println();

                for (Chapter ch : chapters) {
                    w.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
                    w.printf("  Chương %d: %s%n", ch.getChapterNumber(), ch.getTitle());
                    w.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
                    if (ch.getContent() != null && !ch.getContent().isBlank()) {
                        w.println();
                        w.println(ch.getContent());
                    } else {
                        w.println("  (Chưa có nội dung)");
                    }
                    w.println();
                }
                totalChapters += chapters.size();
            }

            w.println("═══════════════════════════════════════════════════════════════");
            w.printf("  HẾT — %d truyện, %d chương%n", novels.size(), totalChapters);
            w.println("═══════════════════════════════════════════════════════════════");
        }

        log.info("Đã export " + novels.size() + " truyện (" + totalChapters + " chương) ra: "
                + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    // =========================================================================
    //  EXPORT JSON
    // =========================================================================

    /** DTO cho JSON export */
    public record NovelExportDto(
            Long id, String title, String author, String description,
            String status, String coverImgUrl, Float averageRating,
            List<String> genres, List<ChapterExportDto> chapters
    ) {}

    public record ChapterExportDto(
            Long id, Long chapterNumber, String title, String content, Integer paragraphs
    ) {}

    /**
     * Export tất cả truyện ra file JSON.
     */
    @Transactional(readOnly = true)
    public String exportAllToJson(String label) throws IOException {
        List<Novel> novels = novelRepository.findAll();
        return exportNovelsToJson(novels, label);
    }

    /**
     * Export 1 truyện ra file JSON.
     */
    @Transactional(readOnly = true)
    public String exportNovelToJson(Long novelId) throws IOException {
        Novel novel = novelRepository.findById(novelId).orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy truyện ID: " + novelId));
        return exportNovelsToJson(List.of(novel), "novel_" + novel.getId());
    }

    @Transactional(readOnly = true)
    public String exportNovelsToJson(List<Novel> novels, String label) throws IOException {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String safeLabel = label.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String filePath = "output/export_" + safeLabel + "_" + timestamp + ".json";

        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        List<NovelExportDto> dtos = new ArrayList<>();
        for (Novel novel : novels) {
            List<Chapter> chapters = chapterRepository
                    .findByNovelIdOrderByChapterNumberAsc(novel.getId());

            List<String> genreNames = novel.getGenres().stream()
                    .map(Genre::getName).collect(Collectors.toList());

            List<ChapterExportDto> chapterDtos = chapters.stream()
                    .map(ch -> new ChapterExportDto(
                            ch.getId(), ch.getChapterNumber(), ch.getTitle(),
                            ch.getContent(), ch.getParagraphs()))
                    .collect(Collectors.toList());

            dtos.add(new NovelExportDto(
                    novel.getId(), novel.getTitle(), novel.getAuthor(),
                    novel.getDescription(),
                    novel.getStatus() != null ? novel.getStatus().name() : null,
                    novel.getCoverImgUrl(), novel.getAverageRating(),
                    genreNames, chapterDtos));
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, dtos);

        log.info("Đã export " + dtos.size() + " truyện ra JSON: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    // =========================================================================
    //  EXPORT CSV (metadata only, không kèm content)
    // =========================================================================

    /**
     * Export danh sách truyện ra file CSV (metadata).
     * Mỗi dòng = 1 truyện: id, title, author, status, genres, totalChapters, coverImgUrl.
     */
    @Transactional(readOnly = true)
    public String exportAllToCsv(String label) throws IOException {
        List<Novel> novels = novelRepository.findAll();

        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String safeLabel = label.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String filePath = "output/export_" + safeLabel + "_" + timestamp + ".csv";

        File file = new File(filePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            // BOM for Excel UTF-8
            w.print('\uFEFF');
            w.println("id,title,author,status,genres,total_chapters,average_rating,cover_img_url");

            for (Novel novel : novels) {
                List<Chapter> chapters = chapterRepository
                        .findByNovelIdOrderByChapterNumberAsc(novel.getId());

                String genreStr = novel.getGenres().stream()
                        .map(Genre::getName).collect(Collectors.joining(";"));

                w.printf("%d,%s,%s,%s,%s,%d,%s,%s%n",
                        novel.getId(),
                        csvEscape(novel.getTitle()),
                        csvEscape(novel.getAuthor()),
                        novel.getStatus() != null ? novel.getStatus().name() : "",
                        csvEscape(genreStr),
                        chapters.size(),
                        novel.getAverageRating() != null ? novel.getAverageRating() : "",
                        csvEscape(novel.getCoverImgUrl()));
            }
        }

        log.info("Đã export " + novels.size() + " truyện ra CSV: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
