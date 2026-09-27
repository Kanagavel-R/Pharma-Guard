package com.pharmaguard.controller;

import com.pharmaguard.entity.ExpiryNotification;
import com.pharmaguard.service.ExpiryAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final ExpiryAlertService expiryAlertService;

    public NotificationApiController(ExpiryAlertService expiryAlertService) {
        this.expiryAlertService = expiryAlertService;
    }

    @GetMapping
    public ResponseEntity<List<ExpiryNotification>> getAllNotifications() {
        return ResponseEntity.ok(expiryAlertService.getAllNotifications());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Map<String, Object> res = new HashMap<>();
        res.put("unreadCount", expiryAlertService.getUnreadCount());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/critical-popup")
    public ResponseEntity<List<ExpiryNotification>> getCriticalPopups() {
        return ResponseEntity.ok(expiryAlertService.getCriticalPopups());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("id") Long id) {
        expiryAlertService.markAsRead(id);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        expiryAlertService.markAllAsRead();
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable("id") Long id) {
        expiryAlertService.deleteNotification(id);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }
}
