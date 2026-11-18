package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Service Catalog Item domain entity representing standard IT/HR/Admin services.
 * Maps explicitly to the 'service_catalog' database table as defined in Flyway V2.
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "service_catalog")
public class ServiceCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "department", nullable = false, length = 50)
    private String department;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "sla_target_hours", nullable = false)
    private Integer slaTargetHours = 24;

    @Column(name = "requires_manager_approval", nullable = false)
    private Boolean requiresManagerApproval = true;

    @Column(name = "requires_secondary_approval", nullable = false)
    private Boolean requiresSecondaryApproval = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public ServiceCatalogItem() {
    }

    public ServiceCatalogItem(String itemCode, String title, String category, String department, 
                              String description, BigDecimal estimatedCost, Integer slaTargetHours, 
                              Boolean requiresManagerApproval, Boolean requiresSecondaryApproval, Boolean isActive) {
        this.itemCode = itemCode;
        this.title = title;
        this.category = category;
        this.department = department;
        this.description = description;
        this.estimatedCost = estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
        this.slaTargetHours = slaTargetHours != null ? slaTargetHours : 24;
        this.requiresManagerApproval = requiresManagerApproval != null ? requiresManagerApproval : true;
        this.requiresSecondaryApproval = requiresSecondaryApproval != null ? requiresSecondaryApproval : false;
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
    }

    public Integer getSlaTargetHours() {
        return slaTargetHours;
    }

    public void setSlaTargetHours(Integer slaTargetHours) {
        this.slaTargetHours = slaTargetHours;
    }

    public Boolean getRequiresManagerApproval() {
        return requiresManagerApproval;
    }

    public void setRequiresManagerApproval(Boolean requiresManagerApproval) {
        this.requiresManagerApproval = requiresManagerApproval;
    }

    public Boolean getRequiresSecondaryApproval() {
        return requiresSecondaryApproval;
    }

    public void setRequiresSecondaryApproval(Boolean requiresSecondaryApproval) {
        this.requiresSecondaryApproval = requiresSecondaryApproval;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive != null && isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}

