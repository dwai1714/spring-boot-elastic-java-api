package com.elastic.dto;

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

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public void setUploadType(String uploadType) {
        this.uploadType = uploadType;
    }

    private String placeName;
    private String categoryName;
    private String subCategoryName;
private String uploadType;
private List<String> fileNames;
}
