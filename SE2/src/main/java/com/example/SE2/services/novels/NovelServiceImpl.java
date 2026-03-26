package com.example.SE2.services.novels;

import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.services.novels.NovelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class NovelServiceImpl implements NovelService {

    @Autowired
    private NovelRepository novelRepository;

    @Override
    public Page<Novel> getTrendingNovels(int page, int size) {
        return novelRepository.findTrendingNovelsNative(
                PageRequest.of(page, size)
        );
    }

    @Override
    public Page<Novel> getRecentNovels(int page, int size) {
        return novelRepository.findRecentNovelsNative(
                PageRequest.of(page, size)
        );
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