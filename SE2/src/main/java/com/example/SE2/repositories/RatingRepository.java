package com.example.SE2.repositories;

import com.example.SE2.models.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndNovelId(String userId, Long novelId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.novel.id = :novelId")
    Float findAverageByNovelId(Long novelId);
}
