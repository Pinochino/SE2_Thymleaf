package com.example.SE2.services.novels;

import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Novel;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NovelService {

    Page<Novel> getTrendingNovels(int page, int size);

    Page<Novel> getRecentNovels(int page, int size);

    Page<Novel> filterNovels(
            List<String> genres,
            List<NovelStatus> statuses,
            Boolean trending,
            int page,
            int size
    );

    Page<Novel> searchNovels(
            String keyword,
            List<String> genres,
            List<NovelStatus> statuses,
            Boolean trending,
            int page,
            int size
    );


//    Page<Novel> getRecommendedNovels(Long userId, int page, int size);

//    Page<Novel> getCurrentlyReadingNovels(Long userId, int page, int size);
}