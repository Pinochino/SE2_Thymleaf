package com.example.SE2.controllers;

import com.example.SE2.repositories.NovelCommentRepository;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.repositories.ReadingProgressRepository;
import com.example.SE2.repositories.RatingRepository;
import com.example.SE2.security.UserDetailImpl;
import com.example.SE2.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.SE2.models.*;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.services.chapter.ChapterService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/novels")
public class NovelController {

    private NovelRepository novelRepository;
    private NovelCommentRepository novelCommentRepository;
    private final ChapterService chapterService;
    private final UserRepository userRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final RatingRepository ratingRepository;
    private final Logger logger = LoggerFactory.getLogger(NovelController.class);

    @Autowired
    public NovelController(NovelRepository novelRepository,
            NovelCommentRepository novelCommentRepository,
            ChapterService chapterService,
            UserRepository userRepository,
            ReadingProgressRepository readingProgressRepository,
            RatingRepository ratingRepository) {
        this.novelRepository = novelRepository;
        this.novelCommentRepository = novelCommentRepository;
        this.chapterService = chapterService;
        this.userRepository = userRepository;
        this.readingProgressRepository = readingProgressRepository;
        this.ratingRepository = ratingRepository;
    }

//     @RequestMapping(value = "/information/{publicId}", method =
//     RequestMethod.GET)
//     public String novelDetail(@PathVariable String publicId,
//     Model model,
//     @AuthenticationPrincipal UserDetailImpl user) {
//
//     Novel novel = novelRepository.findNovelByPublicId(UUID.fromString(publicId));
//
//     // Chỉ lấy comment gốc (không có parent)
//     List<NovelComment> novelComments = novelCommentRepository
//     .findByNovelAndParentCommentIsNullOrderByCreatedAtDesc(novel);
//
//     // Load chapters for the novel
//     List<Chapter> chapters = chapterService.getChaptersByNovelId(novel.getId());
//
//     model.addAttribute("novel", novel);
//     model.addAttribute("novelComment", new NovelComment());
//     model.addAttribute("userLogin", user);
//     model.addAttribute("novelComments", novelComments);
//     model.addAttribute("timeUtils", new TimeUtils());
//     model.addAttribute("chapters", chapters);
//     model.addAttribute("totalChapters", chapters.size());
//     model.addAttribute("hasChapters", !chapters.isEmpty());
//
//     return "client/novel-detail";
//     }

     @GetMapping("/chapter/{id}")
     public String readChapter(@PathVariable Long id, Model model) {
     // Load the chapter
     Chapter chapter = chapterService.getChapterById(id);
     Novel novel = chapter.getNovel();

     // Split content into paragraphs
     List<String> paragraphs =
     chapterService.splitContentIntoParagraphs(chapter.getContent());

     // Load all chapters for TOC sidebar
     List<Chapter> allChapters =
     chapterService.getChaptersByNovelId(novel.getId());

     // Load next/previous chapter for navigation
     Chapter nextChapter = chapterService.getNextChapter(chapter).orElse(null);
     Chapter prevChapter =
     chapterService.getPreviousChapter(chapter).orElse(null);

     // Comment counts per paragraph
     Map<Integer, Long> commentCounts =
     chapterService.getCommentCountsByChapter(id, paragraphs.size());

     // Word count
     int wordCount = paragraphs.stream()
     .mapToInt(p -> p.split("\\s+").length)
     .sum();

     // Model attributes
     model.addAttribute("chapter", chapter);
     model.addAttribute("novel", novel);
     model.addAttribute("paragraphs", paragraphs);
     model.addAttribute("allChapters", allChapters);
     model.addAttribute("nextChapter", nextChapter);
     model.addAttribute("prevChapter", prevChapter);
     model.addAttribute("commentCounts", commentCounts);
     model.addAttribute("wordCount", wordCount);

     // User-specific data (if logged in)
     User currentUser = getCurrentUser();
     if (currentUser != null) {
     String userId = currentUser.getId();

     List<Bookmark> bookmarks = chapterService.getUserBookmarksForChapter(userId,
     id);
     model.addAttribute("bookmarks", bookmarks);

     Set<Integer> bookmarkedParagraphs = bookmarks.stream()
     .map(Bookmark::getParagraphIndex)
     .collect(Collectors.toSet());
     model.addAttribute("bookmarkedParagraphs", bookmarkedParagraphs);

     ReadingSetting settings = chapterService.getUserReadingSetting(userId);
     model.addAttribute("readingSettings", settings);

     List<Long> readChapterIds = chapterService.getReadChapterIds(userId,
     novel.getId());
     model.addAttribute("readChapterIds", readChapterIds);

     model.addAttribute("isLoggedIn", true);
     } else {
     model.addAttribute("bookmarks", List.of());
     model.addAttribute("bookmarkedParagraphs", Set.of());
     model.addAttribute("readingSettings", null);
     model.addAttribute("readChapterIds", List.of());
     model.addAttribute("isLoggedIn", false);
     }

     return "client/chapter";
     }

