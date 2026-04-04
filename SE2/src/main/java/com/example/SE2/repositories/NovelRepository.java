package com.example.SE2.repositories;

import com.example.SE2.models.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    Novel findBookByTitle(String title);

    Novel findByTitle(String title);

    boolean existsByTitle(String title);
    Novel findNovelByPublicId(UUID publicId);

    @Query("SELECT n FROM Novel n ORDER BY n.averageRating DESC NULLS LAST")
    Page<Novel> findTrendingNovels(Pageable pageable);

    @Query("SELECT n FROM Novel n ORDER BY n.updatedAt DESC")
    Page<Novel> findRecentNovels(Pageable pageable);

    @Query("SELECT n FROM Novel n WHERE n.id IN (SELECT DISTINCT rp.chapter.novel.id FROM ReadingProgress rp WHERE rp.user.id = :userId) ORDER BY n.updatedAt DESC")
    List<Novel> findCurrentlyReadingByUserId(String userId, Pageable pageable);

    @Query("SELECT f.novel FROM Favorite f WHERE f.user.id = :userId")
    List<Novel> findFavoritesByUserId(String userId);

    @Query("SELECT DISTINCT n FROM Novel n JOIN n.genres g WHERE g.name = :genreName ORDER BY n.updatedAt DESC")
    Page<Novel> findByGenreName(@org.springframework.data.repository.query.Param("genreName") com.example.SE2.constants.GenreName genreName, Pageable pageable);

    @Query("SELECT n FROM Novel n ORDER BY n.updatedAt DESC")
    Page<Novel> findAllNovels(Pageable pageable);
}
