package com.example.SE2.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books")
public class BookController {

    @GetMapping("/chapter")
    public String readChapter() {
        return "client/chapter";
    }





}
