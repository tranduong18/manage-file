package com.duong.managefile.controller;

import com.duong.managefile.dto.request.GoogleLoginRequest;
import com.duong.managefile.dto.response.ApiResponse;
import com.duong.managefile.dto.response.LoginResponse;
import com.duong.managefile.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    private final AuthService authService;

    @PostMapping("/google")
    public ApiResponse<LoginResponse> loginGoogle(@Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response){
        LoginResponse data = authService.loginGoogle(request.code());
        String refreshToken = data.refreshToken();

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .maxAge(Duration.ofDays(14))
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.<LoginResponse>builder()
                .status("success")
                .data(data)
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(@CookieValue(REFRESH_COOKIE_NAME) String refreshToken){
        LoginResponse data = authService.refreshToken(refreshToken);
        return ApiResponse.<LoginResponse>builder()
                .status("success")
                .data(data)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CookieValue(REFRESH_COOKIE_NAME) String refreshToken, HttpServletResponse response){
        authService.logout(refreshToken);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .maxAge(Duration.ZERO)
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.<Void>builder()
                .status("success")
                .message("Logout successfully")
                .build();
    }
}
