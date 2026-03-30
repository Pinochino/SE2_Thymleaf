package com.example.SE2.service;

import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Chapter;
import com.example.SE2.models.Genre;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.ChapterRepository;
import com.example.SE2.repositories.GenreRepository;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.service.NovelScraperService.*;
import com.example.SE2.service.NovelUpdateService.NovelWithContent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.logging.Logger;

/**
 * NovelPersistenceService - Lưu dữ liệu crawl vào database.
 *
 * Xử lý dedup theo title (Novel) và chapterNumber (Chapter).
 * Map genres từ scraper sang bảng Genre + novel_genre.
 */
@Service
public class NovelPersistenceService {

    private static final Logger log = Logger.getLogger(NovelPersistenceService.class.getName());

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final GenreRepository genreRepository;

    public NovelPersistenceService(NovelRepository novelRepository,
                                   ChapterRepository chapterRepository,
                                   GenreRepository genreRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.genreRepository = genreRepository;
    }

    // =========================================================================
    //  SAVE NOVEL INFO (không kèm content chapter)
    // =========================================================================

    /**
     * Lưu thông tin truyện từ NovelInfo vào DB.
     * Nếu truyện đã tồn tại (theo title) → cập nhật thông tin.
     * Nếu chưa → tạo mới.
     *
     * @return Novel entity đã lưu
     */
    @Transactional
    public Novel saveNovelInfo(NovelInfo info) {
        Novel novel = novelRepository.findByTitle(info.title());

        if (novel == null) {
            novel = new Novel();
            novel.setTitle(info.title());
            log.info("[Persist] Tạo mới truyện: " + info.title());
        } else {
            log.info("[Persist] Cập nhật truyện: " + info.title());
        }

        novel.setAuthor(info.author());
        novel.setDescription(info.description());
        novel.setCoverImgUrl(info.coverImgUrl());
        novel.setStatus(mapStatus(info.status()));

        novel = novelRepository.save(novel);

        // Genres
        if (info.genres() != null && !info.genres().isEmpty()) {
            saveGenres(novel, info.genres());
        }

        return novel;
    }

    // =========================================================================
    //  SAVE CHAPTERS
    // =========================================================================

    /**
     * Lưu danh sách chapter content vào DB cho 1 truyện.
     * Dedup theo novel_id + chapter_number: nếu đã tồn tại → cập nhật content.
     *
     * @return số chapter đã lưu/cập nhật
     */
    @Transactional
    public int saveChapters(Novel novel, List<ChapterContent> contents, List<ChapterLink> links) {
        int saved = 0;

        // Build map link index → ChapterLink cho tra cứu nhanh
        Map<String, ChapterLink> linkMap = new HashMap<>();
        for (ChapterLink link : links) {
            linkMap.put(link.url(), link);
        }

        for (ChapterContent content : contents) {
            // Tìm ChapterLink tương ứng để lấy index
            ChapterLink link = linkMap.get(content.url());
            long chapterNumber = link != null ? link.index() : (saved + 1);

            Chapter chapter = chapterRepository.findByNovelIdAndChapterNumber(
                    novel.getId(), chapterNumber);

            if (chapter == null) {
                chapter = new Chapter();
                chapter.setNovel(novel);
                chapter.setChapterNumber(chapterNumber);
            }

            chapter.setTitle(content.title());
            chapter.setContent(content.content());
            chapter.setParagraphs(countParagraphs(content.content()));

            chapterRepository.save(chapter);
            saved++;
        }

        log.info("[Persist] Đã lưu " + saved + " chapter cho \"" + novel.getTitle() + "\"");
        return saved;
    }

