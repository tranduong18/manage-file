package com.duong.managefile.service;

import com.duong.managefile.client.DriveExecutor;
import com.duong.managefile.common.FileSource;
import com.duong.managefile.common.FolderSource;
import com.duong.managefile.dto.request.CreateFolderRequest;
import com.duong.managefile.dto.request.PickedFileRequest;
import com.duong.managefile.dto.response.FileResponse;
import com.duong.managefile.dto.response.FolderResponse;
import com.duong.managefile.dto.response.PageResponse;
import com.duong.managefile.dto.response.PickerConfigResponse;
import com.duong.managefile.entity.FileMetadata;
import com.duong.managefile.entity.Folder;
import com.duong.managefile.entity.User;
import com.duong.managefile.exception.AppException;
import com.duong.managefile.exception.ErrorCode;
import com.duong.managefile.mapper.FileMapper;
import com.duong.managefile.mapper.FolderMapper;
import com.duong.managefile.repository.FileRepository;
import com.duong.managefile.repository.FolderRepository;
import com.duong.managefile.repository.UserRepository;
import com.duong.managefile.repository.specification.FileSpecification;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.model.File;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DRIVE-SERVICE")
public class DriveService {
    @Value("${oauth.google.picker.api-key}")
    private String pickerApiKey;

    @Value("${oauth.google.picker.app-id}")
    private String pickerAppId;

    private static final String DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file";

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final FolderMapper folderMapper;
    private final FileMapper fileMapper;
    private final DriveExecutor driveExecutor;
    private final GoogleTokenService googleTokenService;

    // Create folder
    @Transactional
    public FolderResponse createFolder(String userId, CreateFolderRequest request){
        User user = getUser(userId);

        File metadata = new File();
        metadata.setName(request.name());
        metadata.setMimeType("application/vnd.google-apps.folder");
        if(request.parentGoogleId() != null){
            metadata.setParents(Collections.singletonList(request.parentGoogleId()));
        }

        File createdFile = driveExecutor.execute(userId, drive ->
                drive.files().create(metadata).setFields("id, name, parents").execute()
        );

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

        File metadata = new File();
        metadata.setName(file.getOriginalFilename());
        if(parentFolderId != null){
            metadata.setParents(Collections.singletonList(parentFolderId));
        }

        File uploadedFile = driveExecutor.execute(userId, drive -> {
            try (var in = file.getInputStream()) {
                InputStreamContent media = new InputStreamContent(file.getContentType(), in);
                media.setLength(file.getSize());
                return drive.files().create(metadata, media)
                        .setFields("id, name, mimeType, size, parents, thumbnailLink, webViewLink")
                        .execute();
            }
        });

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
    public void downloadFile(String userId, String fileId, HttpServletResponse response){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        driveExecutor.execute(userId, drive -> {
            response.setContentType(file.getMimeType() != null
                    ? file.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(file.getFileName(), StandardCharsets.UTF_8).build().toString());
            drive.files().get(file.getGoogleFileId())
                    .executeMediaAndDownloadTo(response.getOutputStream());
            return null;
        });
    }

    // Delete file
    @Transactional
    public void deleteFile(String userId, String fileId){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        File fileDrive = new File();
        fileDrive.setTrashed(true);

        driveExecutor.execute(userId, drive ->
                drive.files().update(file.getGoogleFileId(), fileDrive).execute());
        file.setDeleted(true);
        fileRepository.save(file);
    }

    // Rename file
    @Transactional
    public FileResponse renameFile(String userId, String fileId, String newName){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        File fileDrive = new File();
        fileDrive.setName(newName);

        driveExecutor.execute(userId, drive ->
                drive.files().update(file.getGoogleFileId(), fileDrive).setFields("id, name").execute());
        file.setFileName(newName);
        fileRepository.save(file);

        return fileMapper.toFileResponse(file);
    }

    // Move file to another folder
    @Transactional
    public FileResponse moveFile(String userId, String fileId, String targetFolderId){
        FileMetadata file = fileRepository.findByIdAndUser(fileId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        driveExecutor.execute(userId, drive -> {
            File current = drive.files().get(file.getGoogleFileId()).setFields("parents").execute();
            var update = drive.files().update(file.getGoogleFileId(), null)
                    .setAddParents(targetFolderId)
                    .setFields("id, parents");
            if (current.getParents() != null && !current.getParents().isEmpty()) {
                update.setRemoveParents(String.join(",", current.getParents()));
            }
            return update.execute();
        });
        file.setParentFolderId(targetFolderId);
        fileRepository.save(file);

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

    // Get picker config
    @Transactional(readOnly = true)
    public PickerConfigResponse getPickerConfig(String userId){
        return PickerConfigResponse.builder()
                .accessToken(googleTokenService.getValidAccessToken(userId))
                .apiKey(pickerApiKey)
                .appId(pickerAppId)
                .scope(DRIVE_FILE_SCOPE)
                .build();
    }

    // Save picked file
    @Transactional
    public Object savePicked(String userId, PickedFileRequest request){
        User user = getUser(userId);

        if(request.isFolder()){
            Folder folder = folderRepository.findByUserAndGoogleFolderId(userId, request.googleFileId())
                    .orElseGet(Folder::new);
            folder.setUser(user);
            folder.setGoogleFolderId(request.googleFileId());
            folder.setName(request.fileName());
            folder.setParentGoogleId(request.parentFolderId());
            folder.setSource(FolderSource.PICKED);
            folderRepository.save(folder);

            return FolderResponse.builder()
                    .id(folder.getId())
                    .name(folder.getName())
                    .parentGoogleId(folder.getParentGoogleId())
                    .googleFolderId(folder.getGoogleFolderId())
                    .source(folder.getSource().name())
                    .build();
        }
        else{
            FileMetadata file = fileRepository.findByUserAndGoogleFileId(userId, request.googleFileId())
                    .orElseGet(FileMetadata::new);
            file.setUser(user);
            file.setGoogleFileId(request.googleFileId());
            file.setFileName(request.fileName());
            file.setMimeType(request.mimeType());
            file.setSizeBytes(request.sizeBytes());
            file.setParentFolderId(request.parentFolderId());
            file.setSource(FileSource.PICKED);
            file.setDeleted(false);
            fileRepository.save(file);

            return fileMapper.toFileResponse(file);
        }
    }

    private User getUser(String userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
