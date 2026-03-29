package com.example.SE2.services.novels;

import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NovelServiceImpl implements NovelService {

    @Autowired
    private NovelRepository novelRepository;

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
}
