package com.pharmaguard.dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardIntelligenceDTO {

    private long totalMedicines;
    private long totalStock;
    private long lowStockCount;
    private long activeAlertCount;
    private long stockOutRiskCount;
    private long reorderRecommendedCount;
    private long expiryWasteRiskCount;
    private long fefoRecommendationCount;
    private List<String> smartRecommendations = new ArrayList<>();

    public DashboardIntelligenceDTO() {
    }

    public long getTotalMedicines() {
        return totalMedicines;
    }

    public void setTotalMedicines(long totalMedicines) {
        this.totalMedicines = totalMedicines;
    }

    public long getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(long totalStock) {
        this.totalStock = totalStock;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public long getActiveAlertCount() {
        return activeAlertCount;
    }

    public void setActiveAlertCount(long activeAlertCount) {
        this.activeAlertCount = activeAlertCount;
    }

    public long getStockOutRiskCount() {
        return stockOutRiskCount;
    }

    public void setStockOutRiskCount(long stockOutRiskCount) {
        this.stockOutRiskCount = stockOutRiskCount;
    }

    public long getReorderRecommendedCount() {
        return reorderRecommendedCount;
    }

    public void setReorderRecommendedCount(long reorderRecommendedCount) {
        this.reorderRecommendedCount = reorderRecommendedCount;
    }

    public long getExpiryWasteRiskCount() {
        return expiryWasteRiskCount;
    }

    public void setExpiryWasteRiskCount(long expiryWasteRiskCount) {
        this.expiryWasteRiskCount = expiryWasteRiskCount;
    }

    public long getFefoRecommendationCount() {
        return fefoRecommendationCount;
    }

    public void setFefoRecommendationCount(long fefoRecommendationCount) {
        this.fefoRecommendationCount = fefoRecommendationCount;
    }

    public List<String> getSmartRecommendations() {
        return smartRecommendations;
    }

    public void setSmartRecommendations(List<String> smartRecommendations) {
        this.smartRecommendations = smartRecommendations;
    }

    public void addSmartRecommendation(String recommendation) {
        this.smartRecommendations.add(recommendation);
    }
}
