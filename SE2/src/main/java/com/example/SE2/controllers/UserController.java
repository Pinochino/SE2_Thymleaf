package com.example.SE2.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UserController {

    @GetMapping(value = {"/profile", "/user/profile"})
    public String userProfile() {
        return "client/user/profile";
    }

    @GetMapping(value = "/user/translation-submit")
    public String translationSubmit() {
        return "client/user/translation-submit";
    }
}
