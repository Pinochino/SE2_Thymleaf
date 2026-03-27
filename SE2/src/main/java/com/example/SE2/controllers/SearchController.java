package com.example.SE2.controllers;

import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Novel;
import com.example.SE2.services.novels.NovelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private NovelService novelService;

    @GetMapping
    public String searchPage(
            @RequestParam(required = false)          String            keyword,
            @RequestParam(required = false)          List<String>      genres,
            @RequestParam(required = false)          List<NovelStatus> statuses,
            @RequestParam(required = false)          Boolean           trending,
            @RequestParam(defaultValue = "0")        int               page,
            @RequestParam(defaultValue = "20")       int               size,
            Model model
    ) {
        Page<Novel> results = novelService.searchNovels(
                keyword, genres, statuses, trending, page, size
        );

        // search results
        model.addAttribute("novels",      results.getContent());
        model.addAttribute("currentPage", results.getNumber());
        model.addAttribute("totalPages",  results.getTotalPages());
        model.addAttribute("totalItems",  results.getTotalElements());
        model.addAttribute("hasNext",     results.hasNext());
        model.addAttribute("hasPrev",     results.hasPrevious());

        //  preserve filter state (so form stays filled after search)
        model.addAttribute("keyword",  keyword);
        model.addAttribute("genres",   genres);
        model.addAttribute("statuses", statuses);
        model.addAttribute("trending", trending);
        model.addAttribute("size",     size);

        // populate filter options for the form dropdowns
        model.addAttribute("allStatuses", NovelStatus.values());

        return "searchPage";  // → templates/searchPage.html
    }
}