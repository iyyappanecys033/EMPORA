package com.examly.springapp.dto;

import com.examly.springapp.model.ServiceCatalogItem;
import java.math.BigDecimal;

public class ServiceCatalogItemDTO {

    private Long id;
    private String itemCode;
    private String title;
    private String category;
    private String department;
    private String description;
    private BigDecimal estimatedCost;
    private Integer slaTargetHours;
    private boolean requiresManagerApproval;
    private boolean requiresSecondaryApproval;
    private boolean isActive;

    public ServiceCatalogItemDTO() {
    }

    public ServiceCatalogItemDTO(Long id, String itemCode, String title, String category,
                                 String department, String description, BigDecimal estimatedCost,
                                 Integer slaTargetHours, boolean requiresManagerApproval,
                                 boolean requiresSecondaryApproval, boolean isActive) {
        this.id = id;
        this.itemCode = itemCode;
        this.title = title;
        this.category = category;
        this.department = department;
        this.description = description;
        this.estimatedCost = estimatedCost;
        this.slaTargetHours = slaTargetHours;
        this.requiresManagerApproval = requiresManagerApproval;
        this.requiresSecondaryApproval = requiresSecondaryApproval;
        this.isActive = isActive;
    }

    public static ServiceCatalogItemDTO fromEntity(ServiceCatalogItem item) {
        if (item == null) {
            return null;
        }
        return new ServiceCatalogItemDTO(
                item.getId(),
                item.getItemCode(),
                item.getTitle(),
                item.getCategory(),
                item.getDepartment(),
                item.getDescription(),
                item.getEstimatedCost(),
                item.getSlaTargetHours(),
                item.getRequiresManagerApproval() != null && item.getRequiresManagerApproval(),
                item.getRequiresSecondaryApproval() != null && item.getRequiresSecondaryApproval(),
                item.isActive()
        );
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
        this.estimatedCost = estimatedCost;
    }

    public Integer getSlaTargetHours() {
        return slaTargetHours;
    }

    public void setSlaTargetHours(Integer slaTargetHours) {
        this.slaTargetHours = slaTargetHours;
    }

    public boolean isRequiresManagerApproval() {
        return requiresManagerApproval;
    }

    public void setRequiresManagerApproval(boolean requiresManagerApproval) {
        this.requiresManagerApproval = requiresManagerApproval;
    }

    public boolean isRequiresSecondaryApproval() {
        return requiresSecondaryApproval;
    }

    public void setRequiresSecondaryApproval(boolean requiresSecondaryApproval) {
        this.requiresSecondaryApproval = requiresSecondaryApproval;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
