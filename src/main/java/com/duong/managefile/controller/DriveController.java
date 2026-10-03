package com.duong.managefile.controller;

import com.duong.managefile.dto.request.CreateFolderRequest;
import com.duong.managefile.dto.request.MoveFileRequest;
import com.duong.managefile.dto.request.PickedFileRequest;
import com.duong.managefile.dto.request.RenameRequest;
import com.duong.managefile.dto.response.*;
import com.duong.managefile.service.DriveService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/drives")
@RequiredArgsConstructor
public class DriveController {
    private final DriveService driveService;

    // Create folder
    @PostMapping("/folders")
    public ApiResponse<FolderResponse> createFolder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateFolderRequest request
    ){
        String userId = jwt.getSubject();
        FolderResponse data = driveService.createFolder(userId, request);
        return ApiResponse.<FolderResponse>builder()
                .status("success")
                .data(data)
                .message("Create folder successfully")
                .build();
    }

    // Upload file
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> uploadFile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String parentFolderId
    ){
        String userId = jwt.getSubject();
        FileResponse data = driveService.uploadFile(userId, file, parentFolderId);
        return ApiResponse.<FileResponse>builder()
                .status("success")
                .data(data)
                .message("Upload file successfully")
                .build();
    }

    // List files
    @GetMapping("/files")
    public PageResponse<FileResponse> listFiles(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String parentFolderId,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return driveService.listFiles(jwt.getSubject(), parentFolderId, mimeType, name, page, size);
    }

    // Get file detail
    @GetMapping("/files/{id}")
    public ApiResponse<FileResponse> getFile(@AuthenticationPrincipal Jwt jwt, @PathVariable String id){
        FileResponse data = driveService.getFile(jwt.getSubject(), id);
        return ApiResponse.<FileResponse>builder()
                .status("success")
                .data(data)
                .message("Get file successfully")
                .build();
    }

    // Download file
    @GetMapping("/files/{id}/download")
    public ApiResponse<Void> downloadFile(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, HttpServletResponse response){
        driveService.downloadFile(jwt.getSubject(), id, response);

        return ApiResponse.<Void>builder()
                .status("success")
                .message("Download file successfully")
                .build();
    }

    // Rename file
    @PutMapping("/files/{id}/rename")
    public ApiResponse<FileResponse> renameFile(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @RequestBody RenameRequest request){
        FileResponse data = driveService.renameFile(jwt.getSubject(), id, request.newName());

        return ApiResponse.<FileResponse>builder()
                .status("success")
                .data(data)
                .message("Rename file successfully")
                .build();
    }

    // Delete file
    @DeleteMapping("/files/{id}")
    public ApiResponse<Void> deleteFile(@AuthenticationPrincipal Jwt jwt, @PathVariable String id){
        driveService.deleteFile(jwt.getSubject(), id);

        return ApiResponse.<Void>builder()
                .status("success")
                .message("Delete file successfully")
                .build();
    }

    // Move file
    @PutMapping("/files/{id}/move")
    public ApiResponse<FileResponse> moveFile(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @RequestBody MoveFileRequest request){
        FileResponse data = driveService.moveFile(jwt.getSubject(), id, request.targetFolderId());

        return ApiResponse.<FileResponse>builder()
                .status("success")
                .data(data)
                .message("Move file successfully")
                .build();
    }

    // Get folder tree
    @GetMapping("/folders/tree")
    public PageResponse<FolderResponse> getFolderTree(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String parentGoogleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return driveService.getFolderTree(jwt.getSubject(), parentGoogleId, page, size);
    }

    // Picker config
    @GetMapping("/picker/config")
    public ApiResponse<PickerConfigResponse> pickerConfig(@AuthenticationPrincipal Jwt jwt){
        PickerConfigResponse data = driveService.getPickerConfig(jwt.getSubject());
        return ApiResponse.<PickerConfigResponse>builder()
                .status("success")
                .data(data)
                .build();
    }

    // Save picked file
    @PostMapping("/picker/picked")
    public ApiResponse<Object> picked(@AuthenticationPrincipal Jwt jwt, @RequestBody PickedFileRequest request){
        Object data = driveService.savePicked(jwt.getSubject(), request);
        return ApiResponse.builder()
                .status("success")
                .data(data)
                .message("Picked file successfully")
                .build();
    }
}
