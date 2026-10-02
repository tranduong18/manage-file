package com.duong.managefile.service;

import com.duong.managefile.client.DriveClientFactory;
import com.duong.managefile.common.FileSource;
import com.duong.managefile.common.FolderSource;
import com.duong.managefile.dto.request.CreateFolderRequest;
import com.duong.managefile.dto.response.FileResponse;
import com.duong.managefile.dto.response.FolderResponse;
import com.duong.managefile.dto.response.PageResponse;
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
import com.duong.managefile.repository.specification.FileSpecification;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

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

    // Create folder
    @Transactional
    public FolderResponse createFolder(String userId, CreateFolderRequest request){
        User user = getUser(userId);
        Drive drive = buildDriveClient(userId);

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

    // Upload file
    @Transactional
    public FileResponse uploadFile(String userId, MultipartFile file, String parentFolderId){
        User user = getUser(userId);
        Drive drive = buildDriveClient(userId);

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

    // Get file detail
    @Transactional(readOnly = true)
    public FileResponse getFile(String userId, String fileId){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        return fileMapper.toFileResponse(file);
    }

    // Download file
    @Transactional
    public void downloadFile(String userId, String fileId, HttpServletResponse response){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        Drive drive = buildDriveClient(userId);

        try {
            response.setContentType(file.getMimeType() != null ? file.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(file.getFileName(), StandardCharsets.UTF_8)
                    .build().toString());
            drive.files().get(file.getGoogleFileId()).executeMediaAndDownloadTo(response.getOutputStream());
        } catch (IOException e) {
            throw new AppException(ErrorCode.DRIVE_API_ERROR);
        }
    }

    // Delete file
    @Transactional
    public void deleteFile(String userId, String fileId){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        Drive drive = buildDriveClient(userId);
        File fileDrive = new File();
        fileDrive.setTrashed(true);

        try {
            drive.files().update(file.getGoogleFileId(), fileDrive).execute();
            file.setDeleted(true);
            fileRepository.save(file);
        } catch (IOException e) {
            throw new AppException(ErrorCode.DRIVE_API_ERROR);
        }
    }

    // Rename file
    @Transactional
    public FileResponse renameFile(String userId, String fileId, String newName){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        Drive drive = buildDriveClient(userId);
        File fileDrive = new File();
        fileDrive.setName(newName);

        try {
            drive.files().update(file.getGoogleFileId(), fileDrive).setFields("id, name").execute();
            file.setFileName(newName);
            fileRepository.save(file);
        } catch (IOException e) {
            log.error("Error renaming file: {}", e.getMessage());
            throw new AppException(ErrorCode.DRIVE_API_ERROR);
        }

        return fileMapper.toFileResponse(file);
    }

    // Move file to another folder
    @Transactional
    public FileResponse moveFile(String userId, String fileId, String targetFolderId){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        Drive drive = buildDriveClient(userId);

        try {
            File currentFile = drive.files().get(file.getGoogleFileId()).setFields("parents").execute();
            String preParents = String.join(",", currentFile.getParents());

            drive.files().update(file.getGoogleFileId(), null)
                    .setAddParents(targetFolderId)
                    .setRemoveParents(preParents)
                    .setFields("id, parents")
                    .execute();

            file.setParentFolderId(targetFolderId);
            fileRepository.save(file);
        } catch (IOException e) {
            throw new AppException(ErrorCode.DRIVE_API_ERROR);
        }

        return fileMapper.toFileResponse(file);
    }

    // Get folder tree
    @Transactional(readOnly = true)
    public PageResponse<FolderResponse> getFolderTree(String userId, String parentGoogleId, int page, int size){
        if(page <= 0) page = 1;
        if(size <= 0 || size > 20) size = 20;

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Folder> folderPage = (parentGoogleId == null)
                ? folderRepository.findRootFolders(userId, pageable)
                : folderRepository.findChildrenOf(userId, parentGoogleId, pageable);

        List<FolderResponse> content = folderPage.getContent().stream()
                .map(folderMapper::toFolderResponse)
                .toList();

        return PageResponse.<FolderResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(folderPage.getTotalPages())
                .totalElements(folderPage.getTotalElements())
                .content(content)
                .build();
    }

    // List files
    @Transactional(readOnly = true)
    public PageResponse<FileResponse> listFiles(String userId, String parentFolderId, String mimeType, String name, int page, int size){
        if(page <= 0) page = 1;
        if(size <= 0 || size > 20) size = 20;

        Specification<FileMetadata> fileSpecification = Specification.allOf(
                FileSpecification.belongsToUser(userId),
                FileSpecification.notDeleted(),
                FileSpecification.inFolder(parentFolderId),
                FileSpecification.hasMimeType(mimeType),
                FileSpecification.hasName(name)
        );

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<FileMetadata> filePage = fileRepository.findAll(fileSpecification, pageable);
        List<FileResponse> content = filePage.getContent().stream()
                .map(fileMapper::toFileResponse)
                .toList();

        return PageResponse.<FileResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(filePage.getTotalPages())
                .totalElements(filePage.getTotalElements())
                .content(content)
                .build();
    }


    private User getUser(String userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Drive buildDriveClient(String userId){
        GoogleAccount account = googleAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.GOOGLE_ACCOUNT_NOT_FOUND));

        return driveClientFactory.build(account);
    }
}
