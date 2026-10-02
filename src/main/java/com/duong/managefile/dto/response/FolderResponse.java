package com.duong.managefile.dto.response;

import lombok.Builder;

@Builder
public record FolderResponse (
    String id,
    String googleFolderId,
    String name,
    String parentGoogleId,
    String source
){}
