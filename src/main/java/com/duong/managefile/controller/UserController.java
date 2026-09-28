package com.duong.managefile.controller;

import com.duong.managefile.dto.response.ApiResponse;
import com.duong.managefile.dto.response.UserDetailResponse;
import com.duong.managefile.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserDetailResponse> getUserDetail(@AuthenticationPrincipal Jwt jwt){
        UserDetailResponse data = userService.getUserDetail(jwt.getSubject());
        return ApiResponse.<UserDetailResponse>builder()
                .status("success")
                .data(data)
                .build();
    }
}
