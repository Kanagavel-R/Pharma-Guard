package com.pharmaguard.controller;

import com.pharmaguard.entity.AuditLog;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.MedicineRecall;
import com.pharmaguard.repository.MedicineRepository;
import com.pharmaguard.service.AuditLogService;
import com.pharmaguard.service.RecallService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class ReportController {

    private final MedicineRepository medicineRepository;
    private final RecallService recallService;
    private final AuditLogService auditLogService;

    public ReportController(MedicineRepository medicineRepository, RecallService recallService, AuditLogService auditLogService) {
        this.medicineRepository = medicineRepository;
        this.recallService = recallService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/reports")
    public String viewReports(Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : "Admin";
        model.addAttribute("username", username);
        return "reports";
    }

    @GetMapping("/reports/export/inventory/csv")
    public void exportInventoryCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"pharmaguard_inventory_report.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("ID,Medicine Name,Category,Batch Number,Quantity,Price,Expiry Date,Supplier,Storage Location");

        List<Medicine> medicines = medicineRepository.findAll();
        for (Medicine m : medicines) {
            writer.println(String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%d\",\"%.2f\",\"%s\",\"%s\",\"%s\"",
                    m.getId(),
                    clean(m.getMedicineName()),
                    clean(m.getCategory()),
                    clean(m.getBatchNumber()),
                    m.getQuantity(),
                    m.getPrice(),
                    m.getExpiryDate() != null ? m.getExpiryDate() : "",
                    clean(m.getSupplierName()),
                    clean(m.getStorageLocation())
            ));
        }
    }

    @GetMapping("/reports/export/expiry/csv")
    public void exportExpiryCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"pharmaguard_expiry_report.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("ID,Medicine Name,Batch Number,Quantity,Expiry Date,Days Remaining,Status");

        LocalDate today = LocalDate.now();
        List<Medicine> medicines = medicineRepository.findAll();
        for (Medicine m : medicines) {
            if (m.getExpiryDate() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, m.getExpiryDate());
                String status = days < 0 ? "EXPIRED" : (days <= 30 ? "EXPIRING_SOON" : "SAFE");
                writer.println(String.format("\"%d\",\"%s\",\"%s\",\"%d\",\"%s\",\"%d\",\"%s\"",
                        m.getId(), clean(m.getMedicineName()), clean(m.getBatchNumber()), m.getQuantity(), m.getExpiryDate(), days, status
                ));
            }
        }
    }

    @GetMapping("/reports/export/audit/csv")
    public void exportAuditCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"pharmaguard_audit_logs.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("ID,Timestamp,User,Action,Resource Type,Resource ID,Details,Result");

        List<AuditLog> logs = auditLogService.getRecentLogs();
        for (AuditLog l : logs) {
            writer.println(String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    l.getId(), l.getTimestamp(), clean(l.getUsername()), clean(l.getAction()), clean(l.getResourceType()), clean(l.getResourceId()), clean(l.getDetails()), clean(l.getResult())
            ));
        }
    }

    @GetMapping("/reports/export/recalls/csv")
    public void exportRecallsCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"pharmaguard_recalls_report.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("ID,Medicine Name,Batch Number,Severity,Status,Affected Stock,Recall Reason");

        List<MedicineRecall> recalls = recallService.getAllRecalls();
        for (MedicineRecall r : recalls) {
            writer.println(String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%s\"",
                    r.getId(), clean(r.getMedicineName()), clean(r.getBatchNumber()), clean(r.getSeverity()), clean(r.getStatus()), r.getAffectedStock(), clean(r.getRecallReason())
            ));
        }
    }

    private String clean(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}
