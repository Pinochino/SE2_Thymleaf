package com.example.SE2.controllers;

import com.example.SE2.models.Novel;
import com.example.SE2.services.novels.NovelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class HomeController {
    @Autowired
    private NovelService novelService;

    private final Logger logger = LoggerFactory.getLogger(HomeController.class);


    @RequestMapping(value = "/access-denied", method = RequestMethod.GET)
    public String accessDeniedPage() {
        return "public/accessDenied";
    }

    @RequestMapping(value = {"/","/home"}, method = RequestMethod.GET)
    public String homePage(Model model)
    {
        int page = 0;
        int size = 6;

        Page<Novel> trending = novelService.getTrendingNovels(page, size);
        Page<Novel> recentUp = novelService.getRecentNovels(page, size);

        model.addAttribute("trendingNovel",trending);
        model.addAttribute("recentUpdateNovel",recentUp);

        return "client/homePage";
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String searchPage() {
        return "client/searchPage";
    }

    @RequestMapping(value = "/page-not-found", method = RequestMethod.GET)
    public String pageNotFound() {
        return "public/pageNotFound";
    }

    @RequestMapping(value = "error", method = RequestMethod.GET)
    public String errorPage() {
        return "public/error";
    }


}
