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

import java.util.Arrays;
import java.util.List;

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