    /**
     * Lưu chapter links (chỉ metadata, không có content) vào DB.
     * Dùng cho trường hợp chỉ cần lưu danh sách chương mà không cào nội dung.
     */
    @Transactional
    public int saveChapterLinks(Novel novel, List<ChapterLink> links) {
        int saved = 0;
        for (ChapterLink link : links) {
            if (!chapterRepository.existsByNovelIdAndChapterNumber(novel.getId(), (long) link.index())) {
                Chapter chapter = new Chapter();
                chapter.setNovel(novel);
                chapter.setChapterNumber((long) link.index());
                chapter.setTitle(link.title());
                chapterRepository.save(chapter);
                saved++;
            }
        }
        log.info("[Persist] Đã lưu " + saved + " chapter link cho \"" + novel.getTitle() + "\"");
        return saved;
    }

    // =========================================================================
    //  SAVE FULL (NovelWithContent từ NovelUpdateService)
    // =========================================================================

    /** Kết quả lưu 1 truyện */
    public record PersistResult(Novel novel, int chaptersSaved, boolean isNew) {}

    /**
     * Lưu đầy đủ: thông tin truyện + chapter content.
     * Đây là method chính để gọi sau khi crawl xong.
     */
    @Transactional
    public PersistResult saveNovelWithContent(NovelWithContent nwc) {
        NovelInfo info = nwc.info();
        if (info == null) {
            log.warning("[Persist] Bỏ qua truyện không có info: " + nwc.update().title());
            return null;
        }

        boolean isNew = !novelRepository.existsByTitle(info.title());
        Novel novel = saveNovelInfo(info);
        int chaptersSaved = saveChapters(novel, nwc.chapters(), info.chapterList());

        return new PersistResult(novel, chaptersSaved, isNew);
    }

    /**
     * Lưu batch nhiều truyện cùng lúc.
     *
     * @return danh sách kết quả lưu
     */
    @Transactional
    public List<PersistResult> saveAll(List<NovelWithContent> novels) {
        List<PersistResult> results = new ArrayList<>();
        for (NovelWithContent nwc : novels) {
            try {
                PersistResult result = saveNovelWithContent(nwc);
                if (result != null) results.add(result);
            } catch (Exception e) {
                log.warning("[Persist] Lỗi lưu \"" + nwc.update().title() + "\": " + e.getMessage());
            }
        }
        log.info("[Persist] Hoàn tất: " + results.size() + "/" + novels.size() + " truyện");
        return results;
    }

    // =========================================================================
    //  SAVE NOVEL + TẤT CẢ CHAPTER (từ NovelScraperService)
    // =========================================================================

    /**
     * Lưu truyện từ NovelInfo + toàn bộ chapter content đã cào.
     * Dùng khi gọi trực tiếp NovelScraperService.getAllChapters().
     */
    @Transactional
    public PersistResult saveNovelWithAllChapters(NovelInfo info, List<ChapterContent> chapters) {
        boolean isNew = !novelRepository.existsByTitle(info.title());
        Novel novel = saveNovelInfo(info);
        int saved = saveChapters(novel, chapters, info.chapterList());
        return new PersistResult(novel, saved, isNew);
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private void saveGenres(Novel novel, List<String> genreNames) {
        for (String name : genreNames) {
            Genre genre = genreRepository.findGenreByName(name.trim());
            if (genre == null) {
                genre = new Genre(name.trim());
                genre = genreRepository.save(genre);
                log.info("[Persist] Tạo genre mới: " + name);
            }
            // Thêm novel vào genre (owner side của ManyToMany)
            genre.getNovels().add(novel);
            genreRepository.save(genre);
        }
    }

    private NovelStatus mapStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) return NovelStatus.ONGOING;

        String lower = rawStatus.toLowerCase();
        if (lower.contains("hoàn") || lower.contains("complete") || lower.contains("完")
                || lower.contains("full") || lower.contains("finished")) {
            return NovelStatus.COMPLETED;
        }
        if (lower.contains("drop") || lower.contains("ngừng") || lower.contains("tạm")) {
            return NovelStatus.DROPPED;
        }
        return NovelStatus.ONGOING;
    }

    private int countParagraphs(String content) {
        if (content == null || content.isBlank()) return 0;
        String[] paragraphs = content.split("\\n\\n|\\r\\n\\r\\n");
        return Math.max(1, paragraphs.length);
    }
}
