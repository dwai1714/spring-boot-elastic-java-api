package com.elastic.dto;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.List;

public class UploadDTO implements Serializable {
    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }

    public void setSubCategoryName(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }

    public String getUploadType() {
        return uploadType;
    }



    public void setUploadType(String uploadType) {
        this.uploadType = uploadType;
    }

    private String placeName;
    private String categoryName;
    private String subCategoryName;
    private String uploadType;


    public MultipartFile[] getMultiPartFiles() {
        return multiPartFiles;
    }

    public void setMultiPartFiles(MultipartFile[] multiPartFiles) {
        this.multiPartFiles = multiPartFiles;
    }

    public UploadDTO(String placeName, String categoryName, String subCategoryName, String uploadType, MultipartFile[] multiPartFiles) {
        this.placeName = placeName;
        this.categoryName = categoryName;
        this.subCategoryName = subCategoryName;
        this.uploadType = uploadType;
        this.multiPartFiles = multiPartFiles;
    }

    private MultipartFile[] multiPartFiles;
}
