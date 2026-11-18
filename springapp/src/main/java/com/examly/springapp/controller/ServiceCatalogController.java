package com.examly.springapp.controller;

import com.examly.springapp.dto.ServiceCatalogItemDTO;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.ServiceCatalogItem;
import com.examly.springapp.repository.ServiceCatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog")
public class ServiceCatalogController {

    private final ServiceCatalogRepository catalogRepository;

    @Autowired
    public ServiceCatalogController(ServiceCatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @GetMapping
    public ResponseEntity<Page<ServiceCatalogItemDTO>> listActiveCatalogItems(
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, Sort.by("id").ascending());

        Page<ServiceCatalogItem> items;
        if (department != null && !department.isBlank()) {
            items = catalogRepository.findByDepartmentAndIsActiveTrue(department.trim(), pageable);
        } else if (category != null && !category.isBlank()) {
            items = catalogRepository.findByCategoryAndIsActiveTrue(category.trim(), pageable);
        } else {
            items = catalogRepository.findByIsActiveTrue(pageable);
        }

        Page<ServiceCatalogItemDTO> dtoPage = items.map(ServiceCatalogItemDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCatalogItemDTO> getCatalogItemById(@PathVariable("id") Long id) {
        ServiceCatalogItem item = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service catalog item not found with ID: " + id));
        return ResponseEntity.ok(ServiceCatalogItemDTO.fromEntity(item));
    }

    @GetMapping("/code/{itemCode}")
    public ResponseEntity<ServiceCatalogItemDTO> getCatalogItemByCode(@PathVariable("itemCode") String itemCode) {
        ServiceCatalogItem item = catalogRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Service catalog item not found with code: " + itemCode));
        return ResponseEntity.ok(ServiceCatalogItemDTO.fromEntity(item));
    }
}

