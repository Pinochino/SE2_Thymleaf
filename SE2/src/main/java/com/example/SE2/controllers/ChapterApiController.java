package com.example.SE2.controllers;

import com.example.SE2.constants.FontFamily;
import com.example.SE2.constants.FontSize;
import com.example.SE2.constants.LineSpacing;
import com.example.SE2.constants.Theme;
import com.example.SE2.models.ParagraphComment;
import com.example.SE2.models.ReadingSetting;
import com.example.SE2.models.User;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.services.chapter.ChapterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chapter")
public class ChapterApiController {

    private final ChapterService chapterService;
    private final UserRepository userRepository;

    public ChapterApiController(ChapterService chapterService, UserRepository userRepository) {
        this.chapterService = chapterService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{chapterId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long chapterId,
                                         @RequestParam int paragraphIndex) {
        List<ParagraphComment> comments = chapterService.getCommentsByParagraph(chapterId, paragraphIndex);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ParagraphComment c : comments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("userName", c.getUser().getFirstName() != null ? c.getUser().getFirstName() : c.getUser().getUsername());
            item.put("userInitial", c.getUser().getFirstName() != null ? c.getUser().getFirstName().substring(0, 1) : "U");
            item.put("content", c.getContent());
            // Replies
            List<Map<String, Object>> replies = new ArrayList<>();
            for (ParagraphComment r : c.getReplies()) {
                Map<String, Object> reply = new HashMap<>();
                reply.put("id", r.getId());
                reply.put("userName", r.getUser().getFirstName() != null ? r.getUser().getFirstName() : r.getUser().getUsername());
                reply.put("userInitial", r.getUser().getFirstName() != null ? r.getUser().getFirstName().substring(0, 1) : "U");
                reply.put("content", r.getContent());
                replies.add(reply);
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

        return ResponseEntity.ok(Map.of(
                "id", comment.getId(),
                "userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                "userInitial", user.getFirstName() != null ? user.getFirstName().substring(0, 1) : "U",
                "content", comment.getContent(),
                "paragraphIndex", comment.getParagraphIndex()
        ));
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

    @PostMapping("/{chapterId}/progress")
    public ResponseEntity<?> saveProgress(@PathVariable Long chapterId,
                                          @RequestBody Map<String, Object> body) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required"));
        }

        Long position = ((Number) body.get("position")).longValue();
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

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String email = auth.getName();
        return userRepository.findUserByEmail(email);
    }
}
