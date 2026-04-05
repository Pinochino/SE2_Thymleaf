package com.example.SE2.controllers;

import com.example.SE2.constants.FontFamily;
import com.example.SE2.constants.FontSize;
import com.example.SE2.constants.LineSpacing;
import com.example.SE2.constants.Theme;
import com.example.SE2.models.ParagraphComment;
import com.example.SE2.models.ReadingSetting;
import com.example.SE2.models.User;
import com.example.SE2.repositories.ParagraphCommentRepository;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.services.chapter.ChapterService;
import com.example.SE2.services.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.SE2.utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chapter")
public class ChapterApiController {

    private final ChapterService chapterService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ParagraphCommentRepository paragraphCommentRepository;

    public ChapterApiController(ChapterService chapterService, UserRepository userRepository, NotificationService notificationService, ParagraphCommentRepository paragraphCommentRepository) {
        this.chapterService = chapterService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.paragraphCommentRepository = paragraphCommentRepository;
    }

    @GetMapping("/{chapterId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long chapterId,
                                         @RequestParam int paragraphIndex) {
        List<ParagraphComment> comments = chapterService.getCommentsByParagraph(chapterId, paragraphIndex);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ParagraphComment c : comments) {
            if (c.getUser() == null) continue;
            Map<String, Object> item = buildCommentMap(c);
            List<Map<String, Object>> replies = new ArrayList<>();
            for (ParagraphComment r : c.getReplies()) {
                if (r.getUser() == null) continue;
                replies.add(buildCommentMap(r));
            }
            item.put("replies", replies);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{chapterId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long chapterId,
                                        @RequestBody Map<String, Object> body) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required"));
        }

        int paragraphIndex = ((Number) body.get("paragraphIndex")).intValue();
        String content = (String) body.get("content");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comment cannot be empty"));
        }

        Long parentCommentId = body.containsKey("parentCommentId") && body.get("parentCommentId") != null
                ? ((Number) body.get("parentCommentId")).longValue() : null;

        ParagraphComment comment = chapterService.addComment(user, chapterId, paragraphIndex, content.trim(), parentCommentId);

        // Notify parent comment author if this is a reply
        if (parentCommentId != null) {
            ParagraphComment parent = paragraphCommentRepository.findById(parentCommentId).orElse(null);
            if (parent != null) {
                notificationService.notifyParagraphCommentReply(comment, parent);
            }
        }

        // Notify users who bookmarked this paragraph
        notificationService.notifyBookmarkHolders(comment);

        Map<String, Object> response = buildCommentMap(comment);
        response.put("paragraphIndex", comment.getParagraphIndex());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{chapterId}/bookmarks")
    public ResponseEntity<?> toggleBookmark(@PathVariable Long chapterId,
                                            @RequestBody Map<String, Object> body) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required"));
        }

        int paragraphIndex = ((Number) body.get("paragraphIndex")).intValue();
        boolean added = chapterService.toggleBookmark(user, chapterId, paragraphIndex);

        return ResponseEntity.ok(Map.of("bookmarked", added));
    }

//    @PostMapping("/{chapterId}/progress")
//    public ResponseEntity<?> saveProgress(@PathVariable Long chapterId,
//                                          @RequestBody Map<String, Object> body) {
//        User user = getCurrentUser();
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required"));
//        }
//
//        Long position = ((Number) body.get("position")).longValue();
//        chapterService.saveReadingProgress(user, chapterId, position);
//
//        return ResponseEntity.ok(Map.of("saved", true));
//    }

    @PostMapping("/{chapterId}/progress")
    public ResponseEntity<?> saveProgress(@PathVariable Long chapterId,
                                          @RequestBody Map<String, Object> body) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required"));
        }

        // Hỗ trợ cả "position" (scroll%) lẫn "paragraphIndex"
        Long position;
        if (body.containsKey("paragraphIndex")) {
            position = ((Number) body.get("paragraphIndex")).longValue();
        } else {
            position = ((Number) body.get("position")).longValue();
        }

        chapterService.saveReadingProgress(user, chapterId, position);
        return ResponseEntity.ok(Map.of("saved", true));
    }

    @PostMapping("/settings")
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required"));
        }

        Theme theme = body.containsKey("theme") ? Theme.valueOf(body.get("theme").toUpperCase()) : null;
        FontFamily fontFamily = body.containsKey("fontFamily") ? FontFamily.valueOf(body.get("fontFamily").toUpperCase()) : null;
        FontSize fontSize = body.containsKey("fontSize") ? FontSize.valueOf(body.get("fontSize").toUpperCase()) : null;
        LineSpacing lineSpacing = body.containsKey("lineSpacing") ? LineSpacing.valueOf(body.get("lineSpacing").toUpperCase()) : null;

        ReadingSetting saved = chapterService.saveReadingSetting(user, theme, fontSize, fontFamily, lineSpacing);

        return ResponseEntity.ok(Map.of("saved", true));
    }

    private Map<String, Object> buildCommentMap(ParagraphComment c) {
        User u = c.getUser();
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        String fullName = buildFullName(u);
        map.put("userName", fullName);
        map.put("userInitial", fullName.substring(0, 1));
        map.put("avatarUrl", u.getAvatarUrl());
        map.put("content", c.getContent());
        map.put("timeAgo", c.getCreatedAt() != null ? TimeUtils.timeAgo(c.getCreatedAt()) : "");
        return map;
    }

    private String buildFullName(User u) {
        String first = u.getFirstName() != null ? u.getFirstName() : "";
        String last = u.getLastName() != null ? u.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getUsername() : full;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String email = auth.getName();
        return userRepository.findUserByEmail(email);
    }
}
