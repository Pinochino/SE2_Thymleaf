package com.example.SE2.controllers;

import com.example.SE2.dtos.request.NovelFilterRequest;
import com.example.SE2.models.Novel;
import com.example.SE2.models.User;
import com.example.SE2.repositories.GenreRepository;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.services.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SearchController.class);

    @Autowired
    SearchService searchService;
    private final UserRepository userRepository;

    @Autowired
    GenreRepository genreRepository;

    @Autowired
    com.example.SE2.repositories.NovelRepository novelRepository;

    public SearchController (UserRepository userRepository){
        this.userRepository = userRepository;
    }

@GetMapping
    public String search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "semantic") String mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<Novel> results;
        String searchMode = "filter";

        if (query != null && !query.isBlank()) {
            if ("keyword".equalsIgnoreCase(mode)) {
                results = searchService.searchByKeyword(query, page, size);
                searchMode = "keyword";
            } else {
                results = searchService.searchByVector(query, page, size);
                searchMode = "vector";
            }
        } else {
            results = novelRepository.findAllNovels(PageRequest.of(page, size));
        }

        populateModel(model, results);

        model.addAttribute("query",          query);
        model.addAttribute("selectedStatus", "any");
        model.addAttribute("isTrending",     false);
        model.addAttribute("selectedGenres", List.of());
        model.addAttribute("sort",           "desc");
        model.addAttribute("searchMode",     searchMode);
        model.addAttribute("searchModeOption", mode);

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("isLoggedIn", true);

        }
        model.addAttribute("isLoggedIn", false);

        return "client/searchPage";
    }
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String email = auth.getName();
        return userRepository.findUserByEmail(email);
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

        log.info("Filter: statusStr={}, parsed={}, trending={}, genres={}, results={}",
                statusStr, status, trending, genres, results.getTotalElements());

        populateModel(model, results);
        model.addAttribute("selectedStatus", statusStr != null ? statusStr : "any");
        model.addAttribute("isTrending",     trending != null && trending);
        model.addAttribute("selectedGenres", genres   != null ? genres    : List.of());
        model.addAttribute("sort",           sort);
        model.addAttribute("searchMode", "filter");
        model.addAttribute("baseUrl", "/search/filter");

        StringBuilder extra = new StringBuilder("sort=" + sort);
        if (statusStr != null) extra.append("&statusStr=").append(statusStr);
        if (trending != null && trending) extra.append("&trending=true");
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
        int paginationStart = Math.max(0, currentPage - 2);
        int paginationEnd = Math.min(totalPages - 1, currentPage + 2);
        // Ensure at least 5 pages shown when available
        if (paginationEnd - paginationStart < 4 && totalPages >= 5) {
            paginationStart = Math.max(0, paginationEnd - 4);
        }
        model.addAttribute("paginationStart", paginationStart);
        model.addAttribute("paginationEnd",   paginationEnd);
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
