package com.example.SE2.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
//@RequestMapping("${project.prefix}")
@Slf4j
public class HomeController {

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home() {

        var user = SecurityContextHolder.getContext().getAuthentication();
        log.info("User: " + user);
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
