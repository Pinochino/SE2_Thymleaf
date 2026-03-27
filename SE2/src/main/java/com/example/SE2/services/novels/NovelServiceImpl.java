package com.example.SE2.services.novels;

import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.services.novels.NovelService;
import com.example.SE2.specs.NovelSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NovelServiceImpl implements NovelService {

    private static final double TRENDING_THRESHOLD = 4.0;


    @Autowired
    private NovelRepository novelRepository;

    @Override
    public Page<Novel> getTrendingNovels(int page, int size) {
        return novelRepository.findTrendingNovels(
                PageRequest.of(page, size)
        );
    }

    @Override
    public Page<Novel> getRecentNovels(int page, int size) {
        return novelRepository.findRecentNovels(
                PageRequest.of(page, size)
        );
    }

    @Override
    public Page<Novel> filterNovels(
            List<String> genres,
            List<NovelStatus> statuses,
            Boolean trending,
            int page,
            int size
    ) {
        Specification<Novel> spec = Specification
                .where(NovelSpecification.hasAnyGenres(genres))
                .and(NovelSpecification.hasAnyStatus(statuses))
                .and(Boolean.TRUE.equals(trending)
                        ? NovelSpecification.isTrending(TRENDING_THRESHOLD)
                        : null);

        PageRequest pageable = PageRequest.of(
                page, size,
                Sort.by("averageRating").descending()
        );

        return novelRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Novel> searchNovels(
            String keyword,
            List<String> genres,
            List<NovelStatus> statuses,
            Boolean trending,
            int page,
            int size
    ) {
        Specification<Novel> spec = Specification
                .where(NovelSpecification.hasKeyword(keyword))
                .and(NovelSpecification.hasAnyGenres(genres))
                .and(NovelSpecification.hasAnyStatus(statuses))
                .and(Boolean.TRUE.equals(trending)
                        ? NovelSpecification.isTrending(TRENDING_THRESHOLD)
                        : null);

        return novelRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by("averageRating").descending()));
    }

//    @Override
//    public Page<Novel> getRecommendedNovels(Long userId, int page, int size) {
//        return null;
//    }

//    @Override
//    public Page<Novel> getRecommendedNovels(Long userId, int page, int size) {
//        // Simple version (fallback): return trending
//        return novelRepository.findAllByOrderByAverageRatingDesc(
//                PageRequest.of(page, size)
//        );
//
//        // Later: use meta_vector or user preferences
//    }
//
//    @Override
//    public Page<Novel> getCurrentlyReadingNovels(Long userId, int page, int size) {
//        return progressRepository.findCurrentlyReading(
//                userId,
//                PageRequest.of(page, size)
//        );
//    }
}