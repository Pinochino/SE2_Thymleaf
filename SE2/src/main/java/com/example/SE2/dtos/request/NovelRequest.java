package com.example.SE2.dtos.request;

import com.example.SE2.constants.NovelStatus;
import org.springframework.web.multipart.MultipartFile;

public class NovelRequest {

    private String title;

    private String description;

    private String content;

    private Long genreId;

    private MultipartFile imageFile;

    private String imagePath;

    private String author;

    private NovelStatus status;

    public NovelRequest() {
    }

    public NovelRequest(String title,
                        String description,
                        String content,
                        Long genreId,
                        MultipartFile imageFile,
                        String imagePath,
                        String author,
                        NovelStatus status) {
        this.title = title;
        this.description = description;
        this.content = content;
        this.genreId = genreId;
        this.imageFile = imageFile;
        this.imagePath = imagePath;
        this.author = author;
        this.status = status;
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

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public NovelStatus getStatus() {
        return status;
    }

    public void setStatus(NovelStatus status) {
        this.status = status;
    }
}
