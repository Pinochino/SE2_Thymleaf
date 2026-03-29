package com.example.SE2.controllers;

import com.example.SE2.models.Notification;
import com.example.SE2.models.User;
import com.example.SE2.repositories.NotificationRepository;
import com.example.SE2.security.UserDetailImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal UserDetailImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }
        String userId = userDetails.getUser().getId();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long unreadCount = notificationRepository.countByUserIdAndIsReaded(userId, false);

        List<Map<String, Object>> items = notifications.stream().map(n -> Map.<String, Object>of(
                "id", n.getId(),
                "content", n.getContent(),
                "isRead", n.isReaded(),
                "createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : ""
        )).toList();

        return ResponseEntity.ok(Map.of(
                "notifications", items,
                "unreadCount", unreadCount
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetailImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        if (!notification.getUser().getId().equals(userDetails.getUser().getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your notification"));
        }
        notification.setReaded(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal UserDetailImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }
        String userId = userDetails.getUser().getId();
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        unread.stream().filter(n -> !n.isReaded()).forEach(n -> {
            n.setReaded(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok(Map.of("success", true));
    }
}
