package com.example.SE2.services.search;

import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.services.embedding.EmbeddingServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService{

    @Autowired
    private EmbeddingServiceImpl embeddingService;

    @Autowired
    private NovelRepository novelRepository;

    public SearchServiceImpl(EmbeddingServiceImpl embeddingService, NovelRepository novelRepository) {
        this.embeddingService = embeddingService;
        this.novelRepository = novelRepository;
    }

    @Override
    public List<Novel> searchByVector(String query, int limit) {
        Float[] vector = embeddingService.embed(query);
        String formatted = Arrays.toString(vector);
        return novelRepository.searchVector(formatted, limit);
    }

    @Override
    public List<Novel> searchByFilter() {
        return List.of();
    }


}
