package com.example.SE2.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private String description;

    @ManyToMany
    @JoinTable(
            name = "category_novel",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "novel_id"))
    @JsonManagedReference
    private Set<Novel> novels = new HashSet<>();

    public Category() {
    }

    public Category(long id, String name, String description, Set<Novel> novels) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.novels = novels;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Novel> getNovels() {
        return novels;
    }

    public void setNovels(Set<Novel> novels) {
        this.novels = novels;
    }
}
