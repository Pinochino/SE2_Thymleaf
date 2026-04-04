package com.example.SE2.controllers;

import com.example.SE2.models.Novel;
import com.example.SE2.services.embedding.EmbeddingServiceImpl;
import com.example.SE2.services.search.SearchService;
import com.example.SE2.services.search.SearchServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    SearchServiceImpl searchService;

    @Autowired
    EmbeddingServiceImpl embeddingService;

    @GetMapping({"", "/"})
    public String searchPage(){
        Float[] vector=embeddingService.embed("comedy, hello");
        System.out.println("Vector length: " + vector.length);
        for (int i = 0; i < Math.min(5, vector.length); i++) {
            System.out.println(vector[i]);
        }
        return "client/searchPage";
    }

    @PostMapping("/adwdwa")
    public String searchByVector(@RequestParam String keyword, Model model){
        List<Novel> results= searchService.searchByVector(keyword,10);
        model.addAttribute("searchedNovels", results);
        return "client/searchPage";
    }
}
