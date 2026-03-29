package com.example.SE2.repositories;

import com.example.SE2.models.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUser_Id(String userId);

    @Query("""
                SELECT f FROM Favorite f
                JOIN FETCH f.novel
                WHERE f.user.id = :userId
            """)
    List<Favorite> findFavoritesWithNovel(String userId);

    boolean existsByUser_IdAndNovel_Id(String userId, String novelId);

    void deleteByUser_IdAndNovel_Id(String userId, String novelId);
}