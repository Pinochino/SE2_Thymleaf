package com.example.SE2.controllers;

import com.example.SE2.models.Favorite;
import com.example.SE2.models.Novel;
import com.example.SE2.models.Rating;
import com.example.SE2.models.User;
import com.example.SE2.repositories.FavoriteRepository;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.repositories.RatingRepository;
import com.example.SE2.security.UserDetailImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/novels")
public class NovelApiController {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @PostMapping("/{novelId}/rate")
    public ResponseEntity<?> rateNovel(@PathVariable Long novelId,
                                       @RequestBody Map<String, Integer> body,
                                       @AuthenticationPrincipal UserDetailImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }

        Integer score = body.get("score");
        if (score == null || score < 1 || score > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Score must be between 1 and 5"));
        }

        User user = userDetails.getUser();
        Novel novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<Rating> existing = ratingRepository.findByUserIdAndNovelId(user.getId(), novelId);
        Rating rating;
        if (existing.isPresent()) {
            rating = existing.get();
            rating.setScore(score);
        } else {
            rating = new Rating();
            rating.setUser(user);
            rating.setNovel(novel);
            rating.setScore(score);
        }
        ratingRepository.save(rating);

        // Update novel average rating
        Float avg = ratingRepository.findAverageByNovelId(novelId);
        if (avg != null) {
            novel.setAverageRating(Math.round(avg * 10) / 10.0f);
            novelRepository.save(novel);
        }

        return ResponseEntity.ok(Map.of(
                "score", score,
                "averageRating", novel.getAverageRating() != null ? novel.getAverageRating() : 0
        ));
    }

    @PostMapping("/{novelId}/favorite")
    @Transactional
    public ResponseEntity<?> toggleFavorite(@PathVariable Long novelId,
                                            @AuthenticationPrincipal UserDetailImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }

        User user = userDetails.getUser();
        Novel novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null) {
            return ResponseEntity.notFound().build();
        }

        boolean exists = favoriteRepository.existsByUser_IdAndNovel_Id(user.getId(), novelId);
        if (exists) {
            favoriteRepository.deleteByUser_IdAndNovel_Id(user.getId(), novelId);
            return ResponseEntity.ok(Map.of("favorited", false));
        } else {
            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setNovel(novel);
            favoriteRepository.save(favorite);
            return ResponseEntity.ok(Map.of("favorited", true));
        }
    }

    @GetMapping("/{novelId}/favorite/status")
    public ResponseEntity<?> getFavoriteStatus(@PathVariable Long novelId,
                                               @AuthenticationPrincipal UserDetailImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("favorited", false));
        }
        boolean exists = favoriteRepository.existsByUser_IdAndNovel_Id(userDetails.getUser().getId(), novelId);
        return ResponseEntity.ok(Map.of("favorited", exists));
    }

    @GetMapping("/{novelId}/rating/user")
    public ResponseEntity<?> getUserRating(@PathVariable Long novelId,
                                           @AuthenticationPrincipal UserDetailImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("score", 0));
        }
        Optional<Rating> rating = ratingRepository.findByUserIdAndNovelId(userDetails.getUser().getId(), novelId);
        return ResponseEntity.ok(Map.of("score", rating.map(Rating::getScore).orElse(0)));
    }
}
