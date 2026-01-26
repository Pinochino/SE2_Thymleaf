package com.example.SE2.controllers;

import com.example.SE2.constants.Provider;
import com.example.SE2.constants.RoleName;
import com.example.SE2.dtos.request.LoginRequest;
import com.example.SE2.dtos.request.MailRequest;
import com.example.SE2.dtos.request.RegisterRequest;
import com.example.SE2.models.Role;
import com.example.SE2.models.User;
import com.example.SE2.repositories.RoleRepository;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.services.mail.MailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
//@RequestMapping("${project.prefix}/auth")
public class AuthController {

    private final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Autowired
    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository,
                          MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.mailService = mailService;
    }

    //    [GET] /api/auth/login
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(@RequestParam(value = "error", required = false) String error,
                        Model model) {
        model.addAttribute("loginRequest", new LoginRequest());

        log.warn("Login request received: {}", error);

        if (error != null) {
            model.addAttribute("loginError", "Invalid email or password");
        }
        return "auth/login";
    }

    @RequestMapping(value = "/perform-login", method = RequestMethod.POST)
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest request,
                        BindingResult bindingResult,
                        Model model
    ) {

        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        User oldUser = userRepository.findUserByEmail(request.getEmail());

        if (oldUser == null) {
            throw new RuntimeException("Invalid email or password");
        }

        oldUser.setLoggedIn(true);
        oldUser.setProvider(Provider.LOCAL);
        userRepository.save(oldUser);

        return "redirect:/";
    }


    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           Model model
    ) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        User oldUser = userRepository.findUserByEmail(request.getEmail());

        if (oldUser != null) {
            throw new RuntimeException("Email already in use");
        }

        Role role = roleRepository.findRoleByName(RoleName.USER);

        if (role == null) {
            throw new RuntimeException("Role not found");
        }

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        oldUser = new User();
        oldUser.setFirstName(request.getFirstName());
        oldUser.setLastName(request.getLastName());
        oldUser.setEmail(request.getEmail());
        oldUser.setPassword(passwordEncoder.encode(request.getPassword()));
        oldUser.setRoles(roles);
        oldUser.setPhone(request.getPhone());

        userRepository.save(oldUser);

        Map<String, Object> objectMap = new HashMap<String, Object>();
        objectMap.put("name", oldUser.getEmail());
        objectMap.put("content", "<p>This is a <strong>complex</strong> email with HTML content and CSS styling.</p>");

        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(request.getEmail());
        mailRequest.setSubject("Registration Confirmation");
        mailRequest.setTemplateName("email-template");
        mailRequest.setModel(objectMap);


        mailService.sendMail(mailRequest);

        return "redirect:/login";
    }


}
