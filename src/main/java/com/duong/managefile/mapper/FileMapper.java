package com.duong.managefile.mapper;

import com.duong.managefile.dto.response.FileResponse;
import com.duong.managefile.entity.FileMetadata;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMapper {
    FileResponse toFileResponse(FileMetadata fileMetadata);
}
