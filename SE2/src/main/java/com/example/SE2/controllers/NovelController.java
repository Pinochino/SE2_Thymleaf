package com.example.SE2.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/novels")
public class NovelController {

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable int id, Model model) {
        return "client/novel/NovelInformation";
    }


}
