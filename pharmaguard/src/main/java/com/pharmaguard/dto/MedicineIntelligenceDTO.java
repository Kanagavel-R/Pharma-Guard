package com.pharmaguard.dto;

import com.pharmaguard.entity.Medicine;

public class MedicineIntelligenceDTO {

    private Medicine medicine;
    private double averageDailyConsumption;
    private boolean hasSufficientData;
    private Double estimatedStockOutDays;
    private String stockOutStatus;
    private String stockOutBadgeClass;
    private int reorderPoint;
    private int recommendedReorderQuantity;
    private boolean isReorderRecommended;
    private String reorderExplanation;
    private Long daysUntilExpiry;
    private Double expectedConsumptionBeforeExpiry;
    private int potentialExpiryWaste;
    private boolean isExpiryWasteRisk;
    private String expiryWasteExplanation;
    private Medicine fefoRecommendedBatch;
    private String fefoRecommendationMessage;
    private String summaryStatusText;

    public MedicineIntelligenceDTO() {
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }

    public double getAverageDailyConsumption() {
        return averageDailyConsumption;
    }

    public void setAverageDailyConsumption(double averageDailyConsumption) {
        this.averageDailyConsumption = averageDailyConsumption;
    }

    public boolean isHasSufficientData() {
        return hasSufficientData;
    }

    public void setHasSufficientData(boolean hasSufficientData) {
        this.hasSufficientData = hasSufficientData;
    }

    public Double getEstimatedStockOutDays() {
        return estimatedStockOutDays;
    }

    public void setEstimatedStockOutDays(Double estimatedStockOutDays) {
        this.estimatedStockOutDays = estimatedStockOutDays;
    }

    public String getStockOutStatus() {
        return stockOutStatus;
    }

    public void setStockOutStatus(String stockOutStatus) {
        this.stockOutStatus = stockOutStatus;
    }

    public String getStockOutBadgeClass() {
        return stockOutBadgeClass;
    }

    public void setStockOutBadgeClass(String stockOutBadgeClass) {
        this.stockOutBadgeClass = stockOutBadgeClass;
    }

    public int getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(int reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public int getRecommendedReorderQuantity() {
        return recommendedReorderQuantity;
    }

    public void setRecommendedReorderQuantity(int recommendedReorderQuantity) {
        this.recommendedReorderQuantity = recommendedReorderQuantity;
    }

    public boolean isReorderRecommended() {
        return isReorderRecommended;
    }

    public void setReorderRecommended(boolean reorderRecommended) {
        isReorderRecommended = reorderRecommended;
    }

    public String getReorderExplanation() {
        return reorderExplanation;
    }

    public void setReorderExplanation(String reorderExplanation) {
        this.reorderExplanation = reorderExplanation;
    }

    public Long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public void setDaysUntilExpiry(Long daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public Double getExpectedConsumptionBeforeExpiry() {
        return expectedConsumptionBeforeExpiry;
    }

    public void setExpectedConsumptionBeforeExpiry(Double expectedConsumptionBeforeExpiry) {
        this.expectedConsumptionBeforeExpiry = expectedConsumptionBeforeExpiry;
    }

    public int getPotentialExpiryWaste() {
        return potentialExpiryWaste;
    }

    public void setPotentialExpiryWaste(int potentialExpiryWaste) {
        this.potentialExpiryWaste = potentialExpiryWaste;
    }

    public boolean isExpiryWasteRisk() {
        return isExpiryWasteRisk;
    }

    public void setExpiryWasteRisk(boolean expiryWasteRisk) {
        isExpiryWasteRisk = expiryWasteRisk;
    }

    public String getExpiryWasteExplanation() {
        return expiryWasteExplanation;
    }

    public void setExpiryWasteExplanation(String expiryWasteExplanation) {
        this.expiryWasteExplanation = expiryWasteExplanation;
    }

    public Medicine getFefoRecommendedBatch() {
        return fefoRecommendedBatch;
    }

    public void setFefoRecommendedBatch(Medicine fefoRecommendedBatch) {
        this.fefoRecommendedBatch = fefoRecommendedBatch;
    }

    public String getFefoRecommendationMessage() {
        return fefoRecommendationMessage;
    }

    public void setFefoRecommendationMessage(String fefoRecommendationMessage) {
        this.fefoRecommendationMessage = fefoRecommendationMessage;
    }

    public String getSummaryStatusText() {
        return summaryStatusText;
    }

    public void setSummaryStatusText(String summaryStatusText) {
        this.summaryStatusText = summaryStatusText;
    }
}
