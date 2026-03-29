package com.example.SE2.services.chapter;

import com.example.SE2.constants.FontFamily;
import com.example.SE2.constants.FontSize;
import com.example.SE2.constants.LineSpacing;
import com.example.SE2.constants.Theme;
import com.example.SE2.models.*;
import com.example.SE2.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final ParagraphCommentRepository paragraphCommentRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ReadingSettingRepository readingSettingRepository;
    private final ReadingProgressRepository readingProgressRepository;

    public ChapterServiceImpl(ChapterRepository chapterRepository,
                              ParagraphCommentRepository paragraphCommentRepository,
                              BookmarkRepository bookmarkRepository,
                              ReadingSettingRepository readingSettingRepository,
                              ReadingProgressRepository readingProgressRepository) {
        this.chapterRepository = chapterRepository;
        this.paragraphCommentRepository = paragraphCommentRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.readingSettingRepository = readingSettingRepository;
        this.readingProgressRepository = readingProgressRepository;
    }

    @Override
    public Chapter getChapterById(Long chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NoSuchElementException("Chapter not found with id: " + chapterId));
    }

    @Override
    public List<Chapter> getChaptersByNovelId(Long novelId) {
        return chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
    }

    @Override
    public Optional<Chapter> getNextChapter(Chapter chapter) {
        return chapterRepository.findFirstByNovelIdAndChapterNumberGreaterThanOrderByChapterNumberAsc(chapter.getNovel().getId(), chapter.getChapterNumber());
    }

    @Override
    public Optional<Chapter> getPreviousChapter(Chapter chapter) {
        return chapterRepository.findFirstByNovelIdAndChapterNumberLessThanOrderByChapterNumberDesc(chapter.getNovel().getId(), chapter.getChapterNumber());
    }

    @Override
    public List<String> splitContentIntoParagraphs(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return Arrays.stream(content.split("\\n\\n+|\\r\\n\\r\\n+"))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, Long> getCommentCountsByChapter(Long chapterId, int paragraphCount) {
        Map<Integer, Long> counts = new HashMap<>();
        for (int i = 0; i < paragraphCount; i++) {
            Long count = paragraphCommentRepository.countByChapterIdAndParagraphIndex(chapterId, i);
            if (count > 0) {
                counts.put(i, count);
            }
        }
        return counts;
    }

    @Override
    public List<ParagraphComment> getCommentsByChapter(Long chapterId) {
        return paragraphCommentRepository.findByChapterIdAndParentCommentIsNullOrderByCreatedAtDesc(chapterId);
    }

    @Override
    public List<ParagraphComment> getCommentsByParagraph(Long chapterId, int paragraphIndex) {
        return paragraphCommentRepository.findByChapterIdAndParagraphIndexAndParentCommentIsNullOrderByCreatedAtDesc(chapterId, paragraphIndex);
    }

    @Override
    public List<Bookmark> getUserBookmarksForChapter(String userId, Long chapterId) {
        if (userId == null) return List.of();
        return bookmarkRepository.findByUserIdAndChapterId(userId, chapterId);
    }

    @Override
    public ReadingSetting getUserReadingSetting(String userId) {
        if (userId == null) return null;
        return readingSettingRepository.findByUserId(userId).orElse(null);
    }

    @Override
    public List<Long> getReadChapterIds(String userId, Long novelId) {
        if (userId == null) return List.of();
        return readingProgressRepository.findByUserIdAndChapterNovelId(userId, novelId)
                .stream()
                .map(rp -> rp.getChapter().getId())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParagraphComment addComment(User user, Long chapterId, int paragraphIndex, String content, Long parentCommentId) {
        Chapter chapter = getChapterById(chapterId);
        ParagraphComment comment = new ParagraphComment();
        comment.setUser(user);
        comment.setChapter(chapter);
        comment.setParagraphIndex(paragraphIndex);
        comment.setContent(content);
        if (parentCommentId != null) {
            ParagraphComment parent = paragraphCommentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new NoSuchElementException("Parent comment not found"));
            comment.setParentComment(parent);
        }
        return paragraphCommentRepository.save(comment);
    }

    @Override
    @Transactional
    public boolean toggleBookmark(User user, Long chapterId, int paragraphIndex) {
        List<Bookmark> existing = bookmarkRepository.findByUserIdAndChapterId(user.getId(), chapterId);
        Optional<Bookmark> match = existing.stream()
                .filter(b -> b.getParagraphIndex() == paragraphIndex)
                .findFirst();
        if (match.isPresent()) {
            bookmarkRepository.delete(match.get());
            return false; // removed
        } else {
            Chapter chapter = getChapterById(chapterId);
            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setChapter(chapter);
            bookmark.setParagraphIndex(paragraphIndex);
            bookmarkRepository.save(bookmark);
            return true; // added
        }
    }

    @Override
    @Transactional
    public void saveReadingProgress(User user, Long chapterId, Long position) {
        Optional<ReadingProgress> existing = readingProgressRepository.findByUserIdAndChapterId(user.getId(), chapterId);
        ReadingProgress rp;
        if (existing.isPresent()) {
            rp = existing.get();
        } else {
            rp = new ReadingProgress();
            rp.setUser(user);
            rp.setChapter(getChapterById(chapterId));
        }
        rp.setLastPosition(position);
        readingProgressRepository.save(rp);
    }

    @Override
    @Transactional
    public ReadingSetting saveReadingSetting(User user, Theme theme, FontSize fontSize, FontFamily fontFamily, LineSpacing lineSpacing) {
        Optional<ReadingSetting> existing = readingSettingRepository.findByUserId(user.getId());
        ReadingSetting rs;
        if (existing.isPresent()) {
            rs = existing.get();
        } else {
            rs = new ReadingSetting();
            rs.setUser(user);
        }
        if (theme != null) rs.setTheme(theme);
        if (fontSize != null) rs.setFontSize(fontSize);
        if (fontFamily != null) rs.setFontFamily(fontFamily);
        if (lineSpacing != null) rs.setLineSpacing(lineSpacing);
        return readingSettingRepository.save(rs);
    }
}
