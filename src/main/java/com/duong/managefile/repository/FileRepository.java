package com.duong.managefile.repository;

import com.duong.managefile.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, String>, JpaSpecificationExecutor<File> {
    Optional<File> findByUser_IdAndGoogleFileId(String userId, String googleFileId);

    Optional<File> findByIdAndUser_Id(String id, String userId);
}
