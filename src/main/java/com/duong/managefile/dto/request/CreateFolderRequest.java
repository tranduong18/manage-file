package com.duong.managefile.dto.request;

public record CreateFolderRequest(
        String name,
        String parentGoogleId
) {
}
