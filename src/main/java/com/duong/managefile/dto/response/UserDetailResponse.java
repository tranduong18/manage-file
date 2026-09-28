package com.duong.managefile.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    String id;
    String email;
    String name;
    String avatarUrl;
}
