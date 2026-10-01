package com.duong.managefile.controller;

import com.duong.managefile.dto.request.GoogleLoginRequest;
import com.duong.managefile.dto.response.ApiResponse;
import com.duong.managefile.dto.response.LoginResponse;
import com.duong.managefile.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/google")
    public ApiResponse<LoginResponse> loginGoogle(@Valid @RequestBody GoogleLoginRequest request){
        LoginResponse data = authService.loginGoogle(request.code());

        return ApiResponse.<LoginResponse>builder()
                .status("success")
                .data(data)
                .build();
    }
}
