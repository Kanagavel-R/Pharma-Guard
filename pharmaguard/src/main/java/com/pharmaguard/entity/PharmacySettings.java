package com.pharmaguard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pharmacy_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PharmacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Pharmacy Details
    private String pharmacyName = "Pharma Guard Central Pharmacy";
    private String pharmacyId = "PG-IND-8842";
    private String phone = "+91 98765 43210";
    private String email = "contact@pharmaguard.com";
    private String address = "104 Healthcare Boulevard, Medical Enclave";
    private String workingHours = "08:00 AM - 10:00 PM";
    private String pharmacyStatus = "Open";

    // Theme & Layout
    private String theme = "Light"; // Light, Dark, System
    private String layout = "Comfortable"; // Comfortable, Compact
    private String accentColor = "Blue"; // Blue, Teal, Purple, Green

    // Inventory Intelligence & Reorder Defaults
    @Column(nullable = true)
    private Integer defaultMinStock = 20;

    @Column(nullable = true)
    private Integer expiryWarningDays = 30;

    @Column(nullable = true)
    private Integer safetyStock = 20;

    @Column(nullable = true)
    private Integer leadTimeDays = 7;

    @Column(nullable = true)
    private Boolean enableFefo = true;

    @Column(nullable = true)
    private Boolean stockOutPrediction = true;

    @Column(nullable = true)
    private Boolean reorderRecommendations = true;

    @Column(nullable = true)
    private Boolean expiryWasteDetection = true;

    @Column(nullable = true)
    private Boolean autoLowStockAlerts = true;

    @Column(nullable = true)
    private Boolean autoExpiryAlerts = true;

    // AI Assistant Settings
    @Column(nullable = true)
    private Boolean aiSuggestions = true;

    @Column(nullable = true)
    private Boolean aiInventoryAnalysis = true;

    @Column(nullable = true)
    private Boolean aiExpiryRiskAnalysis = true;

    @Column(nullable = true)
    private Boolean aiMedicineSafetyInsights = true;

    @Column(nullable = true)
    private Boolean aiDailySummary = true;

    private String aiResponseStyle = "Balanced"; // Quick, Balanced, Detailed

    // Scanner Settings
    private String scannerType = "Barcode + QR";

    @Column(nullable = true)
    private Boolean autoOpenScanResult = true;

    @Column(nullable = true)
    private Boolean playScanSound = true;

    @Column(nullable = true)
    private Boolean vibrationFeedback = true;

    @Column(nullable = true)
    private Boolean autoUpdateStockOnScan = false;

    // Notifications Toggles & Thresholds
    @Column(nullable = true)
    private Boolean lowStockAlerts = true;

    @Column(nullable = true)
    private Integer lowStockWarningThreshold = 20;

    @Column(nullable = true)
    private Integer lowStockCriticalThreshold = 5;

    @Column(nullable = true)
    private Boolean expiryAlerts = true;

    @Column(nullable = true)
    private Integer expiryFirstWarningDays = 90;

    @Column(nullable = true)
    private Integer expirySecondWarningDays = 30;

    @Column(nullable = true)
    private Integer expiryCriticalWarningDays = 7;

    @Column(nullable = true)
    private Boolean expiredMedicineAlerts = true;

    @Column(nullable = true)
    private Boolean newOrderAlerts = true;

    @Column(nullable = true)
    private Boolean supplierAlerts = false;

    @Column(nullable = true)
    private Boolean systemNotifications = true;

    @Column(nullable = true)
    private Boolean verificationAlerts = true;

    @Column(nullable = true)
    private Boolean securityNotifications = true;

    @Column(nullable = true)
    private Boolean emailNotifications = true;

    @Column(nullable = true)
    private Boolean inAppNotifications = true;

    // Daily Operating Hours
    private String monHours = "09:00 AM - 09:00 PM";
    private String tueHours = "09:00 AM - 09:00 PM";
    private String wedHours = "09:00 AM - 09:00 PM";
    private String thuHours = "09:00 AM - 09:00 PM";
    private String friHours = "09:00 AM - 09:00 PM";
    private String satHours = "09:00 AM - 09:00 PM";
    private String sunHours = "Closed";

    // Security Settings
    @Column(nullable = true)
    private Boolean twoFactorAuth = false;

    @Column(nullable = true)
    private Integer sessionTimeoutMinutes = 30;

    @Column(nullable = true)
    private Boolean loginNotifications = true;

    // Report & Backup Preferences
    private String defaultReportPeriod = "Monthly";
    private String defaultExportFormat = "PDF";

    @Column(nullable = true)
    private Boolean includeExpiredInReports = true;

    @Column(nullable = true)
    private Boolean includeLowStockInReports = true;

    @Column(nullable = true)
    private Boolean autoBackup = true;

    private String backupFrequency = "Daily";

    // System Preferences
    private String language = "English";
    private String currency = "INR (₹)";
    private String dateFormat = "DD/MM/YYYY";
    private String timezone = "IST (UTC+5:30)";

    // Helper getters for boolean primitives in Thymeleaf and Service
    public boolean isEnableFefo() { return Boolean.TRUE.equals(enableFefo); }
    public boolean isStockOutPrediction() { return Boolean.TRUE.equals(stockOutPrediction); }
    public boolean isReorderRecommendations() { return Boolean.TRUE.equals(reorderRecommendations); }
    public boolean isExpiryWasteDetection() { return Boolean.TRUE.equals(expiryWasteDetection); }
    public boolean isAutoLowStockAlerts() { return Boolean.TRUE.equals(autoLowStockAlerts); }
    public boolean isAutoExpiryAlerts() { return Boolean.TRUE.equals(autoExpiryAlerts); }
    public boolean isAiSuggestions() { return Boolean.TRUE.equals(aiSuggestions); }
    public boolean isAiInventoryAnalysis() { return Boolean.TRUE.equals(aiInventoryAnalysis); }
    public boolean isAiExpiryRiskAnalysis() { return Boolean.TRUE.equals(aiExpiryRiskAnalysis); }
    public boolean isAiMedicineSafetyInsights() { return Boolean.TRUE.equals(aiMedicineSafetyInsights); }
    public boolean isAiDailySummary() { return Boolean.TRUE.equals(aiDailySummary); }
    public boolean isAutoOpenScanResult() { return Boolean.TRUE.equals(autoOpenScanResult); }
    public boolean isPlayScanSound() { return Boolean.TRUE.equals(playScanSound); }
    public boolean isVibrationFeedback() { return Boolean.TRUE.equals(vibrationFeedback); }
    public boolean isAutoUpdateStockOnScan() { return Boolean.TRUE.equals(autoUpdateStockOnScan); }
    public boolean isLowStockAlerts() { return Boolean.TRUE.equals(lowStockAlerts); }
    public boolean isExpiryAlerts() { return Boolean.TRUE.equals(expiryAlerts); }
    public boolean isExpiredMedicineAlerts() { return Boolean.TRUE.equals(expiredMedicineAlerts); }
    public boolean isNewOrderAlerts() { return Boolean.TRUE.equals(newOrderAlerts); }
    public boolean isSupplierAlerts() { return Boolean.TRUE.equals(supplierAlerts); }
    public boolean isSystemNotifications() { return Boolean.TRUE.equals(systemNotifications); }
    public boolean isVerificationAlerts() { return Boolean.TRUE.equals(verificationAlerts); }
    public boolean isSecurityNotifications() { return Boolean.TRUE.equals(securityNotifications); }
    public boolean isEmailNotifications() { return Boolean.TRUE.equals(emailNotifications); }
    public boolean isInAppNotifications() { return Boolean.TRUE.equals(inAppNotifications); }
    public boolean isTwoFactorAuth() { return Boolean.TRUE.equals(twoFactorAuth); }
    public boolean isLoginNotifications() { return Boolean.TRUE.equals(loginNotifications); }
    public boolean isIncludeExpiredInReports() { return Boolean.TRUE.equals(includeExpiredInReports); }
    public boolean isIncludeLowStockInReports() { return Boolean.TRUE.equals(includeLowStockInReports); }
    public boolean isAutoBackup() { return Boolean.TRUE.equals(autoBackup); }
}
