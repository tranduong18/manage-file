package com.duong.managefile.dto.request;

import lombok.Builder;

@Builder
public record PickedFileRequest(
        String googleFileId,
        String parentFolderId,
        String fileName,
        String mimeType,
        Long sizeBytes,
        boolean isFolder
) {
}
