package com.example.SE2.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UserController {

    @RequestMapping(value = "/profile")
    public String userProfile() {
        return "client/user/profile";
    }
}
