package com.examly.springapp.dto;

import com.examly.springapp.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateTicketRequestDTO {

    @NotNull(message = "Catalog item ID is required")
    private Long catalogItemId;

    @NotBlank(message = "Department is required")
    @Size(max = 50, message = "Department must not exceed 50 characters")
    private String department;

    @NotNull(message = "Priority is required")
    private TicketPriority priority;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    public CreateTicketRequestDTO() {
    }

    public CreateTicketRequestDTO(Long catalogItemId, String department, TicketPriority priority, String description) {
        this.catalogItemId = catalogItemId;
        this.department = department;
        this.priority = priority;
        this.description = description;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

