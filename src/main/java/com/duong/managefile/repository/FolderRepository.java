package com.duong.managefile.repository;

import com.duong.managefile.entity.Folder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    @Query("select f from Folder f where f.user.id = :userId and f.googleFolderId = :googleFolderId")
    Optional<Folder> findByUserAndGoogleFolderId(@Param("userId") String userId, @Param("googleFolderId") String googleFolderId);

    @Query("select f from Folder f where f.user.id = :userId and f.parentGoogleId = :parentGoogleId")
    Page<Folder> findChildrenOf(@Param("userId") String userId,
                                @Param("parentGoogleId") String parentGoogleId,
                                Pageable pageable);

    @Query("select f from Folder f where f.user.id = :userId and f.parentGoogleId is null")
    Page<Folder> findRootFolders(@Param("userId") String userId, Pageable pageable);
}