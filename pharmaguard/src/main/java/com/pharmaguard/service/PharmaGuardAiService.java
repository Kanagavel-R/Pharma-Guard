package com.pharmaguard.service;

import com.pharmaguard.dto.AiActionExecutionRequest;
import com.pharmaguard.dto.AiChatResponse;
import com.pharmaguard.entity.Alert;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.entity.Supplier;
import com.pharmaguard.enums.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PharmaGuardAiService {

    private static final Logger logger = LoggerFactory.getLogger(PharmaGuardAiService.class);

    private final MedicineService medicineService;
    private final ExpiryAlertService expiryAlertService;
    private final AlertService alertService;
    private final SupplierService supplierService;
    private final StockTransactionService stockTransactionService;
    private final VerificationService verificationService;
    private final GeminiService geminiService;
    private final RecallService recallService;

    public PharmaGuardAiService(
            MedicineService medicineService,
            ExpiryAlertService expiryAlertService,
            AlertService alertService,
            SupplierService supplierService,
            StockTransactionService stockTransactionService,
            VerificationService verificationService,
            GeminiService geminiService,
            RecallService recallService) {
        this.medicineService = medicineService;
        this.expiryAlertService = expiryAlertService;
        this.alertService = alertService;
        this.supplierService = supplierService;
        this.stockTransactionService = stockTransactionService;
        this.verificationService = verificationService;
        this.geminiService = geminiService;
        this.recallService = recallService;
    }

    public AiChatResponse processChatRequest(String prompt, Authentication auth) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return AiChatResponse.builder()
                    .reply("Please enter a question or command regarding pharmacy operations.")
                    .responseType("TEXT")
                    .build();
        }

        String userRole = getUserRole(auth);
        String lowerPrompt = prompt.toLowerCase().trim();

        // 1. ACTION INTENT DETECTION
        if (isActionIntent(lowerPrompt)) {
            return handleActionProposal(lowerPrompt, prompt, userRole);
        }

        // 2. ROLE-BASED ACCESS CONTROL FOR SENSITIVE DATA
        if (lowerPrompt.contains("user") || lowerPrompt.contains("admin") || lowerPrompt.contains("audit log")) {
            if (!hasRole(auth, "ROLE_ADMIN")) {
                return AiChatResponse.builder()
                        .reply("Access Restricted: User administration and system audit logs require Administrator privileges. Your current role is: " + userRole.replace("ROLE_", ""))
                        .responseType("WARNING")
                        .sourceData("Security Policy Enforcement")
                        .build();
            }
        }

        // 3. DETERMINISTIC QUERY ROUTING WITH REAL DATABASE DATA
        if (lowerPrompt.contains("low stock") || lowerPrompt.contains("running low") || lowerPrompt.contains("out of stock") || lowerPrompt.contains("reorder recommended")) {
            return handleLowStockQuery(prompt);
        }

        if (lowerPrompt.contains("expiring soon") || lowerPrompt.contains("expire") || lowerPrompt.contains("expiring within") || lowerPrompt.contains("critical expiry")) {
            if (lowerPrompt.contains("expired")) {
                return handleExpiredQuery(prompt);
            }
            return handleExpiringQuery(prompt);
        }

        if (lowerPrompt.contains("expired")) {
            return handleExpiredQuery(prompt);
        }

        if (lowerPrompt.contains("recall") || lowerPrompt.contains("recalled batch") || lowerPrompt.contains("recalled medicine")) {
            long activeCount = recallService.getActiveRecallCount();
            return AiChatResponse.builder()
                    .reply("There are currently " + activeCount + " active medicine batch recall(s) flagged by manufacturers or health authorities. You can view, issue, or manage recalls under the Recalls module (/recalls).")
                    .responseType("TEXT")
                    .sourceData("Real-time Database Query: Medicine Recalls")
                    .build();
        }

        if (lowerPrompt.contains("alert") || lowerPrompt.contains("warning") || lowerPrompt.contains("critical notification")) {
            return handleAlertsQuery(prompt);
        }

        if (lowerPrompt.contains("supplier") || lowerPrompt.contains("vendor") || lowerPrompt.contains("distributor")) {
            return handleSuppliersQuery(prompt);
        }

        if (lowerPrompt.contains("transaction") || lowerPrompt.contains("activity") || lowerPrompt.contains("recent stock")) {
            return handleTransactionsQuery(prompt);
        }

        if (lowerPrompt.contains("verification") || lowerPrompt.contains("counterfeit") || lowerPrompt.contains("safety score") || lowerPrompt.contains("scan risk")) {
            return handleVerificationQuery(prompt);
        }

        if (lowerPrompt.contains("reorder") || lowerPrompt.contains("replenish") || lowerPrompt.contains("recommendation")) {
            return handleReorderRecommendations(prompt);
        }

        if (lowerPrompt.contains("risk") || lowerPrompt.contains("urgent attention") || lowerPrompt.contains("threat")) {
            return handleRiskAnalysis(prompt);
        }

        if (lowerPrompt.contains("report") || lowerPrompt.contains("generate report")) {
            return handleReportGeneration(prompt);
        }

        if (lowerPrompt.contains("summary") || lowerPrompt.contains("overview") || lowerPrompt.contains("statistic") || lowerPrompt.contains("dashboard") || lowerPrompt.contains("how many medicines") || lowerPrompt.contains("total stock")) {
            return handleDashboardSummaryQuery(prompt);
        }

        // 4. MEDICINE LOOKUP
        Medicine searchedMed = findMedicineInPrompt(lowerPrompt);
        if (searchedMed != null) {
            return handleSingleMedicineQuery(searchedMed, prompt);
        }

        // 5. GENERAL PHARMACY INTELLIGENCE / REASONING QUERY (ENRICHED WITH REAL CONTEXT)
        return handleGeneralQueryWithContext(prompt, userRole);
    }

    private boolean isActionIntent(String prompt) {
        return prompt.startsWith("reduce stock") || prompt.startsWith("remove stock") || prompt.startsWith("deduct stock") ||
               prompt.startsWith("add stock") || prompt.startsWith("increase stock") ||
               prompt.startsWith("dismiss alert") || prompt.startsWith("reorder") || prompt.startsWith("delete medicine");
    }

    private AiChatResponse handleActionProposal(String lowerPrompt, String originalPrompt, String userRole) {
        if (!userRole.contains("ADMIN") && !userRole.contains("PHARMACIST")) {
            return AiChatResponse.builder()
                    .reply("Unauthorized Action: Modifying inventory or dismissing alerts requires Pharmacist or Administrator authorization.")
                    .responseType("WARNING")
                    .sourceData("Pharma Guard Security Engine")
                    .build();
        }

        // Extract medicine if mentioned
        Medicine targetMed = findMedicineInPrompt(lowerPrompt);

        if (lowerPrompt.contains("reduce") || lowerPrompt.contains("remove") || lowerPrompt.contains("deduct")) {
            int quantity = extractNumber(lowerPrompt, 10);
            if (targetMed != null) {
                int current = targetMed.getQuantity();
                int proposed = Math.max(0, current - quantity);

                AiChatResponse.ActionProposal proposal = AiChatResponse.ActionProposal.builder()
                        .actionId(UUID.randomUUID().toString())
                        .actionType("REDUCE_STOCK")
                        .targetEntityId(targetMed.getId())
                        .entityName(targetMed.getMedicineName())
                        .description("Reduce stock level for " + targetMed.getMedicineName() + " by " + quantity + " units.")
                        .confirmationMessage("Current Stock: " + current + " units | Proposed Remaining: " + proposed + " units.")
                        .parameters(Map.of("quantity", quantity, "medicineName", targetMed.getMedicineName()))
                        .confirmButtonText("Confirm Reduction (-" + quantity + ")")
                        .cancelButtonText("Cancel Action")
                        .build();

                return AiChatResponse.builder()
                        .reply("Action Confirmation Required: Proposed inventory reduction for **" + targetMed.getMedicineName() + "**.")
                        .responseType("ACTION_REQUIRED")
                        .requiresConfirmation(true)
                        .actionProposal(proposal)
                        .sourceData("Pharma Guard Action Engine")
                        .build();
            } else {
                return AiChatResponse.builder()
                        .reply("Please specify a valid medicine name registered in Pharma Guard to reduce stock (e.g., 'Reduce stock for Paracetamol 500mg by 10').")
                        .responseType("TEXT")
                        .build();
            }
        }

        if (lowerPrompt.contains("dismiss alert")) {
            List<Alert> alerts = alertService.getActiveAlerts();
            if (alerts.isEmpty()) {
                return AiChatResponse.builder()
                        .reply("There are no active alerts to dismiss.")
                        .responseType("TEXT")
                        .build();
            }
            Alert targetAlert = alerts.get(0);
            AiChatResponse.ActionProposal proposal = AiChatResponse.ActionProposal.builder()
                    .actionId(UUID.randomUUID().toString())
                    .actionType("DISMISS_ALERT")
                    .targetEntityId(targetAlert.getId())
                    .entityName("Alert #" + targetAlert.getId())
                    .description("Dismiss alert: " + targetAlert.getMessage())
                    .confirmationMessage("Are you sure you want to dismiss this alert for " + (targetAlert.getMedicineName() != null ? targetAlert.getMedicineName() : "Medicine") + "?")
                    .parameters(Map.of("alertId", targetAlert.getId()))
                    .confirmButtonText("Confirm Dismissal")
                    .cancelButtonText("Cancel")
                    .build();

            return AiChatResponse.builder()
                    .reply("Action Confirmation Required: Dismiss alert for " + (targetAlert.getMedicineName() != null ? targetAlert.getMedicineName() : "Medicine") + ".")
                    .responseType("ACTION_REQUIRED")
                    .requiresConfirmation(true)
                    .actionProposal(proposal)
                    .sourceData("Pharma Guard Alert Service")
                    .build();
        }

        return AiChatResponse.builder()
                .reply("Please specify the exact action and medicine details (e.g., 'Reduce stock of Amoxicillin 500mg by 20').")
                .responseType("TEXT")
                .build();
    }

    public AiChatResponse executeConfirmedAction(AiActionExecutionRequest request, Authentication auth) {
        String userRole = getUserRole(auth);
        if (!userRole.contains("ADMIN") && !userRole.contains("PHARMACIST")) {
            return AiChatResponse.builder()
                    .reply("Action Execution Denied: Your account role does not have permission to execute database mutations.")
                    .responseType("WARNING")
                    .build();
        }

        if ("REDUCE_STOCK".equals(request.getActionType())) {
            Long medId = request.getTargetEntityId();
            int qty = request.getParameters() != null && request.getParameters().containsKey("quantity") ?
                    Integer.parseInt(request.getParameters().get("quantity").toString()) : 10;

            try {
                Medicine med = medicineService.getMedicineById(medId);
                int oldStock = med.getQuantity();
                int newStock = Math.max(0, oldStock - qty);
                med.setQuantity(newStock);
                medicineService.saveMedicine(med);

                stockTransactionService.recordTransaction(med, TransactionType.STOCK_OUT, qty, "AI Assistant Manual Stock Reduction", oldStock, newStock, auth.getName());

                return AiChatResponse.builder()
                        .reply("Stock Updated Successfully: **" + med.getMedicineName() + "** stock reduced from " + oldStock + " to **" + newStock + "** units.")
                        .responseType("TEXT")
                        .sourceData("Pharma Guard Transaction Log")
                        .quickActions(List.of("Check Low Stock", "View Transactions"))
                        .build();
            } catch (Exception e) {
                return AiChatResponse.builder()
                        .reply("Failed to update stock: " + e.getMessage())
                        .responseType("WARNING")
                        .build();
            }
        } else if ("DISMISS_ALERT".equals(request.getActionType())) {
            Long alertId = request.getTargetEntityId();
            try {
                alertService.resolveAlert(alertId);
                return AiChatResponse.builder()
                        .reply("Alert #" + alertId + " has been successfully resolved and dismissed.")
                        .responseType("TEXT")
                        .sourceData("Pharma Guard Alert Service")
                        .quickActions(List.of("Today's Alerts", "Inventory Summary"))
                        .build();
            } catch (Exception e) {
                return AiChatResponse.builder()
                        .reply("Failed to dismiss alert: " + e.getMessage())
                        .responseType("WARNING")
                        .build();
            }
        }

        return AiChatResponse.builder()
                .reply("Action execution failed or unsupported action type.")
                .responseType("WARNING")
                .build();
    }

    private AiChatResponse handleLowStockQuery(String userPrompt) {
        List<Medicine> lowStockMeds = medicineService.getAllMedicines().stream()
                .filter(m -> m.getQuantity() <= m.getReorderLevel())
                .collect(Collectors.toList());

        if (lowStockMeds.isEmpty()) {
            return AiChatResponse.builder()
                    .reply("All medicine stock levels are currently healthy! No medicines are at or below their reorder thresholds.")
                    .responseType("TEXT")
                    .sourceData("Source: Pharma Guard Live Inventory DB")
                    .quickActions(List.of("View Expiring Medicines", "Today's Alerts", "Inventory Summary"))
                    .build();
        }

        List<String> headers = List.of("Medicine Name", "Category", "Current Stock", "Reorder Level", "Status");
        List<List<String>> rows = new ArrayList<>();

        for (Medicine m : lowStockMeds) {
            String status = m.getQuantity() == 0 ? "CRITICAL OUT OF STOCK" : "LOW STOCK";
            rows.add(List.of(
                    m.getMedicineName() != null ? m.getMedicineName() : "Unknown",
                    m.getCategory() != null ? m.getCategory() : "General",
                    String.valueOf(m.getQuantity()),
                    String.valueOf(m.getReorderLevel()),
                    status
            ));
        }

        String summary = "Found " + lowStockMeds.size() + " medicine(s) requiring stock replenishment according to Pharma Guard reorder thresholds.";

        return AiChatResponse.builder()
                .reply(summary)
                .responseType("TABLE")
                .sourceData("Source: Pharma Guard Live Inventory Database (" + lowStockMeds.size() + " items found)")
                .tableHeaders(headers)
                .tableRows(rows)
                .quickActions(List.of("View Expiring Medicines", "Today's Alerts", "Inventory Summary"))
                .build();
    }

    private AiChatResponse handleExpiringQuery(String userPrompt) {
        List<Medicine> expiringMeds = expiryAlertService.getExpiringMedicines(30);
        if (expiringMeds.isEmpty()) {
            return AiChatResponse.builder()
                    .reply("No medicines are expiring within the next 30 days based on active database records.")
                    .responseType("TEXT")
                    .sourceData("Source: Pharma Guard Expiry Protection Engine")
                    .quickActions(List.of("Check Low Stock", "Today's Alerts", "Inventory Summary"))
                    .build();
        }

        List<String> headers = List.of("Medicine Name", "Batch #", "Expiry Date", "Stock", "Risk Level");
        List<List<String>> rows = new ArrayList<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (Medicine m : expiringMeds) {
            String expDateStr = m.getExpiryDate() != null ? m.getExpiryDate().format(dtf) : "N/A";
            rows.add(List.of(
                    m.getMedicineName(),
                    m.getBatchNumber() != null ? m.getBatchNumber() : "N/A",
                    expDateStr,
                    String.valueOf(m.getQuantity()),
                    "EXPIRING SOON"
            ));
        }

        String summary = "Pharma Guard detected " + expiringMeds.size() + " medicine(s) expiring within the next 30 days. Priority dispensing under FEFO rules is recommended.";

        return AiChatResponse.builder()
                .reply(summary)
                .responseType("TABLE")
                .sourceData("Source: Pharma Guard Expiry Protection Records")
                .tableHeaders(headers)
                .tableRows(rows)
                .quickActions(List.of("Check Low Stock", "Today's Alerts", "Inventory Summary"))
                .build();
    }

    private AiChatResponse handleExpiredQuery(String userPrompt) {
        List<Medicine> expiredMeds = expiryAlertService.getExpiredMedicines();
        if (expiredMeds.isEmpty()) {
            return AiChatResponse.builder()
                    .reply("Zero expired medicines found in active inventory records.")
                    .responseType("TEXT")
                    .sourceData("Source: Pharma Guard Live Database")
                    .quickActions(List.of("View Expiring Medicines", "Check Low Stock", "Inventory Summary"))
                    .build();
        }

        List<String> headers = List.of("Medicine Name", "Batch #", "Expiry Date", "Stock Units", "Status");
        List<List<String>> rows = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (Medicine m : expiredMeds) {
            rows.add(List.of(
                    m.getMedicineName(),
                    m.getBatchNumber() != null ? m.getBatchNumber() : "N/A",
                    m.getExpiryDate() != null ? m.getExpiryDate().format(dtf) : "Expired",
                    String.valueOf(m.getQuantity()),
                    "EXPIRED — DO NOT DISPENSE"
            ));
        }

        return AiChatResponse.builder()
                .reply("CRITICAL SAFETY WARNING: " + expiredMeds.size() + " expired medicine(s) detected in database. Quarantining these items is strongly advised.")
                .responseType("WARNING")
                .sourceData("Source: Pharma Guard Expiry Protection System")
                .tableHeaders(headers)
                .tableRows(rows)
                .quickActions(List.of("View Expiring Medicines", "Today's Alerts", "Inventory Summary"))
                .build();
    }

    private AiChatResponse handleAlertsQuery(String userPrompt) {
        List<Alert> alerts = alertService.getActiveAlerts();
        if (alerts.isEmpty()) {
            return AiChatResponse.builder()
                    .reply("System Status Normal: No active alerts currently recorded in Pharma Guard.")
                    .responseType("TEXT")
                    .sourceData("Source: Pharma Guard Alert Monitoring Service")
                    .quickActions(List.of("Check Low Stock", "View Expiring Medicines", "Inventory Summary"))
                    .build();
        }

        List<String> headers = List.of("Alert ID", "Medicine", "Type / Severity", "Message", "Time");
        List<List<String>> rows = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm, dd MMM");

        for (Alert a : alerts) {
            rows.add(List.of(
                    "#" + a.getId(),
                    a.getMedicineName() != null ? a.getMedicineName() : "System",
                    a.getSeverity() != null ? a.getSeverity() : "HIGH",
                    a.getMessage() != null ? a.getMessage() : "Alert",
                    a.getCreatedAt() != null ? a.getCreatedAt().format(dtf) : "Today"
            ));
        }

        return AiChatResponse.builder()
                .reply("Active System Alerts (" + alerts.size() + " item(s)):")
                .responseType("TABLE")
                .sourceData("Source: Pharma Guard Real-Time Alert Engine")
                .tableHeaders(headers)
                .tableRows(rows)
                .quickActions(List.of("Check Low Stock", "View Expiring Medicines", "Inventory Summary"))
                .build();
    }

    private AiChatResponse handleSuppliersQuery(String userPrompt) {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        if (suppliers.isEmpty()) {
            return AiChatResponse.builder()
                    .reply("No registered suppliers found in Pharma Guard database.")
                    .responseType("TEXT")
                    .sourceData("Source: Pharma Guard Supplier Registry")
                    .build();
        }

        List<String> headers = List.of("Supplier Name", "Contact Person", "Email", "Phone", "Status");
        List<List<String>> rows = new ArrayList<>();

        for (Supplier s : suppliers) {
            rows.add(List.of(
                    s.getSupplierName() != null ? s.getSupplierName() : "N/A",
                    s.getContactPerson() != null ? s.getContactPerson() : "N/A",
                    s.getEmail() != null ? s.getEmail() : "N/A",
                    s.getPhone() != null ? s.getPhone() : "N/A",
                    "ACTIVE"
            ));
        }

        return AiChatResponse.builder()
                .reply("Registered Pharmacy Suppliers (" + suppliers.size() + " total):")
                .responseType("TABLE")
                .sourceData("Source: Pharma Guard Authorized Supplier Database")
                .tableHeaders(headers)
                .tableRows(rows)
                .quickActions(List.of("Inventory Summary", "Recent Transactions"))
                .build();
    }

    private AiChatResponse handleTransactionsQuery(String userPrompt) {
        List<StockTransaction> txs = stockTransactionService.getRecentTransactions();
        if (txs.isEmpty()) {
            return AiChatResponse.builder()
                    .reply("No recent stock transactions recorded in system logs.")
                    .responseType("TEXT")
                    .sourceData("Source: Pharma Guard Stock Audit Trail")
                    .build();
        }

        List<String> headers = List.of("Transaction ID", "Medicine", "Type", "Quantity", "User", "Timestamp");
        List<List<String>> rows = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm, dd MMM");

        int count = 0;
        for (StockTransaction t : txs) {
            if (count++ >= 8) break;
            rows.add(List.of(
                    "#" + t.getId(),
                    t.getMedicine() != null ? t.getMedicine().getMedicineName() : "Unknown",
                    t.getTransactionType() != null ? t.getTransactionType().name() : "IN",
                    String.valueOf(t.getQuantity()),
                    t.getPerformedBy() != null ? t.getPerformedBy() : "System",
                    t.getTransactionDate() != null ? t.getTransactionDate().format(dtf) : "Recently"
            ));
        }

        return AiChatResponse.builder()
                .reply("Recent Stock Movement Activity (Last " + rows.size() + " transactions):")
                .responseType("TABLE")
                .sourceData("Source: Pharma Guard Audit & Stock Transaction Log")
                .tableHeaders(headers)
                .tableRows(rows)
                .quickActions(List.of("Check Low Stock", "Inventory Summary"))
                .build();
    }

    private AiChatResponse handleVerificationQuery(String userPrompt) {
        long totalVerif = verificationService.getTotalVerificationsCount();
        long lowRisk = verificationService.getCountByRisk("LOW");
        long highRisk = verificationService.getCountByRisk("HIGH") + verificationService.getCountByRisk("CRITICAL");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("Total Verifications Performed", totalVerif);
        metrics.put("Verified Genuine (Low Risk)", lowRisk);
        metrics.put("Safety Flags / High Risk Scans", highRisk);
        metrics.put("Verification Compliance Score", totalVerif > 0 ? ((lowRisk * 100) / totalVerif) + "%" : "100%");

        return AiChatResponse.builder()
                .reply("Pharma Guard Verification & Counterfeit Prevention Intelligence:")
                .responseType("CARDS")
                .sourceData("Source: Pharma Guard Verification Engine & Scan Audit Log")
                .cardMetrics(metrics)
                .quickActions(List.of("Today's Alerts", "Inventory Summary", "Check Low Stock"))
                .build();
    }

    private AiChatResponse handleReorderRecommendations(String userPrompt) {
        List<Medicine> lowStockMeds = medicineService.getAllMedicines().stream()
                .filter(m -> m.getQuantity() <= m.getReorderLevel())
                .collect(Collectors.toList());

        if (lowStockMeds.isEmpty()) {
            return AiChatResponse.builder()
                    .reply("REORDER RECOMMENDATIONS\n\nAll stock levels are currently above minimum configured thresholds. No immediate reorders required.")
                    .responseType("TEXT")
                    .sourceData("Source: Pharma Guard Inventory Reorder Engine")
                    .quickActions(List.of("Check Low Stock", "View Expiring Medicines", "Pharmacy Summary"))
                    .build();
        }

        List<String> headers = List.of("Medicine Name", "Current Stock", "Minimum Stock", "Recommended Qty", "Reason");
        List<List<String>> rows = new ArrayList<>();

        for (Medicine m : lowStockMeds) {
            int current = m.getQuantity();
            int min = m.getReorderLevel();
            int recQty = Math.max(min * 2 - current, min);
            String reason = current == 0 ? "Critical Out of Stock — Immediate Order Needed" : "Stock below minimum safety threshold (" + min + " units)";

            rows.add(List.of(
                    m.getMedicineName() != null ? m.getMedicineName() : "Unknown",
                    String.valueOf(current),
                    String.valueOf(min),
                    String.valueOf(recQty),
                    reason
            ));
        }

        String summary = "REORDER RECOMMENDATIONS\n\nIdentified " + lowStockMeds.size() + " medicine(s) requiring stock replenishment. Note: Recommendations require user confirmation before order placement.";

        return AiChatResponse.builder()
                .reply(summary)
                .responseType("TABLE")
                .sourceData("Source: Pharma Guard Inventory & Reorder Intelligence Engine")
                .tableHeaders(headers)
                .tableRows(rows)
                .quickActions(List.of("Check Low Stock", "Today's Alerts", "Pharmacy Summary"))
                .build();
    }

    private AiChatResponse handleRiskAnalysis(String userPrompt) {
        long lowStockCount = medicineService.getLowStockCount();
        long expiredCount = expiryAlertService.countExpired();
        long expiringSoonCount = expiryAlertService.countCriticalExpiring(30);
        long activeAlerts = alertService.getActiveAlertCount();
        long highRiskScans = verificationService.getCountByRisk("HIGH") + verificationService.getCountByRisk("CRITICAL");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("Overall Risk Status", (expiredCount > 0 || highRiskScans > 0) ? "HIGH RISK — IMMEDIATE ACTION REQUIRED" : (lowStockCount > 0 ? "MEDIUM RISK — ATTENTION NEEDED" : "LOW RISK — HEALTHY OPERATIONAL STATE"));
        metrics.put("Out of Stock / Low Stock Risk Items", lowStockCount + " items");
        metrics.put("Expired Medicine Compliance Risk", expiredCount + " items");
        metrics.put("Near Expiry Risk (30 Days)", expiringSoonCount + " items");
        metrics.put("Active Unresolved Alerts", activeAlerts + " alerts");
        metrics.put("Verification Safety Flags", highRiskScans + " scans");

        StringBuilder analysis = new StringBuilder();
        analysis.append("PHARMA GUARD INVENTORY RISK ANALYSIS\n\n");
        if (expiredCount > 0) {
            analysis.append("- CRITICAL EXpiry RISK: ").append(expiredCount).append(" expired medicine(s) present in active inventory records requiring immediate quarantine.\n");
        }
        if (lowStockCount > 0) {
            analysis.append("- SUPPLY SHORTAGE RISK: ").append(lowStockCount).append(" medicine(s) are at or below reorder thresholds.\n");
        }
        if (highRiskScans > 0) {
            analysis.append("- VERIFICATION RISK: ").append(highRiskScans).append(" scan(s) flagged high risk or unverified packaging.\n");
        }
        if (expiredCount == 0 && lowStockCount == 0 && highRiskScans == 0) {
            analysis.append("- All core inventory, expiry, and verification safety metrics are operating within normal parameters.\n");
        }

        return AiChatResponse.builder()
                .reply(analysis.toString())
                .responseType("CARDS")
                .sourceData("Source: Pharma Guard Risk Analytics & Safety Engine")
                .cardMetrics(metrics)
                .quickActions(List.of("Check Low Stock", "View Expiring Medicines", "Today's Alerts"))
                .build();
    }

    private AiChatResponse handleReportGeneration(String userPrompt) {
        long totalMedicines = medicineService.getTotalMedicines();
        long totalStock = medicineService.getTotalStock();
        long lowStockCount = medicineService.getLowStockCount();
        long expiredCount = expiryAlertService.countExpired();
        long expiring30 = expiryAlertService.countCriticalExpiring(30);
        long totalSuppliers = supplierService.getAllSuppliers().size();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("Report Scope", "Pharma Guard Full Operations Report");
        metrics.put("Total Catalog Medicines", totalMedicines);
        metrics.put("Total On-Hand Inventory Units", totalStock);
        metrics.put("Low Stock / Reorder Count", lowStockCount);
        metrics.put("Near Expiry (30-Day Window)", expiring30);
        metrics.put("Quarantine / Expired Count", expiredCount);
        metrics.put("Authorized Suppliers Registered", totalSuppliers);

        return AiChatResponse.builder()
                .reply("PHARMACY OPERATIONS & INVENTORY AUDIT REPORT\n\nExecutive summary generated from real-time database records:")
                .responseType("CARDS")
                .sourceData("Source: Pharma Guard System Audit & Reporting Module")
                .cardMetrics(metrics)
                .quickActions(List.of("Check Low Stock", "View Expiring Medicines", "Suppliers"))
                .build();
    }

    private AiChatResponse handleDashboardSummaryQuery(String userPrompt) {
        long totalMedicines = medicineService.getTotalMedicines();
        long totalStock = medicineService.getTotalStock();
        long lowStockCount = medicineService.getLowStockCount();
        long expiredCount = expiryAlertService.countExpired();
        long expiringSoon = expiryAlertService.countCriticalExpiring(7);
        long activeAlerts = alertService.getActiveAlertCount();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("Total Registered Medicines", totalMedicines);
        metrics.put("Total Stock Units Available", totalStock);
        metrics.put("Low Stock Warnings", lowStockCount);
        metrics.put("Expiring (Next 7 Days)", expiringSoon);
        metrics.put("Expired Medicines", expiredCount);
        metrics.put("Active System Alerts", activeAlerts);

        return AiChatResponse.builder()
                .reply("Pharma Guard Overall Operations & Inventory Executive Summary:")
                .responseType("CARDS")
                .sourceData("Source: Pharma Guard Real-Time Application Database")
                .cardMetrics(metrics)
                .quickActions(List.of("Check Low Stock", "View Expiring Medicines", "Today's Alerts"))
                .build();
    }

    private AiChatResponse handleSingleMedicineQuery(Medicine med, String userPrompt) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("Medicine Name", med.getMedicineName());
        metrics.put("Category", med.getCategory() != null ? med.getCategory() : "General");
        metrics.put("Batch Number", med.getBatchNumber() != null ? med.getBatchNumber() : "Unassigned");
        metrics.put("Current Stock Quantity", med.getQuantity() + " units");
        metrics.put("Minimum Reorder Level", med.getReorderLevel() + " units");
        metrics.put("Unit Price", "$" + med.getPrice());
        metrics.put("Expiry Date", med.getExpiryDate() != null ? med.getExpiryDate().toString() : "N/A");
        metrics.put("Manufacturer / Supplier", med.getManufacturer() != null ? med.getManufacturer() : "Approved Source");

        return AiChatResponse.builder()
                .reply("Detailed Profile for Medicine **" + med.getMedicineName() + "**:")
                .responseType("CARDS")
                .sourceData("Source: Pharma Guard Medicine Catalog DB")
                .cardMetrics(metrics)
                .quickActions(List.of("Check Low Stock", "View Expiring Medicines", "Inventory Summary"))
                .build();
    }

    private AiChatResponse handleGeneralQueryWithContext(String prompt, String userRole) {
        long totalMeds = medicineService.getTotalMedicines();
        long lowStock = medicineService.getLowStockCount();
        long expiring = expiryAlertService.countCriticalExpiring(30);

        String dbContext = String.format("Current Pharma Guard State: %d total medicines, %d low-stock items, %d items expiring within 30 days. User role: %s.",
                totalMeds, lowStock, expiring, userRole);

        String fullPrompt = prompt + "\n\n[Context: " + dbContext + "]";

        String aiReply = geminiService.askAiAssistant(fullPrompt);

        return AiChatResponse.builder()
                .reply(aiReply)
                .responseType("TEXT")
                .sourceData("Source: Pharma Guard Operational Intelligence")
                .quickActions(List.of("Check Low Stock", "View Expiring Medicines", "Today's Alerts", "Inventory Summary"))
                .build();
    }

    private Medicine findMedicineInPrompt(String lowerPrompt) {
        List<Medicine> allMeds = medicineService.getAllMedicines();
        for (Medicine m : allMeds) {
            if (m.getMedicineName() != null && lowerPrompt.contains(m.getMedicineName().toLowerCase())) {
                return m;
            }
        }
        return null;
    }

    private int extractNumber(String prompt, int defaultVal) {
        try {
            String[] parts = prompt.split("\\s+");
            for (String p : parts) {
                if (p.matches("\\d+")) {
                    return Integer.parseInt(p);
                }
            }
        } catch (Exception ignored) {}
        return defaultVal;
    }

    private String getUserRole(Authentication auth) {
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (ga.getAuthority().startsWith("ROLE_")) {
                    return ga.getAuthority();
                }
            }
        }
        return "ROLE_STAFF";
    }

    private boolean hasRole(Authentication auth, String role) {
        return getUserRole(auth).equals(role);
    }
}
