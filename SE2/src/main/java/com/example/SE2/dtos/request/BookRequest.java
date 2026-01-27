package com.example.SE2.dtos.request;

import org.springframework.web.multipart.MultipartFile;

public class BookRequest {

    private String title;

    private String description;

    private Long categoryId;

    private MultipartFile imageFile;

    private String imagePath;

    private String author;

    public BookRequest() {
    }

    public BookRequest(String title, String description, Long categoryId, MultipartFile imageFile, String imagePath,
                       String author) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.imageFile = imageFile;
        this.imagePath = imagePath;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
