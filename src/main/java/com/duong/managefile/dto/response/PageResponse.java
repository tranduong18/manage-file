package com.duong.managefile.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PageResponse<T>(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalElements,
        List<T> content
) {
}
