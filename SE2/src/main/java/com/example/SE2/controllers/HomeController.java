package com.example.SE2.controllers;

import com.example.SE2.models.Novel;
import com.example.SE2.repositories.NovelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;


@Controller
public class HomeController {

    private final Logger logger = LoggerFactory.getLogger(HomeController.class);
    private final NovelRepository novelRepository;

    @Autowired
    public HomeController(NovelRepository novelRepository) {
        this.novelRepository = novelRepository;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String homePage(Model model) {

        List<Novel> novels = novelRepository.findAll();

        List<Novel> subList = novels.subList(1, Math.min(5, novels.size()));
        model.addAttribute("novels", subList);

        return "client/homePage";
    }

    @RequestMapping(value = "/access-denied", method = RequestMethod.GET)
    public String accessDeniedPage() {
        return "public/accessDenied";
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
