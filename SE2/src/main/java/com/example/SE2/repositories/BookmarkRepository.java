package com.example.SE2.repositories;

import com.example.SE2.models.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, String> {
    List<Bookmark> findByUserIdAndChapterId(String userId, Long chapterId);

    @Query("SELECT DISTINCT b.user.id FROM Bookmark b WHERE b.chapter.id = :chapterId AND b.paragraphIndex = :paragraphIndex")
    List<String> findUserIdsByChapterIdAndParagraphIndex(Long chapterId, int paragraphIndex);
}