     private User getCurrentUser() {
     Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     if (auth == null || !auth.isAuthenticated() ||
     "anonymousUser".equals(auth.getPrincipal())) {
     return null;
     }
     String email = auth.getName();
     return userRepository.findUserByEmail(email);
     }

     public void saveProgress(User user, Chapter chapter, Long position) {

     Optional<ReadingProgress> optional =
     readingProgressRepository.findByUserIdAndChapterId(
     user.getId(),
     chapter.getId());

     ReadingProgress progress;

     if (optional.isPresent()) {
     progress = optional.get();
     progress.setLastPosition(position);
     } else {
     progress = new ReadingProgress();
     progress.setUser(user);
     progress.setChapter(chapter);
     progress.setLastPosition(position);
     }

     readingProgressRepository.save(progress);
     }

 @RequestMapping(value = "/information/{publicId}", method = RequestMethod.GET)
 public String novelDetail(@PathVariable String publicId,
                           Model model,
                           @AuthenticationPrincipal UserDetailImpl userDetail) {

  Novel novel = novelRepository.findNovelByPublicId(UUID.fromString(publicId));
  List<NovelComment> novelComments = novelCommentRepository
          .findByNovelAndParentCommentIsNullOrderByCreatedAtDesc(novel);
  List<Chapter> chapters = chapterService.getChaptersByNovelId(novel.getId());
  int totalChapters = chapters.size();

  long ratingCount = ratingRepository.countByNovelId(novel.getId());

  model.addAttribute("novel", novel);
  model.addAttribute("novelComment", new NovelComment());
  model.addAttribute("userLogin", userDetail);
  model.addAttribute("novelComments", novelComments);
  model.addAttribute("timeUtils", new TimeUtils());
  model.addAttribute("chapters", chapters);
  model.addAttribute("totalChapters", totalChapters);
  model.addAttribute("hasChapters", !chapters.isEmpty());
  model.addAttribute("ratingCount", ratingCount);

  // Tính reading progress
  int currentChapter = 0;
  int progressPct = 0;

  if (userDetail != null) {
   User user = userRepository.findUserByEmail(userDetail.getUsername());
   if (user != null) {
    // Lấy progress mới nhất của user cho novel này
    readingProgressRepository
            .findTopByUserAndChapter_NovelOrderByUpdatedAtDesc(user, novel)
            .ifPresent(progress -> {
             // không dùng được vì effectively final — xử lý bên dưới
            });

    // Dùng cách này thay thế
    Optional<ReadingProgress> latestProgress = readingProgressRepository
            .findTopByUserAndChapter_NovelOrderByUpdatedAtDesc(user, novel);

    if (latestProgress.isPresent()) {
     long chapterNumber = latestProgress.get().getChapter().getChapterNumber();
     model.addAttribute("currentChapter", chapterNumber);
     model.addAttribute("progressPct", totalChapters > 0
             ? (int) Math.round((chapterNumber * 100.0) / totalChapters)
             : 0);
    } else {
     model.addAttribute("currentChapter", 0);
     model.addAttribute("progressPct", 0);
    }
   }
  } else {
   model.addAttribute("currentChapter", 0);
   model.addAttribute("progressPct", 0);
  }

  return "client/novel-detail";
 }


}
