package com.example.SE2.repositories;

import com.example.SE2.models.Chapter;
import com.example.SE2.models.Novel;
import com.example.SE2.models.ReadingProgress;

import com.example.SE2.models.User;
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
            User user, Novel novel);

    @Query("SELECT rp FROM ReadingProgress rp " +
            "WHERE rp.user.email = :email " +
            "AND rp.chapter.novel = :novel " +
            "ORDER BY rp.chapter.chapterNumber DESC")
    Optional<ReadingProgress> findLatestByUserAndNovel(
            @Param("email") String email,
            @Param("novel") Novel novel);

    ReadingProgress findTopByUserEmailAndChapterNovelOrderByUpdatedAtDesc(
            String username, Novel novel);

    // ✅ Thay thế findByUserAndChapter — dùng findFirst để không bị
    // NonUniqueResultException khi DB có duplicate rows
    Optional<ReadingProgress> findFirstByUserAndChapterOrderByIdDesc(
            User user, Chapter chapter);

    // Đã có sẵn nhưng trả List — dùng được cho upsert
    List<ReadingProgress> findAllByUserIdAndChapterId(String userId, Long chapterId);
}
