package com.example.SE2.controllers;

import com.example.SE2.dtos.request.NovelFilterRequest;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.GenreRepository;
import com.example.SE2.services.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.SE2.constants.NovelStatus;
import org.springframework.data.domain.Sort;
import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    SearchService searchService;

    @Autowired
    GenreRepository genreRepository;

    @Autowired
    com.example.SE2.repositories.NovelRepository novelRepository;

@GetMapping
    public String search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<Novel> results;
        if (query != null && !query.isBlank()) {
            results = searchService.searchByVector(query, page, size);
        } else {
            results = novelRepository.findAllNovels(PageRequest.of(page, size));
        }

        populateModel(model, results);

        model.addAttribute("query",          query);
        model.addAttribute("selectedStatus", "any");
        model.addAttribute("isTrending",     false);
        model.addAttribute("selectedGenres", List.of());
        model.addAttribute("baseUrl", "/search");
        model.addAttribute("extraParams", query != null ? "query=" + query : "");

        return "client/searchPage";
    }

@GetMapping("/filter")
    public String searchByFilter(
            @RequestParam(required = false) Boolean trending,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) String statusStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.example.SE2.security.UserDetailImpl userDetails,
            Model model
) {
        boolean isLoggedIn = userDetails != null;

        Pageable pageable = PageRequest.of(page, size,
                "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC,
                "averageRating");

        // Only apply status/trending filter when logged in
        NovelStatus status = isLoggedIn ? parseStatus(statusStr) : null;
        Boolean effectiveTrending = isLoggedIn ? trending : null;
        Page<Novel> results = searchService.searchByFilter(
                new NovelFilterRequest(effectiveTrending, genres, status), pageable);

        populateModel(model, results);
        model.addAttribute("selectedStatus", isLoggedIn && statusStr != null ? statusStr : "any");
        model.addAttribute("isTrending",     isLoggedIn && trending != null && trending);
        model.addAttribute("selectedGenres", genres   != null ? genres    : List.of());
        model.addAttribute("sort",           sort);
        model.addAttribute("searchMode", "filter");
        model.addAttribute("baseUrl", "/search/filter");

        StringBuilder extra = new StringBuilder("sort=" + sort);
        if (isLoggedIn && statusStr != null) extra.append("&statusStr=").append(statusStr);
        if (isLoggedIn && trending != null && trending) extra.append("&trending=true");
        model.addAttribute("extraParams", extra.toString());

        return "client/searchPage";
    }

    private void populateModel(Model model, Page<Novel> results) {
        int currentPage = results.getNumber();
        int totalPages  = results.getTotalPages();

        model.addAttribute("genres",          genreRepository.findAll());
        model.addAttribute("searchedNovels",  results);
        model.addAttribute("totalResults",    results.getTotalElements());
        model.addAttribute("currentPage",     currentPage);
        model.addAttribute("totalPages",      totalPages);
        model.addAttribute("paginationStart", Math.max(1, currentPage - 2));
        model.addAttribute("paginationEnd",   Math.min(totalPages, currentPage + 2));
    }

    private NovelStatus parseStatus(String statusStr) {
        if (statusStr == null || "any".equalsIgnoreCase(statusStr)) return null;
        try {
            return NovelStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
