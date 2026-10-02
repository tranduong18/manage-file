package com.duong.managefile.repository;

import com.duong.managefile.entity.GoogleAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleAccountRepository extends JpaRepository<GoogleAccount, String> {
    @Query("select g from GoogleAccount g where g.user.email = :email")
    Optional<GoogleAccount> findByUserEmail(@Param("email") String email);

    @Query("select g from GoogleAccount g where g.user.id = :userId")
    Optional<GoogleAccount> findByUserId(@Param("userId") String userId);
}
