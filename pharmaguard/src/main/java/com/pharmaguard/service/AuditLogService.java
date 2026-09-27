package com.pharmaguard.service;

import com.pharmaguard.entity.AuditLog;
import com.pharmaguard.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog logAction(String username, String action, String resourceType, String resourceId, String details, String ipAddress, String result) {
        try {
            AuditLog log = new AuditLog(
                    username != null ? username : "System",
                    username != null ? username : "System",
                    action != null ? action : "ACTION",
                    resourceType,
                    resourceId,
                    details,
                    ipAddress != null ? ipAddress : "127.0.0.1",
                    result != null ? result : "SUCCESS",
                    UUID.randomUUID().toString().substring(0, 8)
            );
            return auditLogRepository.save(log);
        } catch (Exception e) {
            return null;
        }
    }

    public AuditLog logAction(String username, String action, String resourceType, String resourceId, String details) {
        return logAction(username, action, resourceType, resourceId, details, "127.0.0.1", "SUCCESS");
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public Page<AuditLog> getLogsPaginated(int page, int size) {
        return auditLogRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
    }
}
