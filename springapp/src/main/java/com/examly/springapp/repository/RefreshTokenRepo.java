package com.examly.springapp.repository;

import com.examly.springapp.model.RefreshToken;
import com.examly.springapp.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUserAccountAndRevokedFalse(UserAccount userAccount);
}
