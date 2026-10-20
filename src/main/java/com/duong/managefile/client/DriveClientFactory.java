package com.duong.managefile.client;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
public class DriveClientFactory {
    private static final String APP_NAME = "manage-file-app";

    private final HttpTransport transport;
    private final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

    public DriveClientFactory() {
        try {
            this.transport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Cannot init Google HTTP transport", e);
        }
    }

    public Drive build(String accessToken) {
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        return new Drive.Builder(transport, gsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APP_NAME)
                .build();
    }
}
