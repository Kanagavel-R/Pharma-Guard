package com.pharmaguard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Entity
@Table(name = "medicines", indexes = {
    @Index(name = "idx_med_name", columnList = "medicineName"),
    @Index(name = "idx_med_batch", columnList = "batchNumber"),
    @Index(name = "idx_med_expiry", columnList = "expiryDate"),
    @Index(name = "idx_med_category", columnList = "category"),
    @Index(name = "idx_med_barcode", columnList = "barcode")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_med_batch_number", columnNames = {"batchNumber"})
})
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotBlank(message = "Medicine name is required")
    @Size(
            max = 100,
            message = "Medicine name cannot exceed 100 characters"
    )
    private String medicineName;


    @NotBlank(message = "Category is required")
    @Size(
            max = 50,
            message = "Category cannot exceed 50 characters"
    )
    private String category;


    @NotBlank(message = "Batch number is required")
    @Size(
            max = 50,
            message = "Batch number cannot exceed 50 characters"
    )
    @Column(unique = true)
    private String batchNumber;

    private String genericName;
    private String strength;
    private String dosageForm;
    private String manufacturer;
    private String barcode;


    @Min(
            value = 0,
            message = "Quantity cannot be negative"
    )
    private int quantity;


    @DecimalMin(
            value = "0.01",
            message = "Price must be greater than 0"
    )
    private double price;


    @NotNull(
            message = "Expiry date is required"
    )
    private LocalDate expiryDate;


    @Min(
            value = 0,
            message = "Reorder level cannot be negative"
    )
    private int reorderLevel;


    @Size(
            max = 100,
            message = "Supplier name cannot exceed 100 characters"
    )
    private String supplierName;

    @Min(value = 0, message = "Lead time cannot be negative")
    private int leadTimeDays = 7;

    @Min(value = 0, message = "Safety stock cannot be negative")
    private int safetyStock = 10;

    @Version
    private Long version;

    public Medicine() {
    }


    public Medicine(
            String medicineName,
            String category,
            String batchNumber,
            int quantity,
            double price,
            LocalDate expiryDate,
            int reorderLevel,
            String supplierName) {

        this.medicineName = medicineName;
        this.category = category;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.price = price;
        this.expiryDate = expiryDate;
        this.reorderLevel = reorderLevel;
        this.supplierName = supplierName;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }


    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }


    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }


    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }


    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }


    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    @Size(max = 100, message = "Storage location cannot exceed 100 characters")
    private String storageLocation = "Main Rack";

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public int getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(int safetyStock) {
        this.safetyStock = safetyStock;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getManufacturer() {
        return manufacturer != null && !manufacturer.isBlank() ? manufacturer : supplierName;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}