package com.example.SE2.controllers;

import com.example.SE2.repositories.GenreRepository;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.services.file.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {


    @Autowired
    public AdminController(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           SessionRegistry sessionRegistry,
                           GenreRepository genreRepository,
                           FileService fileService) {

    }

    /**
     * NOVEL MANAGEMENT (mock data - design pages)
     */
    @GetMapping(path = {"/", "/novels/list"})
    public String listNovels(Model model) {
        model.addAttribute("activePage", "novels");
        return "admin/novel-management";
    }

    @GetMapping("/novels/create")
    public String createNovel(Model model) {
        model.addAttribute("activePage", "novels");
        return "admin/novel-create";
    }

    /**
     * TRANSLATION MANAGEMENT (mock data - design pages)
     */

    @GetMapping("/translations/list")
    public String listTranslations(Model model) {
        model.addAttribute("activePage", "translations");
        return "admin/translation-management";
    }

    @GetMapping("/translations/review")
    public String reviewTranslation(Model model) {
        model.addAttribute("activePage", "translations");
        return "admin/translation-review";
    }

}
