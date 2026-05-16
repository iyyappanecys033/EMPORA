package com.examly.springapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.examly.springapp.model.Profile;
import java.util.List;

@Repository
public interface ProfileRepo extends JpaRepository<Profile, Long> {
    @Query("SELECT p FROM Profile p WHERE p.firstName = :name")
    List<Profile> findByName(@Param("name") String name);
    
    @Query("SELECT p FROM Profile p WHERE p.firstName = :name AND p.address = :address")
    List<Profile> findByNameAndAddress(@Param("name") String name, @Param("address") String address);
}
