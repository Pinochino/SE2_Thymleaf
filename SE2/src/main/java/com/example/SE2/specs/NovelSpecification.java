package com.example.SE2.specs;

import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Genre;
import com.example.SE2.models.Novel;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;


public class NovelSpecification {

    //keyword
    public static Specification<Novel> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")),  pattern),
                    cb.like(cb.lower(root.get("author")), pattern)
            );
        };
    }

    //genre
    public static Specification<Novel> hasGenre(String genreName){
        return ((root, query, criteriaBuilder) -> {
            if(genreName == null || genreName.isBlank()) return null;
            query.distinct(true);

            Join<Novel,Genre> g= root.join("genres", JoinType.INNER);
            return criteriaBuilder.equal(criteriaBuilder.lower(g.get("name")), genreName.toLowerCase());
        });

    }

    //many genres
    public static Specification<Novel> hasAnyGenres(List<String> genreNames){
        return ((root, query, criteriaBuilder) -> {
            if (genreNames == null || genreNames.isEmpty()) return null;
            query.distinct(true);
            Join<Novel, Genre> g = root.join("genres", JoinType.INNER);
            List<String> lower = genreNames.stream()
                    .map(String::toLowerCase)
                    .toList();
            return criteriaBuilder.lower(g.get("name")).in(lower);
        });
    }

    //status
    public static Specification<Novel> hasStatus(NovelStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    //many status
    public static Specification<Novel> hasAnyStatus(List<NovelStatus> statuses) {
        return (root, query, criteriaBuilder) ->
                (statuses == null || statuses.isEmpty())
                        ? null : root.get("status").in(statuses);
    }

    //trending
    public static Specification<Novel> isTrending(double minRating) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("averageRating"), minRating);
    }

}
