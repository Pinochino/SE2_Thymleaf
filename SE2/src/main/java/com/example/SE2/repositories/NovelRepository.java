package com.example.SE2.repositories;

import com.example.SE2.models.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    Novel findBookByTitle(String title);

    @Query(value = "SELECT * FROM novel ORDER BY average_rating DESC",
            nativeQuery = true)
    Page<Novel> findTrendingNovelsNative(Pageable pageable);

    @Query(value = "SELECT * FROM novel ORDER BY updated_at DESC",
            nativeQuery = true)
    Page<Novel> findRecentNovelsNative(Pageable pageable);


}
