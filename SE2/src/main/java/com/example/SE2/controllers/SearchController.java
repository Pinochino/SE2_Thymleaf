package com.example.SE2.controllers;

import com.example.SE2.dtos.request.NovelFilterRequest;
import com.example.SE2.models.Genre;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.SE2.constants.NovelStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    SearchService searchService;

    @Autowired
    GenreRepository genreRepository;

@GetMapping
    public String search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<Novel> results = (query != null && !query.isBlank())
                ? searchService.searchByVector(query, page, size)
                : Page.empty();

        populateModel(model, results);

        model.addAttribute("query",          query);
        model.addAttribute("selectedStatus", "any");
        model.addAttribute("isTrending",     false);
        model.addAttribute("selectedGenres", List.of());

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
            Model model
) {

        Pageable pageable = PageRequest.of(page, size,
                "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC,
                "averageRating");

        NovelStatus status = parseStatus(statusStr);
        Page<Novel> results = searchService.searchByFilter(
                new NovelFilterRequest(trending, genres, status), pageable);

        populateModel(model, results);
        model.addAttribute("selectedStatus", statusStr != null ? statusStr : "any");
        model.addAttribute("isTrending",     trending != null && trending);
        model.addAttribute("selectedGenres", genres   != null ? genres    : List.of());
        model.addAttribute("sort",           sort);
        model.addAttribute("searchMode", "filter");

        return "client/searchPage";
    }

    //HELPER

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
