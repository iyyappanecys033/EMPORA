package com.examly.springapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.examly.springapp.model.Profile;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepo extends JpaRepository<Profile, Long> {

    @Query("SELECT p FROM Profile p WHERE p.firstName = :name")
    List<Profile> findByName(@Param("name") String name);
    
    @Query("SELECT p FROM Profile p WHERE p.firstName = :name AND p.address = :address")
    List<Profile> findByNameAndAddress(@Param("name") String name, @Param("address") String address);

    Optional<Profile> findByEmployeeId(String employeeId);

    Optional<Profile> findByUserId(Long userId);

    @Query("SELECT p FROM Profile p WHERE p.user.username = :username")
    Optional<Profile> findByUserUsername(@Param("username") String username);

    List<Profile> findByDepartment(String department);

    Page<Profile> findByDepartment(String department, Pageable pageable);

    @Query("SELECT p FROM Profile p WHERE " +
           "(:dept IS NULL OR :dept = '' OR LOWER(p.department) = LOWER(:dept)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.employeeId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.designation) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(p.user IS NOT NULL AND LOWER(p.user.username) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(p.user IS NOT NULL AND LOWER(p.user.email) LIKE LOWER(CONCAT('%', :query, '%'))))")
    Page<Profile> searchProfiles(@Param("query") String query, @Param("dept") String dept, Pageable pageable);
}
