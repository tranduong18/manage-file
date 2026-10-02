package com.duong.managefile.repository;

import com.duong.managefile.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, String>, JpaSpecificationExecutor<FileMetadata> {
    @Query("select f from FileMetadata f where f.user.id = :userId and f.googleFileId = :googleFileId")
    Optional<FileMetadata> findByUserAndGoogleFileId(@Param("userId") String userId, @Param("googleFileId") String googleFileId);

    @Query("select f from FileMetadata f where f.id = :id and f.user.id = :userId")
    Optional<FileMetadata> findByIdAndUser(@Param("id") String id, @Param("userId") String userId);
}
