package com.duong.managefile.mapper;

import com.duong.managefile.dto.response.FolderResponse;
import com.duong.managefile.entity.Folder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FolderMapper {
    FolderResponse toFolderResponse(Folder folder);
}
