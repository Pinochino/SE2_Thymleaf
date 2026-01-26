package com.example.SE2.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class HomeController {

    private final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home() {

        var user = SecurityContextHolder.getContext().getAuthentication();
        return "client/home";
    }

    @RequestMapping(value = "/access-denied", method = RequestMethod.GET)
    public String accessDeniedPage() {
        return "public/accessDenied";
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
