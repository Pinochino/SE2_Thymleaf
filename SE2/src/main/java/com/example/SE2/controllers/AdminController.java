package com.example.SE2.controllers;

import com.example.SE2.dtos.request.NovelRequest;
import com.example.SE2.models.Genre;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.GenreRepository;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.services.file.FileService;
import com.example.SE2.services.novels.NovelServiceImpl;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {


    private final NovelRepository novelRepository;
    private final GenreRepository genreRepository;
    private final FileService fileService;
    private final NovelServiceImpl novelService;
    private final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    public AdminController(
            GenreRepository genreRepository,
            FileService fileService,
            NovelRepository novelRepository,
            NovelServiceImpl novelService) {
        this.novelRepository = novelRepository;
        this.genreRepository = genreRepository;
        this.fileService = fileService;
        this.novelService = novelService;
    }

    /**
     * NOVEL MANAGEMENT (mock data - design pages)
     */
    @GetMapping(path = "/novels/list")
    public String listNovels(Model model) {

        List<Novel> novels = novelRepository.findAll();
        List<Genre> genres = genreRepository.findAll();
        model.addAttribute("novel", new Novel());
        model.addAttribute("novels", novels);
        model.addAttribute("genres", genres);

        return "admin/novel-management";
    }

    @GetMapping("/novels/create")
    public String createNovel(Model model) {
        model.addAttribute("novel", new Novel());
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/novel-create";
    }

    @PostMapping("/novels/add")
    public String addNovel(@Valid @ModelAttribute("novel") Novel novelRequest,
                           BindingResult bindingResult,
                           @RequestParam("image") MultipartFile file,
                           Model model) {

        if (bindingResult.hasErrors()) {
            return "admin/novel-create";
        }


        String filePath = fileService.store(file);

        Novel newNovel = new Novel();
        Set<Genre> genres = new HashSet<>();

        for (Genre genre : novelRequest.getGenres()) {
            Genre oldGenre = genreRepository.getById(genre.getId());
            genres.add(oldGenre);
        }

        BeanUtils.copyProperties(novelRequest, newNovel);

        newNovel.setCoverImgUrl(filePath);

        newNovel.setGenres(genres);
        novelRepository.save(newNovel);

        try {
            novelService.indexNovel(newNovel);
        } catch (Exception e) {
            logger.warn("Failed to index novel vector: {}", e.getMessage());
        }

        return "redirect:/admin/novels/list";
    }

    @GetMapping("/novels/delete/{id}")
    @Transactional
    public String deleteNovel(@PathVariable long id, Model model) {
        Novel novel = novelRepository.getById(id);

        if (novel == null) {
            model.addAttribute("error", "No such novel");
            return "redirect:/admin/novels/list";
        }

        novel.getGenres().forEach(g -> g.getNovels().remove(novel));
        novel.getGenres().clear();

        novelRepository.delete(novel);
        return "redirect:/admin/novels/list";
    }

    @GetMapping("/novels/reindex")
    @ResponseBody
    @Transactional
    public String reindexAllNovels() {
        List<Novel> novels = novelRepository.findAll();
        int success = 0, failed = 0;
        for (Novel novel : novels) {
            try {
                novelService.indexNovel(novel);
                success++;
                logger.info("Indexed novel [{}]: {}", novel.getId(), novel.getTitle());
            } catch (Exception e) {
                failed++;
                logger.error("Failed to index novel [{}]: {}", novel.getId(), e.getMessage(), e);
            }
        }
        return String.format("Reindex complete: %d success, %d failed, %d total", success, failed, novels.size());
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
