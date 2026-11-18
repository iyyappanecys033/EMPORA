package com.examly.springapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.examly.springapp.model.UserRoleMapping;
import java.util.List;

@Repository
public interface UserRoleMappingRepo extends JpaRepository<UserRoleMapping, Long> {
    @Query("SELECT u FROM UserRoleMapping u WHERE u.user.id = :userId")
    List<UserRoleMapping> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT u FROM UserRoleMapping u WHERE u.user.id = :userId AND u.role.id = :roleId")
    UserRoleMapping findByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Query("SELECT u FROM UserRoleMapping u WHERE u.user.username = :username")
    List<UserRoleMapping> findByUserUsername(@Param("username") String username);

    @Query("SELECT m.user.id, r.roleName FROM UserRoleMapping m JOIN m.role r")
    List<Object[]> findAllUserIdAndRoleNames();
}
