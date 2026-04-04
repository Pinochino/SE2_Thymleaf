package com.example.SE2.controllers;

import com.example.SE2.models.Novel;
import com.example.SE2.models.User;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.security.UserDetailImpl;
import com.example.SE2.services.novels.NovelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class HomeController {
    @Autowired
    NovelRepository novelRepository;

    private final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private NovelService novelService;

    @RequestMapping(value = "/access-denied", method = RequestMethod.GET)
    public String accessDeniedPage() {
        return "public/accessDenied";
    }

    @RequestMapping(value = {"/", "/home"}, method = RequestMethod.GET)
    public String homePage(@AuthenticationPrincipal UserDetailImpl userDetails, Model model) {
        Page<Novel> trending = novelService.getTrendingNovels(0, 6);
        Page<Novel> recentUpdates = novelService.getRecentNovels(0, 6);

        model.addAttribute("trendingNovels", trending.getContent());
        model.addAttribute("recentUpdateNovels", recentUpdates.getContent());

        if (userDetails != null) {
            User user = userDetails.getUser();
            model.addAttribute("user", user);
            model.addAttribute("favoriteNovels", novelService.getFavoriteNovels(user.getId()));
            model.addAttribute("currentlyReading", novelService.getCurrentlyReadingNovels(user.getId()));
        }

        if (!trending.isEmpty()) {
            model.addAttribute("heroNovel", trending.getContent().get(0));
        }

        return "client/homePage";
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String searchPage(@RequestParam(value = "genre", required = false) String genre,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "12") int size,
                             Model model) {
        Page<Novel> novelPage;
        if (genre != null && !genre.isEmpty()) {
            try {
                com.example.SE2.constants.GenreName genreName = com.example.SE2.constants.GenreName.valueOf(genre);
                novelPage = novelRepository.findByGenreName(genreName, PageRequest.of(page, size));
            } catch (IllegalArgumentException e) {
                novelPage = novelRepository.findAllNovels(PageRequest.of(page, size));
            }
            model.addAttribute("selectedGenre", genre);
        } else {
            novelPage = novelRepository.findAllNovels(PageRequest.of(page, size));
        }
        model.addAttribute("novels", novelPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", novelPage.getTotalPages());
        model.addAttribute("totalResults", novelPage.getTotalElements());
        model.addAttribute("baseUrl", "/search");
        model.addAttribute("extraParams", genre != null ? "genre=" + genre : "");

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
