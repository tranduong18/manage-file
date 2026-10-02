package com.duong.managefile.repository;

import com.duong.managefile.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    Optional<Folder> findByUser_IdAndGoogleFolderId(String userId, String googleFolderId);

    List<Folder> findByUser_IdAndParentGoogleId(String userId, String parentGoogleId);

    List<Folder> findByUser_IdAndParentGoogleIdIsNull(String userId);
}
