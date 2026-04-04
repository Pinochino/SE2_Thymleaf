package com.example.SE2.controllers;

import com.example.SE2.constants.TranslationStatus;
import com.example.SE2.models.*;
import com.example.SE2.repositories.*;
import com.example.SE2.security.UserDetailImpl;
import com.example.SE2.services.chapter.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    @Autowired
    ChapterService chapterService;
    @Autowired
    ChapterRepository chapterRepository;

    @GetMapping(value = {"/user/profile"})
    public String userProfile(@AuthenticationPrincipal UserDetailImpl userDetails, Model model) {
        User user = userDetails.getUser();
        model.addAttribute("user", user);
        return "client/user/profile";
    }

    @PostMapping("/user/profile/avatar")
    public String updateAvatar(@AuthenticationPrincipal UserDetailImpl userDetails,
                               @RequestParam String avatarUrl) {
        User currentUser = userDetails.getUser();
        currentUser.setAvatarUrl(avatarUrl);
        userRepository.save(currentUser);
        return "redirect:/user/profile";
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
    public String favoriteNovels(
            @AuthenticationPrincipal UserDetailImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {
        User user = userDetails.getUser();
        String userId = user.getId();

        Pageable pageable = PageRequest.of(page, size);
        Page<Favorite> favoritePage = favoriteRepository.findFavoritesWithNovel(userId, pageable);

        model.addAttribute("favoriteNovels", favoritePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", favoritePage.getTotalPages());
        return "client/user/favorite-novels";
    }

    @GetMapping(value = "/user/translations")
    public String userTranslations(@AuthenticationPrincipal UserDetailImpl userDetails, Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "12") int size) {
        User user = userDetails.getUser();
        String userId = user.getId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Translation> translationPage = translationRepository.findByAssignedBy_Id(userId, pageable);
        
        model.addAttribute("translations", translationPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", translationPage.getTotalPages());
        return "client/user/translations";
    }

    @GetMapping(value = "/user/submit-translation")
    public String translationSubmit(@AuthenticationPrincipal UserDetailImpl userDetails,
                                    @RequestParam(required = false) Long novelId,
                                    @RequestParam(required = false) Long chapterId,
                                    Model model) {
        // Load all novels for the dropdown
        List<Novel> novels = novelRepository.findAll();
        model.addAttribute("novels", novels);

        // If novelId is provided, load its chapters
        if (novelId != null) {
            Novel novel = novelRepository.findById(novelId).orElse(null);
            model.addAttribute("selectedNovel", novel);
            if (novel != null) {
                List<Chapter> chapters = chapterService.getChaptersByNovelId(novelId);
                model.addAttribute("chapters", chapters);
            }
        }

        if (chapterId != null) {
            model.addAttribute("selectedChapterId", chapterId);
        }

        return "client/user/translation-submit";
    }

    @PostMapping(value = "/user/submit-translation")
    public String submitTranslation(@AuthenticationPrincipal UserDetailImpl userDetails,
                                    @RequestParam Long chapterId,
                                    @RequestParam String content) {
        User user = userDetails.getUser();
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null || content == null || content.trim().isEmpty()) {
            return "redirect:/user/submit-translation";
        }

        Translation translation = new Translation();
        translation.setChapter(chapter);
        translation.setAssignedBy(user);
        translation.setContent(content.trim());
        translation.setStatus(TranslationStatus.PENDING);
        translationRepository.save(translation);

        return "redirect:/user/translations";
    }
}
