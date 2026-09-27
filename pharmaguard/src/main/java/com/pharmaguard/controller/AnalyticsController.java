package com.pharmaguard.controller;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.MedicineRepository;
import com.pharmaguard.repository.VerificationHistoryRepository;
import com.pharmaguard.service.MedicineService;
import com.pharmaguard.service.RecallService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AnalyticsController {

    private final MedicineRepository medicineRepository;
    private final MedicineService medicineService;
    private final VerificationHistoryRepository verificationRepository;
    private final RecallService recallService;

    public AnalyticsController(
            MedicineRepository medicineRepository,
            MedicineService medicineService,
            VerificationHistoryRepository verificationRepository,
            RecallService recallService) {
        this.medicineRepository = medicineRepository;
        this.medicineService = medicineService;
        this.verificationRepository = verificationRepository;
        this.recallService = recallService;
    }

    @GetMapping("/analytics")
    public String viewAnalytics(Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : "Admin";
        model.addAttribute("username", username);

        List<Medicine> all = medicineRepository.findAll();
        double totalVal = all.stream().mapToDouble(m -> m.getPrice() * m.getQuantity()).sum();
        long totalMeds = all.size();
        long totalStock = all.stream().mapToLong(Medicine::getQuantity).sum();
        long lowStock = medicineRepository.countLowStockMedicines();
        long expired = medicineRepository.countExpiredMedicines(LocalDate.now());
        long expiringSoon = medicineRepository.countExpiringSoonMedicines(LocalDate.now(), LocalDate.now().plusDays(30));
        long activeRecalls = recallService.getActiveRecallCount();
        long totalVerifications = verificationRepository.count();

        model.addAttribute("totalMedicines", totalMeds);
        model.addAttribute("totalStock", totalStock);
        model.addAttribute("totalValue", String.format("%.2f", totalVal));
        model.addAttribute("lowStockCount", lowStock);
        model.addAttribute("expiredCount", expired);
        model.addAttribute("expiringSoonCount", expiringSoon);
        model.addAttribute("activeRecalls", activeRecalls);
        model.addAttribute("totalVerifications", totalVerifications);

        return "analytics";
    }

    @GetMapping("/api/analytics/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnalyticsData() {
        Map<String, Object> data = new HashMap<>();
        List<Medicine> all = medicineRepository.findAll();

        Map<String, Integer> categoryDistribution = new HashMap<>();
        for (Medicine m : all) {
            String cat = m.getCategory() != null ? m.getCategory() : "Uncategorized";
            categoryDistribution.put(cat, categoryDistribution.getOrDefault(cat, 0) + m.getQuantity());
        }

        data.put("categoryDistribution", categoryDistribution);
        data.put("totalValue", all.stream().mapToDouble(m -> m.getPrice() * m.getQuantity()).sum());
        data.put("totalCount", all.size());
        data.put("lowStock", medicineRepository.countLowStockMedicines());
        data.put("expired", medicineRepository.countExpiredMedicines(LocalDate.now()));

        return ResponseEntity.ok(data);
    }
}
