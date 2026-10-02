package com.duong.managefile.service;

import com.duong.managefile.client.DriveClientFactory;
import com.duong.managefile.common.FileSource;
import com.duong.managefile.common.FolderSource;
import com.duong.managefile.dto.request.CreateFolderRequest;
import com.duong.managefile.dto.response.FileResponse;
import com.duong.managefile.dto.response.FolderResponse;
import com.duong.managefile.entity.FileMetadata;
import com.duong.managefile.entity.Folder;
import com.duong.managefile.entity.GoogleAccount;
import com.duong.managefile.entity.User;
import com.duong.managefile.exception.AppException;
import com.duong.managefile.exception.ErrorCode;
import com.duong.managefile.mapper.FileMapper;
import com.duong.managefile.mapper.FolderMapper;
import com.duong.managefile.repository.FileRepository;
import com.duong.managefile.repository.FolderRepository;
import com.duong.managefile.repository.GoogleAccountRepository;
import com.duong.managefile.repository.UserRepository;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DRIVE-SERVICE")
public class DriveService {
    private final DriveClientFactory driveClientFactory;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final GoogleAccountRepository googleAccountRepository;
    private final FolderMapper folderMapper;
    private final FileMapper fileMapper;

    @Transactional
    public FolderResponse createFolder(String userId, CreateFolderRequest request){
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        GoogleAccount account = getGoogleAccount(user);
        Drive drive = driveClientFactory.build(account);

        File metadata = new File();
        metadata.setName(request.name());
        metadata.setMimeType("application/vnd.google-apps.folder");
        if(request.parentGoogleId() != null){
            metadata.setParents(Collections.singletonList(request.parentGoogleId()));
        }

        File createdFile;
        try {
            createdFile = drive.files().create(metadata)
                    .setFields("id, name, parents")
                    .execute();
        } catch (IOException e) {
            throw new AppException(ErrorCode.DRIVE_API_ERROR);
        }

        Folder folder = Folder.builder()
                .user(user)
                .googleFolderId(createdFile.getId())
                .name(createdFile.getName())
                .parentGoogleId(request.parentGoogleId())
                .source(FolderSource.CREATED)
                .build();
        folderRepository.save(folder);

        return folderMapper.toFolderResponse(folder);
    }

    @Transactional
    public FileResponse uploadFile(String userId, MultipartFile file, String parentFolderId){
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        GoogleAccount account = getGoogleAccount(user);
        Drive drive = driveClientFactory.build(account);

        File metadata = new File();
        metadata.setName(file.getOriginalFilename());
        if(parentFolderId != null){
            metadata.setParents(Collections.singletonList(parentFolderId));
        }

        File uploadedFile;
        try(var inputStream = file.getInputStream()) {
            InputStreamContent mediaContent = new InputStreamContent(file.getContentType(), inputStream);
            mediaContent.setLength(file.getSize());

            uploadedFile = drive.files().create(metadata, mediaContent)
                    .setFields("id, name, mimeType, size, parents, thumbnailLink, webViewLink")
                    .execute();
        } catch (IOException e){
            throw new AppException(ErrorCode.DRIVE_API_ERROR);
        }

        FileMetadata fileMetadata = FileMetadata.builder()
                .user(user)
                .googleFileId(uploadedFile.getId())
                .fileName(uploadedFile.getName())
                .mimeType(uploadedFile.getMimeType())
                .sizeBytes(uploadedFile.getSize())
                .parentFolderId(parentFolderId)
                .thumbnailLink(uploadedFile.getThumbnailLink())
                .webViewLink(uploadedFile.getWebViewLink())
                .source(FileSource.UPLOADED)
                .build();
        fileRepository.save(fileMetadata);

        return fileMapper.toFileResponse(fileMetadata);
    }

    private GoogleAccount getGoogleAccount(User user){
        return googleAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.GOOGLE_ACCOUNT_NOT_FOUND));
    }
}
