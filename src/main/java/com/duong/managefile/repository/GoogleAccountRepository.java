package com.duong.managefile.repository;

import com.duong.managefile.entity.GoogleAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleAccountRepository extends JpaRepository<GoogleAccount, String> {
    Optional<GoogleAccount> findByUserEmail(String email);
    Optional<GoogleAccount> findByUserId(String userId);
}
