package com.example.SE2.controllers;

import com.example.SE2.constants.Provider;
import com.example.SE2.constants.RoleName;
import com.example.SE2.dtos.request.LoginRequest;
import com.example.SE2.dtos.request.RegisterRequest;
import com.example.SE2.models.Role;
import com.example.SE2.models.User;
import com.example.SE2.repositories.RoleRepository;
import com.example.SE2.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@Controller
//@RequestMapping("${project.prefix}/auth")
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
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

        oldUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .roles(roles)
                .build();

        userRepository.save(oldUser);
        return "redirect:/login";
    }


}
