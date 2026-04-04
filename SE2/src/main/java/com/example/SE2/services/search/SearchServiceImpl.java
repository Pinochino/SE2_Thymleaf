package com.example.SE2.services.search;

import com.example.SE2.dtos.request.NovelFilterRequest;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.services.embedding.EmbeddingService;
import com.example.SE2.services.embedding.EmbeddingServiceImpl;
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
    private EmbeddingService embeddingService;

    @Autowired
    private NovelRepository novelRepository;

    @Override
    public Page<Novel> searchByVector(String query, int page, int size) {
        Float[] vector   = embeddingService.embed(query);
        String formatted = Arrays.toString(vector);
        int offset       = page * size;

        List<Novel> results = novelRepository.searchVector(formatted, size, offset);
        long total          = novelRepository.countAllNovels();

        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }

    @Override
    public Page<Novel> searchByFilter(NovelFilterRequest request, Pageable pageable) {
        boolean hasFilter = (request.getTrending() != null && request.getTrending())
                || (request.getGenres() != null && !request.getGenres().isEmpty())
                || request.getStatus() != null;

        if (!hasFilter) {
            throw new IllegalArgumentException(
                    "At least one filter must be provided."
            );
        }

        return novelRepository.searchFilter(
                request.getTrending(),
                request.getGenres(),
                request.getStatus(),
                pageable
        );
    }

}
