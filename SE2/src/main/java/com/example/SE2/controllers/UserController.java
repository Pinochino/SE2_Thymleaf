package com.example.SE2.controllers;

import com.example.SE2.models.Favorite;
import com.example.SE2.models.Novel;
import com.example.SE2.models.Translation;
import com.example.SE2.models.User;
import com.example.SE2.repositories.FavoriteRepository;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.repositories.TranslationRepository;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.security.UserDetailImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class UserController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    NovelRepository novelRepository;
    @Autowired
    FavoriteRepository favoriteRepository;
    @Autowired
    TranslationRepository translationRepository;

    @GetMapping(value = {"/user/profile"})
    public String userProfile(@AuthenticationPrincipal UserDetailImpl userDetails, Model model) {
        User user = userDetails.getUser();
        model.addAttribute("user", user);
        return "client/user/profile";
    }

    @PostMapping("/user/profile/save")
    public String saveUserProfile(@AuthenticationPrincipal UserDetailImpl userDetails,
                                  @ModelAttribute User userForm) {
        User currentUser = userDetails.getUser();
        currentUser.setFirstName(userForm.getFirstName());
        currentUser.setLastName(userForm.getLastName());
        currentUser.setUsername(userForm.getUsername());
        currentUser.setEmail(userForm.getEmail());
        currentUser.setPhone(userForm.getPhone());
        userRepository.save(currentUser);
        return "redirect:/user/profile";
    }

    @GetMapping(value = "/user/favorite-novels")
    public String favoriteNovels(@AuthenticationPrincipal UserDetailImpl userDetails, Model model) {
        User user = userDetails.getUser();
        String userId = user.getId();
        List<Favorite> favoriteNovels = favoriteRepository.findFavoritesWithNovel(userId);
        model.addAttribute("favoriteNovels", favoriteNovels);
        return "client/user/favorite-novels";
    }

    @GetMapping(value = "/user/translations")
    public String userTranslations(@AuthenticationPrincipal UserDetailImpl userDetails, Model model) {
        User user = userDetails.getUser();
        String userId = user.getId();
        List<Translation> translations = translationRepository.findByAssignedBy_Id(userId);
        model.addAttribute("translations", translations);
        return "client/user/translations";
    }

    @GetMapping(value = "/user/translation-submit")
    public String translationSubmit() {
        return "client/user/translation-submit";
    }
}
