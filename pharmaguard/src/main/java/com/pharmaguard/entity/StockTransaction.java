package com.pharmaguard.entity;

import com.pharmaguard.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions", indexes = {
    @Index(name = "idx_stk_tx_med_id", columnList = "medicine_id"),
    @Index(name = "idx_stk_tx_date", columnList = "transactionDate"),
    @Index(name = "idx_stk_tx_type", columnList = "transactionType")
})
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    @NotNull(message = "Medicine reference is required")
    private Medicine medicine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    private String notes;

    private Integer previousStock;
    private Integer newStock;
    private String performedBy;

    public StockTransaction() {
    }

    public StockTransaction(Medicine medicine, TransactionType transactionType, int quantity, LocalDateTime transactionDate, String notes) {
        this.medicine = medicine;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.transactionDate = transactionDate;
        this.notes = notes;
    }

    public StockTransaction(Medicine medicine, TransactionType transactionType, int quantity, LocalDateTime transactionDate, String notes, Integer previousStock, Integer newStock, String performedBy) {
        this.medicine = medicine;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.transactionDate = transactionDate;
        this.notes = notes;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.performedBy = performedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(Integer previousStock) {
        this.previousStock = previousStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public String getPerformedBy() {
        return performedBy != null ? performedBy : "Admin";
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
}
