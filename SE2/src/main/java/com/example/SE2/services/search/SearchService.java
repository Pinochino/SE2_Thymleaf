package com.example.SE2.services.search;

import com.example.SE2.dtos.request.NovelFilterRequest;
import com.example.SE2.models.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchService {
    Page<Novel> searchByVector(String query, int page, int size);
    Page<Novel> searchByFilter(NovelFilterRequest request, Pageable pageable);
}
