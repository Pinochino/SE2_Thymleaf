package com.example.SE2.controllers;

import com.example.SE2.dtos.request.NovelFilterRequest;
import com.example.SE2.models.Novel;
import com.example.SE2.services.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.SE2.constants.NovelStatus;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    SearchService searchService;

    @GetMapping
    public String searchPage() {
        return "client/searchPage";
    }

    @GetMapping("/vector")
    public String searchByVector(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        if (keyword == null || keyword.isBlank()) {
            model.addAttribute("searchedNovels", Page.empty());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages",  0);
            return "client/searchPage";
        }

        Page<Novel> results = searchService.searchByVector(keyword, page, size);

        model.addAttribute("searchedNovels", results);
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     results.getTotalPages());
        model.addAttribute("keyword",        keyword);

        return "client/searchPage";
    }

    @GetMapping("/filter")
    public String searchByFilter(
            @RequestParam(required = false) Boolean trending,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) NovelStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        NovelFilterRequest req = new NovelFilterRequest(trending,genres,status);
        req.setTrending(trending);
        req.setGenres(genres);
        req.setStatus(status);

        boolean hasFilter = (trending != null && trending)
                || (genres != null && !genres.isEmpty())
                || status != null;

        if (!hasFilter) {
            model.addAttribute("searchedNovels", Page.empty());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages",  0);
            return "client/searchPage";
        }

        Page<Novel> results = searchService.searchByFilter(req, PageRequest.of(page, size));

        model.addAttribute("searchedNovels", results);
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     results.getTotalPages());
        model.addAttribute("trending",       trending);
        model.addAttribute("genres",         genres);
        model.addAttribute("status",         status);

        return "client/searchPage";
    }
}
