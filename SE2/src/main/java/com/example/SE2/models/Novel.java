package com.example.SE2.models;

import com.example.SE2.constants.NovelStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

@Entity
public class Novel extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, updatable = false)
    private UUID publicId;

    @Column(unique = true)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String author;

    @Enumerated(EnumType.STRING)
    private NovelStatus status;

    private Float averageRating;

    @Column(columnDefinition = "TEXT")
    private String coverImgUrl;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "meta_vector", columnDefinition = "vector(384)")
    private float[] metaVector;

    @ManyToMany(mappedBy = "novels")
    @JsonBackReference
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "novel", cascade = CascadeType.ALL)
    private Set<Chapter> chapters = new HashSet<>();

    @OneToMany(mappedBy = "novel", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<NovelComment> comments = new ArrayList<>();


    public Novel() {
    }

    @PrePersist
    public void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public NovelStatus getStatus() {
        return status;
    }

    public void setStatus(NovelStatus status) {
        this.status = status;
    }

    public Float getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Float averageRating) {
        this.averageRating = averageRating;
    }

    public String getCoverImgUrl() {
        return coverImgUrl;
    }

    public void setCoverImgUrl(String coverImgUrl) {
        this.coverImgUrl = coverImgUrl;
    }

    public float[] getMetaVector(){
        return this.metaVector;
    }

    public void setMetaVector(float[] metaVector){
        this.metaVector = metaVector;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Set<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(Set<Chapter> chapters) {
        this.chapters = chapters;
    }

    public List<NovelComment> getComments() {
        return comments;
    }

    public void setComments(List<NovelComment> comments) {
        this.comments = comments;
    }


    public Novel(Long id,
                 UUID publicId,
                 String title,
                 String description,
                 String author,
                 NovelStatus status,
                 Float averageRating,
                 String coverImgUrl,
                 float[] metaVector,
                 Set<Genre> genres,
                 Set<Chapter> chapters,
                 List<NovelComment> comments) {
        this.id = id;
        this.publicId = publicId;
        this.title = title;
        this.description = description;
        this.author = author;
        this.status = status;
        this.averageRating = averageRating;
        this.coverImgUrl = coverImgUrl;
        this.metaVector=metaVector;
        this.genres = genres;
        this.chapters = chapters;
        this.comments = comments;
    }
}
