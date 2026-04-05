package com.example.SE2.services.novels;

import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.services.embedding.EmbeddingServiceImpl;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.SE2.models.Genre;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NovelServiceImpl implements NovelService {

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private EmbeddingServiceImpl embeddingService;

    @Override
    public Page<Novel> getTrendingNovels(int page, int size) {
        return novelRepository.findTrendingNovels(PageRequest.of(page, size));
    }

    @Override
    public Page<Novel> getRecentNovels(int page, int size) {
        return novelRepository.findRecentNovels(PageRequest.of(page, size));
    }

    @Override
    public List<Novel> getFavoriteNovels(String userId) {
        return novelRepository.findFavoritesByUserId(userId);
    }

    @Override
    public List<Novel> getCurrentlyReadingNovels(String userId) {
        return novelRepository.findCurrentlyReadingByUserId(userId, PageRequest.of(0, 6));
    }



    @Override
    public List<Novel> getRecommendedNovels(String userId) {
        // Try currently reading first, then favorites
        List<Novel> sourceNovels = getCurrentlyReadingNovels(userId);
        if (sourceNovels.isEmpty()) {
            sourceNovels = getFavoriteNovels(userId);
        }
        if (sourceNovels.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect genre IDs from source novels
        List<Long> genreIds = sourceNovels.stream()
                .flatMap(n -> n.getGenres().stream())
                .map(Genre::getId)
                .distinct()
                .collect(Collectors.toList());

        if (genreIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Exclude source novels from results
        List<Long> excludeIds = sourceNovels.stream()
                .map(Novel::getId)
                .collect(Collectors.toList());

        return novelRepository.findByGenreIdsExcluding(genreIds, excludeIds, PageRequest.of(0, 6));
    }

    public void indexNovel(Novel novel) {
        Hibernate.initialize(novel.getGenres());

        float[] vector = embeddingService.embedNovel(novel);

        saveWithTransaction(novel, vector);
    }

    @Transactional
    public void saveWithTransaction(Novel novel, float[] vector) {
        novel.setMetaVector(vector);
        novelRepository.save(novel);
    }
}
