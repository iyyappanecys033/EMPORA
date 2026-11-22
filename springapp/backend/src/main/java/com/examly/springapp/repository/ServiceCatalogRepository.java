package com.examly.springapp.repository;

import com.examly.springapp.model.ServiceCatalogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ServiceCatalogItem entity.
 */
@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogItem, Long> {

    Optional<ServiceCatalogItem> findByItemCode(String itemCode);

    boolean existsByItemCode(String itemCode);

    List<ServiceCatalogItem> findByIsActiveTrue();

    Page<ServiceCatalogItem> findByIsActiveTrue(Pageable pageable);

    List<ServiceCatalogItem> findByDepartmentAndIsActiveTrue(String department);

    Page<ServiceCatalogItem> findByDepartmentAndIsActiveTrue(String department, Pageable pageable);

    List<ServiceCatalogItem> findByCategoryAndIsActiveTrue(String category);

    Page<ServiceCatalogItem> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
}

