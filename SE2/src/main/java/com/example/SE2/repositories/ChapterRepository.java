package com.example.SE2.repositories;

import com.example.SE2.models.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByNovelIdOrderByChapterNumberAsc(Long novelId);

    Optional<Chapter> findFirstByNovelIdAndChapterNumberGreaterThanOrderByChapterNumberAsc(Long novelId, Long chapterNumber);

    Optional<Chapter> findFirstByNovelIdAndChapterNumberLessThanOrderByChapterNumberDesc(Long novelId, Long chapterNumber);
}
