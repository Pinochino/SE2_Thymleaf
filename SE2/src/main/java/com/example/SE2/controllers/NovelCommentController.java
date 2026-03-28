package com.example.SE2.controllers;

import com.example.SE2.models.Novel;
import com.example.SE2.models.NovelComment;
import com.example.SE2.models.User;
import com.example.SE2.repositories.NovelCommentRepository;
import com.example.SE2.repositories.NovelRepository;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.security.UserDetailImpl;
import com.example.SE2.utils.TimeUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/comments")
public class NovelCommentController {

    private final NovelRepository novelRepository;
    private final NovelCommentRepository novelCommentRepository;
    private final UserRepository userRepository;

    @Autowired
    public NovelCommentController(NovelRepository novelRepository, NovelCommentRepository novelCommentRepository, UserRepository userRepository) {
        this.novelRepository = novelRepository;
        this.novelCommentRepository = novelCommentRepository;
        this.userRepository = userRepository;
    }


    @RequestMapping(value = "/save/{publicId}", method = RequestMethod.POST)
    public String saveNovelComment(@PathVariable() String publicId,
                                   @RequestParam(value = "parentId", required = false) Long parentId,
                                   @Valid @RequestParam("content") String content,
                                   @AuthenticationPrincipal UserDetailImpl userDetail,
                                   Model model) {

//        GET USER LOGIN
        User user = userDetail.getUser();

        if (user == null) {
            model.addAttribute("error", "You are not logged in");
        }


//        GET NOVEL
        Novel novel = novelRepository.findNovelByPublicId(UUID.fromString(publicId));

        NovelComment newNovelComment = new NovelComment();
        newNovelComment.setContent(content);
        newNovelComment.setUser(user);
        newNovelComment.setNovel(novel);

        if (parentId != null) {
            NovelComment oldNovelComment = novelCommentRepository.getById(parentId);
            newNovelComment.setParentComment(oldNovelComment);
        }

        novelCommentRepository.save(newNovelComment);

        return "redirect:/novels/information/{publicId}";
    }


}
