package com.example.SE2.repositories;

import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long>, JpaSpecificationExecutor<Novel> {
    Novel findBookByTitle(String title);

    @Query(value = "SELECT * FROM novel ORDER BY average_rating DESC",
            nativeQuery = true)
    Page<Novel> findTrendingNovels(Pageable pageable);

    @Query(value = "SELECT * FROM novel ORDER BY updated_at DESC",
            nativeQuery = true)
    Page<Novel> findRecentNovels(Pageable pageable);

    Page<Novel> findNovelByStatus(NovelStatus status, Pageable pageable);


    @Query("""
            SELECT DISTINCT n FROM Novel n
            JOIN n.novelGenres ng
            JOIN ng.genre g
            WHERE LOWER(g.name) = LOWER(:genreName)
           """)
    Page<Novel> findByGenreName(@Param("genreName") String genreName, Pageable pageable);

    @Query(value = """
            SELECT * FROM novel
            WHERE status = :status
            ORDER BY average_rating DESC
           """, nativeQuery = true)
    Page<Novel> findTrendingByStatus(@Param("status") String status, Pageable pageable);

    @Query("""
            SELECT DISTINCT n FROM Novel n
            JOIN n.novelGenres ng
            JOIN ng.genre g
            WHERE LOWER(g.name) = LOWER(:genreName)
              AND n.status = :status
           """)
    Page<Novel> findByGenreAndStatus(
            @Param("genreName") String genreName,
            @Param("status") NovelStatus status,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT n FROM Novel n
            JOIN n.novelGenres ng
            JOIN ng.genre g
            WHERE LOWER(g.name) = LOWER(:genreName)
            ORDER BY n.averageRating DESC
           """)
    Page<Novel> findTrendingByGenre(@Param("genreName") String genreName, Pageable pageable);


}
