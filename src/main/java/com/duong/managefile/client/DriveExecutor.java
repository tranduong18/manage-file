package com.duong.managefile.client;

import com.duong.managefile.exception.AppException;
import com.duong.managefile.exception.ErrorCode;
import com.duong.managefile.service.GoogleTokenService;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class DriveExecutor {
    private final GoogleTokenService tokenService;
    private final DriveClientFactory driveClientFactory;

    @FunctionalInterface
    public interface DriveCall<T>{
        T run(Drive drive) throws IOException;
    }

    public <T> T execute(String userId, DriveCall<T> call){
        String token = tokenService.getValidAccessToken(userId);
        try {
            return call.run(driveClientFactory.build(token));
        } catch (GoogleJsonResponseException e){
            if(e.getStatusCode() != 401){
                throw mapDriveError(e);
            }
        } catch (IOException e) {
            throw new AppException(ErrorCode.DRIVE_API_ERROR);
        }

        // 401: refresh một lần, retry đúng một lần
        String fresh = tokenService.forceRefresh(userId, token);
        try {
            return call.run(driveClientFactory.build(fresh));
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401) throw new AppException(ErrorCode.GOOGLE_REAUTH_REQUIRED);
            throw mapDriveError(e);
        } catch (IOException e) {
            throw new AppException(ErrorCode.DRIVE_API_ERROR);
        }
    }

    private AppException mapDriveError(GoogleJsonResponseException e){
        if(e.getStatusCode() == 404){
            return new AppException(ErrorCode.FILE_NOT_FOUND);
        }
        return new AppException(ErrorCode.DRIVE_API_ERROR);
    }
}
