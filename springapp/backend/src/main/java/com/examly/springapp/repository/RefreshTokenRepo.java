package com.examly.springapp.repository;

import com.examly.springapp.model.RefreshToken;
import com.examly.springapp.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    @Query("SELECT r FROM RefreshToken r JOIN FETCH r.userAccount WHERE r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findByUserAccountAndRevokedFalse(UserAccount userAccount);
}
