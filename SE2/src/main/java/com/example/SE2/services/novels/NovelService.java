package com.example.SE2.services.novels;

import com.example.SE2.models.Novel;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NovelService {

    Page<Novel> getTrendingNovels(int page, int size);

    Page<Novel> getRecentNovels(int page, int size);

    List<Novel> getFavoriteNovels(String userId);

    List<Novel> getCurrentlyReadingNovels(String userId);

    List<Novel> getRecommendedNovels(String userId);

    void indexNovel(Novel novel);
}
