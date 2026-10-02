package com.duong.managefile.controller;

import com.duong.managefile.dto.request.CreateFolderRequest;
import com.duong.managefile.dto.response.ApiResponse;
import com.duong.managefile.dto.response.FileResponse;
import com.duong.managefile.dto.response.FolderResponse;
import com.duong.managefile.service.DriveService;
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
}
