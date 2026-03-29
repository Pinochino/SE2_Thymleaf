package com.example.SE2.repositories;

import com.example.SE2.models.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUser_Id(String userId);

    @Query(value = "SELECT f FROM Favorite f JOIN FETCH f.novel WHERE f.user.id = :userId",
           countQuery = "SELECT count(f) FROM Favorite f WHERE f.user.id = :userId")
    Page<Favorite> findFavoritesWithNovel(String userId, Pageable pageable);

    boolean existsByUser_IdAndNovel_Id(String userId, Long novelId);

    void deleteByUser_IdAndNovel_Id(String userId, Long novelId);
}