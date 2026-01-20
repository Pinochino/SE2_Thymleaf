package com.example.SE2.controllers;

import com.example.SE2.dtos.request.MailRequest;
import com.example.SE2.models.User;
import com.example.SE2.services.mail.MailService;
import com.example.SE2.services.users.UserService;
import com.example.SE2.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ForgotPasswordController {

    MailService mailService;
    UserService userService;

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(HttpServletRequest request,
                                        Model model) {
        String email = request.getParameter("email");
        String token = UUID.randomUUID().toString();

        try {
            userService.updateResetPasswordToken(email, token);
            String resetPasswordLink = AuthUtil.getSiteUrl(request) + "/reset-password?token=" + token;

            Map<String, Object> objectMap = new HashMap<String, Object>();
            objectMap.put("resetPasswordLink", resetPasswordLink);

            MailRequest mailRequest = MailRequest.builder()
                    .to(email)
                    .subject("Reset Password")
                    .templateName("email-reset-password")
                    .body(resetPasswordLink)
                    .model(objectMap)
                    .build();
            mailService.sendMail(mailRequest);
            model.addAttribute("message", "We have sent a reset password link to your email. Please check your email address and try again.");
        } catch (Exception e) {
            model.addAttribute("message", e.getMessage());
        }

        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(@Param("token") String token, Model model) {
        User user = userService.getByResetPasswordToken(token);
        model.addAttribute("token", token);

        if (user == null) {
            model.addAttribute("message", "Invalid token");
            return "message";
        }

        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(HttpServletRequest request,
                                       Model model) {
        String token = request.getParameter("token");
        String password = request.getParameter("password");

        User user = userService.getByResetPasswordToken(token);
        model.addAttribute("title", "Reset Password");

        if (user == null) {
            model.addAttribute("message", "Invalid token");
            return "message";
        } else {
            userService.updatePassword(user, password);
            model.addAttribute("message", "Password changed successfully");
        }

        return "message";

    }


}
