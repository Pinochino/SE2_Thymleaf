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
    public String login(@RequestParam(value = "error", required = false) String error,
                        Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        if (error != null) {
            model.addAttribute("loginError", "Invalid email or password");
        }
        return "auth/login";
    }


    @PostMapping("/perform-login")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest request,
                        Model model,
                        BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("loginRequest", new LoginRequest());

            return "auth/login";
        }

        return "redirect:/";
    }


    @GetMapping("/register")
    public String register(Model model) {
//        model.addAttribute("registerRequest", new LoginRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") LoginRequest request,
                        Model model,
                        BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        model.addAttribute("error", false);
        return "auth/register";
    }


}
