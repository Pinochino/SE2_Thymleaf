package com.example.SE2.dtos.request;

import com.example.SE2.constants.NovelStatus;

import java.util.List;

public class NovelFilterRequest {
    private Boolean trending;
    private List<String> genres;
    private NovelStatus status;

    public NovelFilterRequest(Boolean trending, List<String> genres, NovelStatus status) {
        this.trending = trending;
        this.genres = genres;
        this.status = status;
    }

    public Boolean getTrending() {
        return trending;
    }

    public void setTrending(Boolean trending) {
        this.trending = trending;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public NovelStatus getStatus() {
        return status;
    }

    public void setStatus(NovelStatus status) {
        this.status = status;
    }
}