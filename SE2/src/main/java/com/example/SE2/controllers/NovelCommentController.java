package com.example.SE2.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/comments")
public class NovelCommentController {

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String addComment(@RequestParam String title, @RequestParam String content) {
        return "redirect:/chapters/" + title;
    }

}
