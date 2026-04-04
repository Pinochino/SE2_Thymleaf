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

import java.util.Arrays;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService{

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Override
    public Page<Novel> searchByVector(String query, int page, int size) {
        Float[] vector   = embeddingService.embed(query);
        String formatted = Arrays.toString(vector);
        int offset       = page * size;

        List<Novel> results = novelRepository.searchVector(formatted, size, offset);
        long total          = novelRepository.countVectorSearch(formatted);

        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }

    @Override
    public Page<Novel> searchByFilter(NovelFilterRequest request, Pageable pageable) {
        return novelRepository.searchFilter(
                request.getTrending(),
                4.0,
                request.getGenres(),
                request.getStatus(),
                pageable
        );
    }

}
