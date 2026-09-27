package com.pharmaguard.controller;

import com.pharmaguard.entity.Supplier;
import com.pharmaguard.service.SupplierService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private static final Logger log = LoggerFactory.getLogger(SupplierController.class);
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String listSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String error,
            Model model) {

        model.addAttribute("suppliers", supplierService.searchSuppliers(keyword));
        model.addAttribute("keyword", keyword);
        if (success != null) {
            model.addAttribute("successMessage", success);
        }
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }

        return "suppliers";
    }

    @GetMapping("/add")
    public String addSupplierForm(Model model) {
        if (!model.containsAttribute("supplier")) {
            model.addAttribute("supplier", new Supplier());
        }
        return "add-supplier";
    }

    @PostMapping("/save")
    public String saveSupplier(
            @Valid @ModelAttribute("supplier") Supplier supplier,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Request received to save supplier: {}", supplier.getSupplierName());

        if (bindingResult.hasErrors()) {
            log.warn("Validation failed for save supplier: {}", bindingResult.getAllErrors());
            return "add-supplier";
        }

        if (supplierService.isDuplicateSupplierName(supplier.getSupplierName(), null)) {
            log.warn("Duplicate supplier name attempt: {}", supplier.getSupplierName());
            bindingResult.rejectValue("supplierName", "duplicate", "Supplier already exists with this name.");
            return "add-supplier";
        }

        if (supplier.getEmail() != null && !supplier.getEmail().trim().isEmpty()
                && supplierService.isDuplicateEmail(supplier.getEmail(), null)) {
            log.warn("Duplicate supplier email attempt: {}", supplier.getEmail());
            bindingResult.rejectValue("email", "duplicate", "A supplier with this email already exists.");
            return "add-supplier";
        }

        try {
            Supplier saved = supplierService.saveSupplier(supplier);
            log.info("Supplier created successfully with ID: {}", saved.getId());
            redirectAttributes.addAttribute("success", "Supplier added successfully.");
            return "redirect:/suppliers";
        } catch (Exception e) {
            log.error("Failed to save supplier: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Unable to save the supplier right now. Please try again.");
            return "add-supplier";
        }
    }

    @GetMapping("/edit/{id}")
    public String editSupplier(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute("supplier", supplierService.getSupplierById(id));
            return "edit-supplier";
        } catch (Exception e) {
            log.error("Supplier not found for edit with ID {}: {}", id, e.getMessage());
            redirectAttributes.addAttribute("error", "Supplier not found.");
            return "redirect:/suppliers";
        }
    }

    @PostMapping("/update")
    public String updateSupplier(
            @Valid @ModelAttribute("supplier") Supplier supplier,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Request received to update supplier ID: {}", supplier.getId());

        if (bindingResult.hasErrors()) {
            log.warn("Validation failed for update supplier ID {}: {}", supplier.getId(), bindingResult.getAllErrors());
            return "edit-supplier";
        }

        if (supplierService.isDuplicateSupplierName(supplier.getSupplierName(), supplier.getId())) {
            log.warn("Duplicate supplier name attempt on update: {}", supplier.getSupplierName());
            bindingResult.rejectValue("supplierName", "duplicate", "Supplier already exists with this name.");
            return "edit-supplier";
        }

        if (supplier.getEmail() != null && !supplier.getEmail().trim().isEmpty()
                && supplierService.isDuplicateEmail(supplier.getEmail(), supplier.getId())) {
            log.warn("Duplicate supplier email attempt on update: {}", supplier.getEmail());
            bindingResult.rejectValue("email", "duplicate", "A supplier with this email already exists.");
            return "edit-supplier";
        }

        try {
            Supplier updated = supplierService.saveSupplier(supplier);
            log.info("Supplier updated successfully with ID: {}", updated.getId());
            redirectAttributes.addAttribute("success", "Supplier updated successfully.");
            return "redirect:/suppliers";
        } catch (Exception e) {
            log.error("Failed to update supplier ID {}: {}", supplier.getId(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Unable to update the supplier right now. Please try again.");
            return "edit-supplier";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteSupplier(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            supplierService.deleteSupplier(id);
            log.info("Supplier deleted successfully with ID: {}", id);
            redirectAttributes.addAttribute("success", "Supplier deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete supplier ID {}: {}", id, e.getMessage());
            redirectAttributes.addAttribute("error", "Unable to delete supplier.");
        }
        return "redirect:/suppliers";
    }
}