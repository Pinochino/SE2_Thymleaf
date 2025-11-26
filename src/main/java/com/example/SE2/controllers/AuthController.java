package com.example.SE2.controllers;

import com.example.SE2.dtos.request.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
//@RequestMapping("${project.prefix}/auth")
public class AuthController {

    //    [GET] /api/auth/login
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest request,
                        Model model,
                        BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        model.addAttribute("error", false);
        return "auth/login";
    }


}
