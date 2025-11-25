package com.example.SE2.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("${project.prefix}/auth")
public class AuthController {

//    [GET] /api/auth/login
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }



}
