package com.example.SE2.controllers;

import com.example.SE2.models.Novel;
import com.example.SE2.models.User;
import com.example.SE2.security.UserDetailImpl;
import com.example.SE2.services.novels.NovelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
public class HomeController {

    private final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private NovelService novelService;

    @RequestMapping(value = "/access-denied", method = RequestMethod.GET)
    public String accessDeniedPage() {
        return "public/accessDenied";
    }

    @RequestMapping(value = { "/", "/home" }, method = RequestMethod.GET)
    public String homePage(@AuthenticationPrincipal UserDetailImpl userDetails, Model model) {

        User user = (userDetails != null) ? userDetails.getUser() : null;

        Page<Novel> trending = novelService.getTrendingNovels(0, 6);
        Page<Novel> recentUpdates = novelService.getRecentNovels(0, 6);

        model.addAttribute("user", user);
        model.addAttribute("trendingNovels", trending.getContent());
        model.addAttribute("recentUpdateNovels", recentUpdates.getContent());

        if (!trending.isEmpty()) {
            model.addAttribute("heroNovel", trending.getContent().get(0));
        }

        // Chỉ load dữ liệu cần login khi đã đăng nhập
        if (user != null) {
            List<Novel> favorites = novelService.getFavoriteNovels(user.getId());
            List<Novel> currentlyReading = novelService.getCurrentlyReadingNovels(user.getId());
            model.addAttribute("favoriteNovels", favorites);
            model.addAttribute("currentlyReading", currentlyReading);
        } else {
            model.addAttribute("favoriteNovels", List.of());
            model.addAttribute("currentlyReading", List.of());
        }

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
