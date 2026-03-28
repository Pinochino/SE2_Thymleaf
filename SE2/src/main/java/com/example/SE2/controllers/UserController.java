package com.example.SE2.controllers;

import com.example.SE2.models.User;
import com.example.SE2.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UserController {
    @Autowired
    UserRepository userRepository;

    @GetMapping(value = {"/user/profile/:id"})
    public String userProfile(@PathVariable(value = "id") String id, Model model) {
        User user = userRepository.findById(id).orElse(null);
        model.addAttribute("user", user);
        return "client/user/profile";
    }

    @GetMapping(value = "/user/translation-submit")
    public String translationSubmit() {
        return "client/user/translation-submit";
    }
}
