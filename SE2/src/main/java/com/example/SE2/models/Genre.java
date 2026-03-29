package com.example.SE2.models;

import com.example.SE2.constants.GenreName;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private GenreName name;

    @ManyToMany
    @JoinTable(
            name = "novel_genre",
            joinColumns = @JoinColumn(name = "genre_id"),
            inverseJoinColumns = @JoinColumn(name = "novel_id"))
    @JsonManagedReference
    private Set<Novel> novels = new HashSet<>();

    public Genre() {
    }

    public Genre(GenreName name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GenreName getName() {
        return name;
    }

    public void setName(GenreName name) {
        this.name = name;
    }

    public Set<Novel> getNovels() {
        return novels;
    }

    public void setNovels(Set<Novel> novels) {
        this.novels = novels;
    }


}
