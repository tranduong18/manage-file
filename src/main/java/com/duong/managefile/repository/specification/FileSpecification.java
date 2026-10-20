package com.duong.managefile.repository.specification;

import com.duong.managefile.entity.FileMetadata;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class FileSpecification {
    public static Specification<FileMetadata> belongsToUser(String userId){
        return ((root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("user").get("id"), userId)
        );
    }

    public static Specification<FileMetadata> notDeleted(){
        return ((root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("deleted"), false)
        );
    }

    public static Specification<FileMetadata> inFolder(String parentFolderId){
        return ((root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(parentFolderId)) {
                return criteriaBuilder.isNull(root.get("parentFolderId"));
            }
            return criteriaBuilder.equal(root.get("parentFolderId"), parentFolderId);
        });
    }

    public static Specification<FileMetadata> hasMimeType(String mimeType){
        return ((root, query, cb) -> {
            if (!StringUtils.hasText(mimeType)) {
                return cb.conjunction();
            }
            String v = mimeType.trim().toLowerCase();
            if(v.endsWith("/")){
                String escaped = v.replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                return cb.like(cb.lower(root.get("mimeType")), escaped + "%", '\\');
            }
            return cb.equal(cb.lower(root.get("mimeType")), v);
        });
    }

    public static Specification<FileMetadata> hasName(String name) {
        return ((root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(name)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("fileName")),
                    "%" + name.toLowerCase() + "%");
        });
    }
}
