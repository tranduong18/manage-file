package com.duong.managefile.repository;

import com.duong.managefile.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    @Query("select f from Folder f where f.user.id = :userId and f.googleFolderId = :googleFolderId")
    Optional<Folder> findByUserAndGoogleFolderId(@Param("userId") String userId, @Param("googleFolderId") String googleFolderId);

    @Query("select f from Folder f where f.user.id = :userId and f.parentGoogleId = :parentGoogleId")
    List<Folder> findChildrenOf(@Param("userId") String userId, @Param("parentGoogleId") String parentGoogleId);

    @Query("select f from Folder f where f.user.id = :userId and f.parentGoogleId is null")
    List<Folder> findRootFolders(@Param("userId") String userId);
}
