package com.duong.managefile.security;

import com.duong.managefile.entity.GoogleAccount;
import com.duong.managefile.entity.User;
import com.duong.managefile.repository.GoogleAccountRepository;
import com.duong.managefile.repository.UserRepository;
import com.duong.managefile.service.TokenEncryptionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CustomOauth2SuccessHandler implements AuthenticationSuccessHandler {
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserRepository userRepository;
    private final GoogleAccountRepository googleAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final JwtService jwtService;

    @Value("${app.frontend.redirect-url:http://localhost:3000/oauth2/callback}")
    private String frontendRedirectUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauth2Token.getPrincipal();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauth2Token.getAuthorizedClientRegistrationId(),
                oauth2Token.getName()
        );

        if(authorizedClient == null) {
            throw new RuntimeException("Authorized client is null");
        }

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl  = oAuth2User.getAttribute("picture");
        String scopes = String.join(" ", accessToken.getScopes());

        User user = userRepository.findByEmail(email)
                .orElseGet(() ->
                        User.builder().email(email).build()
                );
        user.setName(name);
        user.setAvatarUrl(avatarUrl);
        User savedUser = userRepository.save(user);

        GoogleAccount googleAccount = googleAccountRepository.findByUserId(savedUser.getId())
                .orElseGet(() -> GoogleAccount.builder().user(savedUser).build());

        googleAccount.setAccessTokenEnc(tokenEncryptionService.encrypt(accessToken.getTokenValue()));

        if (refreshToken != null) {
            googleAccount.setRefreshTokenEnc(tokenEncryptionService.encrypt(refreshToken.getTokenValue()));
        }

        googleAccount.setExpiresAt(accessToken.getExpiresAt() != null
                ? accessToken.getExpiresAt()
                : Instant.now().plusSeconds(3600));
        googleAccount.setScopes(scopes);
        googleAccountRepository.save(googleAccount);

        String systemJwt = jwtService.generateAccessToken(email);
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", systemJwt)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
