package com.example.SE2.services.search;

import com.example.SE2.dtos.request.NovelFilterRequest;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.services.embedding.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private EmbeddingService embeddingService;

    private static final int MAX_VECTOR_TOTAL = 100;

    @Override
    public Page<Novel> searchByVector(String query, int page, int size) {
        int offset = page * size;
        float[] vector = embeddingService.embed(query);
        String formated = toVectorString(vector);
        List<Novel> results = novelRepository.searchVector(formated, size, offset);
        long total = Math.min(novelRepository.countAllNovels(), MAX_VECTOR_TOTAL);

        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }

    @Override
    public Page<Novel> searchByFilter(NovelFilterRequest request, Pageable pageable) {
        Assert.notNull(request, "Filter must not null");

        return novelRepository.searchFilter(
                request.getTrending(),
                request.getGenres(),
                request.getStatus(),
                pageable
        );
    }

    //Helper
    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
