package com.example.SE2.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/books")
public class BookController {

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String list() {
        return "admin/book-management";
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(@RequestParam(value = "error", required = false) String error,
                         Model model) {

        return "admin/book-create";
    }


}
