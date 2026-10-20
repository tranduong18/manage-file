package com.duong.managefile.service;

import com.duong.managefile.client.GoogleTokenClient;
import com.duong.managefile.dto.response.GoogleExchangeTokenResponse;
import com.duong.managefile.entity.GoogleAccount;
import com.duong.managefile.exception.AppException;
import com.duong.managefile.exception.ErrorCode;
import com.duong.managefile.repository.GoogleAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "GOOGLE-TOKEN-SERVICE")
public class GoogleTokenService {
    @Value("${oauth.google.client-id}")
    private String clientId;
    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    private static final Duration REFRESH_BEFORE_EXPIRY  = Duration.ofMinutes(60);

    private final GoogleAccountRepository googleAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final GoogleTokenClient googleTokenClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = AppException.class)
    public String getValidAccessToken(String userId){
        GoogleAccount account = lock(userId);
        if(account.getExpiresAt().isAfter(Instant.now().plus(REFRESH_BEFORE_EXPIRY))){
            return tokenEncryptionService.decrypt(account.getAccessTokenEnc());
        }
        return refresh(account);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = AppException.class)
    public String forceRefresh(String userId, String refreshToken){
        GoogleAccount account = lock(userId);
        String currentRefreshToken = tokenEncryptionService.decrypt(account.getRefreshTokenEnc());
        if(!currentRefreshToken.equals(refreshToken)){
            return currentRefreshToken;
        }

        return refresh(account);
    }

    private GoogleAccount lock(String userId){
        return googleAccountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.GOOGLE_ACCOUNT_NOT_FOUND));
    }

    private String refresh(GoogleAccount account){
        if(account.getRefreshTokenEnc() == null){
            throw new AppException(ErrorCode.GOOGLE_REAUTH_REQUIRED);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", tokenEncryptionService.decrypt(account.getRefreshTokenEnc()));
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        GoogleExchangeTokenResponse response;
        try {
            log.info("Refreshing Google access token for user {}", account.getUser().getId());
            response = googleTokenClient.exchangeToken(form);
        } catch (HttpClientErrorException e) {
            if(e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains("invalid_grant")){
                account.setRefreshTokenEnc(null);
                account.setExpiresAt(Instant.EPOCH);
                googleAccountRepository.save(account);
                throw new AppException(ErrorCode.GOOGLE_REAUTH_REQUIRED);
            }

            log.error("Google refresh failed, status {}", e.getStatusCode());
            throw new AppException(ErrorCode.GOOGLE_REFRESH_FAILED);
        } catch (RuntimeException e) {
            throw new AppException(ErrorCode.GOOGLE_REFRESH_FAILED);
        }

        account.setAccessTokenEnc(tokenEncryptionService.encrypt(response.accessToken()));
        account.setExpiresAt(Instant.now().plusSeconds(response.expiresIn()));
        if(response.refreshToken() != null){
            account.setRefreshTokenEnc(tokenEncryptionService.encrypt(response.refreshToken()));
        }
        googleAccountRepository.save(account);
        return response.accessToken();
    }
}
