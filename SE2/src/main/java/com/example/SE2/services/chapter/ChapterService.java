package com.example.SE2.services.chapter;

import com.example.SE2.constants.FontFamily;
import com.example.SE2.constants.FontSize;
import com.example.SE2.constants.LineSpacing;
import com.example.SE2.constants.Theme;
import com.example.SE2.models.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChapterService {

    Chapter getChapterById(Long chapterId);

    List<Chapter> getChaptersByNovelId(Long novelId);

    Optional<Chapter> getNextChapter(Chapter chapter);

    Optional<Chapter> getPreviousChapter(Chapter chapter);

    List<String> splitContentIntoParagraphs(String content);

    Map<Integer, Long> getCommentCountsByChapter(Long chapterId, int paragraphCount);

    List<ParagraphComment> getCommentsByChapter(Long chapterId);

    List<ParagraphComment> getCommentsByParagraph(Long chapterId, int paragraphIndex);

    List<Bookmark> getUserBookmarksForChapter(String userId, Long chapterId);

    ReadingSetting getUserReadingSetting(String userId);

    List<Long> getReadChapterIds(String userId, Long novelId);

    // === Write operations ===

    ParagraphComment addComment(User user, Long chapterId, int paragraphIndex, String content, Long parentCommentId);

    boolean toggleBookmark(User user, Long chapterId, int paragraphIndex);

    void saveReadingProgress(User user, Long chapterId, Long position);

    ReadingSetting saveReadingSetting(User user, Theme theme, FontSize fontSize, FontFamily fontFamily, LineSpacing lineSpacing);
}
