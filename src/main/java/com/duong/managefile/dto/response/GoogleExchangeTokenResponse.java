package com.duong.managefile.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleExchangeTokenResponse(
        String accessToken,
        String refreshToken,
        String idToken,
        Long expiresIn,
        String scope,
        String tokenType
) {}