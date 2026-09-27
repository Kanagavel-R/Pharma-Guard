package com.pharmaguard.service;

import com.pharmaguard.entity.PharmacySettings;
import com.pharmaguard.repository.PharmacySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SettingsService {

    private final PharmacySettingsRepository settingsRepository;

    public SettingsService(PharmacySettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public PharmacySettings getSettings() {
        List<PharmacySettings> list = settingsRepository.findAll();
        if (list.isEmpty()) {
            PharmacySettings defaultSettings = new PharmacySettings();
            return settingsRepository.save(defaultSettings);
        }
        return list.get(0);
    }

    @Transactional
    public PharmacySettings saveSettings(PharmacySettings updated) {
        PharmacySettings existing = getSettings();

        // Pharmacy Details
        if (updated.getPharmacyName() != null) existing.setPharmacyName(updated.getPharmacyName());
        if (updated.getPharmacyId() != null) existing.setPharmacyId(updated.getPharmacyId());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        if (updated.getAddress() != null) existing.setAddress(updated.getAddress());
        if (updated.getWorkingHours() != null) existing.setWorkingHours(updated.getWorkingHours());

        // Theme & Appearance
        if (updated.getTheme() != null) existing.setTheme(updated.getTheme());
        if (updated.getLayout() != null) existing.setLayout(updated.getLayout());
        if (updated.getAccentColor() != null) existing.setAccentColor(updated.getAccentColor());

        // Inventory Intelligence & Thresholds
        existing.setDefaultMinStock(updated.getDefaultMinStock());
        existing.setExpiryWarningDays(updated.getExpiryWarningDays());
        existing.setSafetyStock(updated.getSafetyStock());
        existing.setLeadTimeDays(updated.getLeadTimeDays());
        existing.setEnableFefo(updated.isEnableFefo());
        existing.setStockOutPrediction(updated.isStockOutPrediction());
        existing.setReorderRecommendations(updated.isReorderRecommendations());
        existing.setExpiryWasteDetection(updated.isExpiryWasteDetection());
        existing.setAutoLowStockAlerts(updated.isAutoLowStockAlerts());
        existing.setAutoExpiryAlerts(updated.isAutoExpiryAlerts());

        // AI Assistant Settings
        existing.setAiSuggestions(updated.isAiSuggestions());
        existing.setAiInventoryAnalysis(updated.isAiInventoryAnalysis());
        existing.setAiExpiryRiskAnalysis(updated.isAiExpiryRiskAnalysis());
        existing.setAiMedicineSafetyInsights(updated.isAiMedicineSafetyInsights());
        existing.setAiDailySummary(updated.isAiDailySummary());
        if (updated.getAiResponseStyle() != null) existing.setAiResponseStyle(updated.getAiResponseStyle());

        // Scanner Settings
        if (updated.getScannerType() != null) existing.setScannerType(updated.getScannerType());
        existing.setAutoOpenScanResult(updated.isAutoOpenScanResult());
        existing.setPlayScanSound(updated.isPlayScanSound());
        existing.setVibrationFeedback(updated.isVibrationFeedback());
        existing.setAutoUpdateStockOnScan(updated.isAutoUpdateStockOnScan());

        // Notifications
        existing.setLowStockAlerts(updated.isLowStockAlerts());
        existing.setLowStockWarningThreshold(updated.getLowStockWarningThreshold());
        existing.setLowStockCriticalThreshold(updated.getLowStockCriticalThreshold());
        existing.setExpiryAlerts(updated.isExpiryAlerts());
        existing.setExpiryFirstWarningDays(updated.getExpiryFirstWarningDays());
        existing.setExpirySecondWarningDays(updated.getExpirySecondWarningDays());
        existing.setExpiryCriticalWarningDays(updated.getExpiryCriticalWarningDays());
        existing.setExpiredMedicineAlerts(updated.isExpiredMedicineAlerts());
        existing.setNewOrderAlerts(updated.isNewOrderAlerts());
        existing.setSupplierAlerts(updated.isSupplierAlerts());
        existing.setSystemNotifications(updated.isSystemNotifications());
        existing.setVerificationAlerts(updated.isVerificationAlerts());
        existing.setSecurityNotifications(updated.isSecurityNotifications());
        existing.setEmailNotifications(updated.isEmailNotifications());
        existing.setInAppNotifications(updated.isInAppNotifications());

        // Daily Operating Hours
        if (updated.getMonHours() != null) existing.setMonHours(updated.getMonHours());
        if (updated.getTueHours() != null) existing.setTueHours(updated.getTueHours());
        if (updated.getWedHours() != null) existing.setWedHours(updated.getWedHours());
        if (updated.getThuHours() != null) existing.setThuHours(updated.getThuHours());
        if (updated.getFriHours() != null) existing.setFriHours(updated.getFriHours());
        if (updated.getSatHours() != null) existing.setSatHours(updated.getSatHours());
        if (updated.getSunHours() != null) existing.setSunHours(updated.getSunHours());

        // Security
        existing.setTwoFactorAuth(updated.isTwoFactorAuth());
        existing.setSessionTimeoutMinutes(updated.getSessionTimeoutMinutes());
        existing.setLoginNotifications(updated.isLoginNotifications());

        // Reports & Backup
        if (updated.getDefaultReportPeriod() != null) existing.setDefaultReportPeriod(updated.getDefaultReportPeriod());
        if (updated.getDefaultExportFormat() != null) existing.setDefaultExportFormat(updated.getDefaultExportFormat());
        existing.setIncludeExpiredInReports(updated.isIncludeExpiredInReports());
        existing.setIncludeLowStockInReports(updated.isIncludeLowStockInReports());
        existing.setAutoBackup(updated.isAutoBackup());
        if (updated.getBackupFrequency() != null) existing.setBackupFrequency(updated.getBackupFrequency());

        // Regional Preferences
        if (updated.getLanguage() != null) existing.setLanguage(updated.getLanguage());
        if (updated.getCurrency() != null) existing.setCurrency(updated.getCurrency());
        if (updated.getDateFormat() != null) existing.setDateFormat(updated.getDateFormat());
        if (updated.getTimezone() != null) existing.setTimezone(updated.getTimezone());

        return settingsRepository.save(existing);
    }

    @Transactional
    public PharmacySettings resetToDefault() {
        PharmacySettings settings = getSettings();
        settings.setTheme("Light");
        settings.setLayout("Comfortable");
        settings.setAccentColor("Blue");
        settings.setDefaultMinStock(20);
        settings.setExpiryWarningDays(30);
        settings.setSafetyStock(20);
        settings.setLeadTimeDays(7);
        settings.setEnableFefo(true);
        settings.setStockOutPrediction(true);
        settings.setReorderRecommendations(true);
        settings.setExpiryWasteDetection(true);
        settings.setAutoLowStockAlerts(true);
        settings.setAutoExpiryAlerts(true);
        settings.setAiSuggestions(true);
        settings.setAiInventoryAnalysis(true);
        settings.setAiExpiryRiskAnalysis(true);
        settings.setAiMedicineSafetyInsights(true);
        settings.setAiDailySummary(true);
        settings.setAiResponseStyle("Balanced");
        settings.setScannerType("Barcode + QR");
        settings.setAutoOpenScanResult(true);
        settings.setPlayScanSound(true);
        settings.setVibrationFeedback(true);
        settings.setAutoUpdateStockOnScan(false);
        settings.setLowStockAlerts(true);
        settings.setLowStockWarningThreshold(20);
        settings.setLowStockCriticalThreshold(5);
        settings.setExpiryAlerts(true);
        settings.setExpiryFirstWarningDays(90);
        settings.setExpirySecondWarningDays(30);
        settings.setExpiryCriticalWarningDays(7);
        settings.setExpiredMedicineAlerts(true);
        settings.setNewOrderAlerts(true);
        settings.setSupplierAlerts(false);
        settings.setSystemNotifications(true);
        settings.setTwoFactorAuth(false);
        settings.setSessionTimeoutMinutes(30);
        settings.setLoginNotifications(true);
        settings.setDefaultReportPeriod("Monthly");
        settings.setDefaultExportFormat("PDF");
        settings.setIncludeExpiredInReports(true);
        settings.setIncludeLowStockInReports(true);
        settings.setAutoBackup(true);
        settings.setBackupFrequency("Daily");
        settings.setLanguage("English");
        settings.setCurrency("INR (₹)");
        settings.setDateFormat("DD/MM/YYYY");
        settings.setTimezone("IST (UTC+5:30)");
        return settingsRepository.save(settings);
    }
}
