package com.example.SE2.repositories;

import com.example.SE2.constants.GenreName;
import com.example.SE2.models.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    Genre findGenreByName(GenreName name);
}
