package com.duong.managefile.entity;

import com.duong.managefile.common.FolderSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "google_folder_id", nullable = false)
    private String googleFolderId;

    @Column(nullable = false)
    private String name;

    private String parentGoogleId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FolderSource source = FolderSource.CREATED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
