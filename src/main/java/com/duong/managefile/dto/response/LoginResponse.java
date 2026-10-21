package com.duong.managefile.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType
) {
}
