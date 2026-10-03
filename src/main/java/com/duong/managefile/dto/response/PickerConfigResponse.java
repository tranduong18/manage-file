package com.duong.managefile.dto.response;

import lombok.Builder;

@Builder
public record PickerConfigResponse(
        String accessToken,
        String apiKey,
        String appId,
        String scope
) {
}
