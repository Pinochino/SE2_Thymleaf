package com.example.SE2.controllers;

import com.example.SE2.services.scraper.ChapterScraperService;
import com.example.SE2.services.scraper.NovelScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    @Autowired
    private NovelScraperService scraperService;

    @Autowired
    private ChapterScraperService chapterScraperService;

    @GetMapping("/run")
    public ResponseEntity<Map<String, Object>> scrape(
            @RequestParam(defaultValue = "50") int count) {
        int saved = scraperService.scrapeAndSave(count);
        return ResponseEntity.ok(Map.of(
                "status", "done",
                "novelsSaved", saved,
                "requested", count
        ));
    }

    @GetMapping("/chapters")
    public ResponseEntity<Map<String, Object>> scrapeChapters(
            @RequestParam(defaultValue = "20") int maxBooks) {
        int saved = chapterScraperService.scrapeChapters(maxBooks);
        return ResponseEntity.ok(Map.of(
                "status", "done",
                "chaptersSaved", saved,
                "booksProcessed", maxBooks
        ));
    }
}
