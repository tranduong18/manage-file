package com.duong.managefile.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
    String accessToken,
    String tokenType) {
}
