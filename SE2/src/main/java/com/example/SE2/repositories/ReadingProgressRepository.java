package com.example.SE2.repositories;

import com.example.SE2.models.Novel;
import com.example.SE2.models.ReadingProgress;

import com.example.SE2.models.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {
    Optional<ReadingProgress> findByUserIdAndChapterId(String userId, Long chapterId);


    List<ReadingProgress> findByUserIdAndChapterNovelId(String userId, Long novelId);
    
    Optional<ReadingProgress> findTopByUserAndChapter_NovelOrderByUpdatedAtDesc(
            User user, Novel novel
    );
}
