package com.example.SE2.controllers;

import com.example.SE2.models.User;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.security.UserDetailImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class HomeController {

    @Autowired
    UserRepository userRepository;

    private final Logger logger = LoggerFactory.getLogger(HomeController.class);


    @RequestMapping(value = "/access-denied", method = RequestMethod.GET)
    public String accessDeniedPage() {
        return "public/accessDenied";
    }

    @RequestMapping(value = {"/"}, method = RequestMethod.GET)
    public String homePage(@AuthenticationPrincipal UserDetailImpl userDetails, Model model) {
        User user = userDetails.getUser();
        model.addAttribute("user", user);
        System.out.println("User details from SecurityContextHolder: " + user.getId());
        System.out.println(user.getEmail());
        System.out.println(user.getFirstName());

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
