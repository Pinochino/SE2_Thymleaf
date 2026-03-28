package com.example.SE2.controllers;

import com.example.SE2.models.Novel;
import com.example.SE2.models.NovelComment;
import com.example.SE2.models.User;
import com.example.SE2.repositories.NovelCommentRepository;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.security.UserDetailImpl;
import com.example.SE2.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/novels")
public class NovelController {

    private NovelRepository novelRepository;
    private NovelCommentRepository novelCommentRepository;
    private final Logger logger = LoggerFactory.getLogger(NovelController.class);


    @Autowired
    public NovelController(NovelRepository novelRepository,
                           NovelCommentRepository novelCommentRepository) {
        this.novelRepository = novelRepository;
        this.novelCommentRepository = novelCommentRepository;
    }

    @RequestMapping(value = "/information/{publicId}", method = RequestMethod.GET)
    public String novelDetail(@PathVariable String publicId,
                              Model model,
                              @AuthenticationPrincipal UserDetailImpl user) {

        Novel novel = novelRepository.findNovelByPublicId(UUID.fromString(publicId));

        // Chỉ lấy comment gốc (không có parent)
        List<NovelComment> novelComments = novelCommentRepository
                .findByNovelAndParentCommentIsNullOrderByCreatedAtDesc(novel);

        model.addAttribute("novel", novel);
        model.addAttribute("novelComment", new NovelComment());
        model.addAttribute("userLogin", user);
        model.addAttribute("novelComments", novelComments);
        model.addAttribute("timeUtils", new TimeUtils()); // Chuyển ra ngoài loop

        return "client/novel-detail";
    }

    @GetMapping("/chapter")
    public String readChapter() {
        return "client/chapter";
    }


}
