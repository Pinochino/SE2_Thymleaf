package com.example.SE2.services.search;

import com.example.SE2.models.Novel;

import java.util.List;

public interface SearchService {
    List<Novel> searchByVector(String query, int limit);
    List<Novel> searchByFilter();
}
