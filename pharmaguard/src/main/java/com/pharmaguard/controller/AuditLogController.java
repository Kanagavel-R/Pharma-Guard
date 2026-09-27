package com.pharmaguard.controller;

import com.pharmaguard.entity.AuditLog;
import com.pharmaguard.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping({"/security/audit-logs", "/audit-logs"})
    public String viewAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            Principal principal,
            Model model) {

        String username = (principal != null) ? principal.getName() : "Admin";
        model.addAttribute("username", username);

        Page<AuditLog> auditLogPage = auditLogService.getLogsPaginated(page, size);
        model.addAttribute("auditLogs", auditLogPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auditLogPage.getTotalPages());
        model.addAttribute("totalElements", auditLogPage.getTotalElements());

        return "audit-logs";
    }
}
