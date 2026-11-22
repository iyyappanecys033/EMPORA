package com.examly.springapp.controller;

import com.examly.springapp.dto.NotificationDTO;
import com.examly.springapp.dto.UnreadCountDTO;
import com.examly.springapp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public ResponseEntity<Page<NotificationDTO>> getMyNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(name = "eventType", required = false) String eventType,
            Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(notificationService.getMyNotifications(username, unreadOnly, eventType, page, size));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<UnreadCountDTO> getMyUnreadCount(Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(notificationService.getUnreadCount(username));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable("id") Long id,
            Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(notificationService.markAsRead(id, username));
    }

    @PutMapping("/my/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(notificationService.markAllAsRead(username));
    }
}

