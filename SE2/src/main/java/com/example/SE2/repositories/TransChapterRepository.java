package com.example.SE2.repositories;

import com.example.SE2.models.TransChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransChapterRepository extends JpaRepository<TransChapter, Long> {
}
