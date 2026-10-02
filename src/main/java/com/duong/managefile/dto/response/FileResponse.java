package com.duong.managefile.dto.response;

import lombok.Builder;

@Builder
public record FileResponse(
        String id,
        String googleFileId,
        String fileName,
        String mimeType,
        Long sizeBytes,
        String parentFolderId,
        String thumbnailLink,
        String webViewLink,
        String source,
        String uploadedAt
) {
}
