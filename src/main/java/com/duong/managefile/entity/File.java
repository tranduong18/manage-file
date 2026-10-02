package com.duong.managefile.entity;

import com.duong.managefile.common.FileSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "google_file_id", nullable = false)
    private String googleFileId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    private String mimeType;
    private Long sizeBytes;
    private String parentFolderId;

    @Column(columnDefinition = "TEXT")
    private String thumbnailLink;

    @Column(columnDefinition = "text")
    private String webViewLink;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FileSource source = FileSource.UPLOADED;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant uploadedAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
