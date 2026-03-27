package com.example.SE2.controllers;

import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/novels")
public class NovelController {

    private NovelRepository novelRepository;
    private final Logger logger = LoggerFactory.getLogger(NovelController.class);


    @Autowired
    public NovelController(NovelRepository novelRepository) {
        this.novelRepository = novelRepository;
    }

    @RequestMapping(value = "/information/{publicId}", method = RequestMethod.GET)
    public String novelDetail(@PathVariable String publicId, Model model) {

        Novel novel = novelRepository.findNovelByPublicId(UUID.fromString(publicId));

        logger.info("Novel found: " + novel.getTitle());

        model.addAttribute("novel", novel);

        return "client/novel-detail";
    }

    @GetMapping("/chapter")
    public String readChapter() {
        return "client/chapter";
    }


}
