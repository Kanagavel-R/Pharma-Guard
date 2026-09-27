package com.pharmaguard.controller;

import com.pharmaguard.entity.Supplier;
import com.pharmaguard.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierApiController {

    private final SupplierService supplierService;

    public SupplierApiController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(supplierService.searchSuppliers(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSupplierById(@PathVariable Long id) {
        try {
            Supplier supplier = supplierService.getSupplierById(id);
            return ResponseEntity.ok(supplier);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Supplier not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
    }

    @PostMapping
    public ResponseEntity<?> createSupplier(@Valid @RequestBody Supplier supplier) {
        if (supplierService.isDuplicateSupplierName(supplier.getSupplierName(), null)) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Supplier already exists with this name.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }

        if (supplier.getEmail() != null && !supplier.getEmail().trim().isEmpty()
                && supplierService.isDuplicateEmail(supplier.getEmail(), null)) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "A supplier with this email already exists.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }

        Supplier saved = supplierService.saveSupplier(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupplier(@PathVariable Long id, @Valid @RequestBody Supplier supplier) {
        try {
            Supplier existing = supplierService.getSupplierById(id);
            supplier.setId(existing.getId());

            if (supplierService.isDuplicateSupplierName(supplier.getSupplierName(), id)) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Supplier already exists with this name.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
            }

            if (supplier.getEmail() != null && !supplier.getEmail().trim().isEmpty()
                    && supplierService.isDuplicateEmail(supplier.getEmail(), id)) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "A supplier with this email already exists.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
            }

            Supplier updated = supplierService.saveSupplier(supplier);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Supplier not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplier(@PathVariable Long id) {
        try {
            supplierService.deleteSupplier(id);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Supplier deleted successfully.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Unable to delete supplier");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
    }
}
