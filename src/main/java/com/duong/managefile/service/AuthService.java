package com.duong.managefile.service;

import com.duong.managefile.client.GoogleTokenClient;
import com.duong.managefile.client.GoogleUserInfoClient;
import com.duong.managefile.common.TokenType;
import com.duong.managefile.config.GoogleOAuthProperties;
import com.duong.managefile.dto.response.GoogleExchangeTokenResponse;
import com.duong.managefile.dto.response.GoogleUserInfoResponse;
import com.duong.managefile.dto.response.LoginResponse;
import com.duong.managefile.entity.GoogleAccount;
import com.duong.managefile.entity.User;
import com.duong.managefile.exception.AppException;
import com.duong.managefile.exception.ErrorCode;
import com.duong.managefile.repository.GoogleAccountRepository;
import com.duong.managefile.repository.UserRepository;
import com.duong.managefile.security.JwtService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import java.text.ParseException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTH-SERVICE")
public class AuthService {
    private final GoogleTokenClient googleTokenClient;
    private final GoogleUserInfoClient googleUserInfoClient;
    private final GoogleOAuthProperties props;
    private final TokenEncryptionService tokenEncryptionService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final GoogleAccountRepository googleAccountRepository;

    @Transactional
    public LoginResponse loginGoogle(String code){
        GoogleExchangeTokenResponse token;
        try {
            token = googleTokenClient.exchangeToken(buildTokenRequest(code));
        } catch (HttpClientErrorException e) {
            // invalid_grant: code sai, hết hạn (sống vài phút), hoặc đã dùng rồi
            log.warn("Google exchange code failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        GoogleUserInfoResponse info = googleUserInfoClient.getUserInfo("Bearer " + token.accessToken());

        if (!info.emailVerified()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(info.email())
                .orElseGet(() -> User.builder().email(info.email()).build());
        user.setName(info.name());
        user.setAvatarUrl(info.picture());
        User savedUser = userRepository.save(user);

        GoogleAccount googleAccount = googleAccountRepository.findByUserId(savedUser.getId())
                .orElseGet(() -> GoogleAccount.builder().user(savedUser).build());

        googleAccount.setAccessTokenEnc(tokenEncryptionService.encrypt(token.accessToken()));

        // Google chỉ trả refresh token ở lần đầu consent;
        if (token.refreshToken() != null) {
            googleAccount.setRefreshTokenEnc(tokenEncryptionService.encrypt(token.refreshToken()));
        }

        googleAccount.setExpiresAt(Instant.now().plusSeconds(
                token.expiresIn() != null ? token.expiresIn() : 3600));
        googleAccount.setScopes(token.scope() != null ? token.scope() : props.scope());
        googleAccountRepository.save(googleAccount);

        String access = jwtService.generateAccessToken(savedUser.getId(), info.email());
        String refresh = jwtService.generateRefreshToken(savedUser.getId(), savedUser.getTokenVersion());
        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken){
        try {
            SignedJWT signedJWT = jwtService.validateToken(refreshToken, TokenType.REFRESH);
            String userId = signedJWT.getJWTClaimsSet().getSubject();
            String email = signedJWT.getJWTClaimsSet().getStringClaim("email");
            Integer tokenVersion = signedJWT.getJWTClaimsSet().getIntegerClaim("version");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
            if(user.getTokenVersion() != tokenVersion){
                log.error("Refresh token failed: token version mismatch");
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            String accessToken = jwtService.generateAccessToken(userId, email);
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .build();
        } catch (ParseException e){
            log.error("Refresh token failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Transactional
    public void logout(String refreshToken){
        try {
            SignedJWT signedJWT = jwtService.validateToken(refreshToken, TokenType.REFRESH);
            String userId = signedJWT.getJWTClaimsSet().getSubject();

            User user = userRepository.findUserById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);

            log.info("User {} logout", userId);
        } catch (ParseException e) {
            log.error("Logout failed: {}", e.getMessage());
        }
    }

    private MultiValueMap<String, String> buildTokenRequest(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        form.add("redirect_uri", props.redirectUri());
        form.add("grant_type", "authorization_code");
        return form;
    }
}
