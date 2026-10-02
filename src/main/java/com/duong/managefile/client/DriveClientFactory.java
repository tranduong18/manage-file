package com.duong.managefile.client;

import com.duong.managefile.entity.GoogleAccount;
import com.duong.managefile.exception.AppException;
import com.duong.managefile.exception.ErrorCode;
import com.duong.managefile.service.TokenEncryptionService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class DriveClientFactory {
    private static final String APP_NAME = "manage-file-app";

    private final TokenEncryptionService tokenEncryptionService;

    public Drive build(GoogleAccount account) {
        if (account.getExpiresAt() != null && account.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_EXPIRED);
        }

        String rawAccessToken = tokenEncryptionService.decrypt(account.getAccessTokenEnc());

        AccessToken accessToken = new AccessToken(
                rawAccessToken,
                account.getExpiresAt() != null ? Date.from(account.getExpiresAt()) : null
        );

        GoogleCredentials credentials = GoogleCredentials.create(accessToken);

        try {
            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APP_NAME)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new AppException(ErrorCode.GOOGLE_CLIENT_BUILD_FAILED);
        }
    }
}
