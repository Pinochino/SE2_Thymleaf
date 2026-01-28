package com.example.SE2.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String title;

    private boolean status;

    @Lob()
    @Column(columnDefinition = "TEXT", name = "description")
    private String description;

    @Lob()
    @Column(columnDefinition = "TEXT", name = "content")
    private String content;

    private String author;

    private String image;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1024)
    @Column(columnDefinition = "VECTOR(1024)")
    private float[] embedding;

    @ManyToMany(mappedBy = "books")
    @JsonBackReference
    private Set<Category> categories = new HashSet<>();

    public Book() {
    }

    public Book(long id, String title,
                boolean status, String description,
                String content, String author,
                String image, float[] embedding,
                Set<Category> categories) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.description = description;
        this.content = content;
        this.author = author;
        this.image = image;
        this.embedding = embedding;
        this.categories = categories;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
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

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
