package com.pharmaguard.service;

import com.pharmaguard.entity.Supplier;
import com.pharmaguard.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Supplier not found with ID: " + id));
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Supplier> searchSuppliers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return supplierRepository.findAll();
        }
        return supplierRepository.findBySupplierNameContainingIgnoreCase(keyword.trim());
    }

    @Transactional(readOnly = true)
    public boolean isDuplicateSupplierName(String supplierName, Long currentId) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            return false;
        }
        String cleanName = supplierName.trim();
        if (currentId == null) {
            return supplierRepository.existsBySupplierNameIgnoreCase(cleanName);
        } else {
            return supplierRepository.existsBySupplierNameIgnoreCaseAndIdNot(cleanName, currentId);
        }
    }

    @Transactional(readOnly = true)
    public boolean isDuplicateEmail(String email, Long currentId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String cleanEmail = email.trim();
        if (currentId == null) {
            return supplierRepository.existsByEmailIgnoreCase(cleanEmail);
        } else {
            return supplierRepository.existsByEmailIgnoreCaseAndIdNot(cleanEmail, currentId);
        }
    }
}