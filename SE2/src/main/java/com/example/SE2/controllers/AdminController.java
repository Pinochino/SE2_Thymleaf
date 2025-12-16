package com.example.SE2.controllers;

import com.example.SE2.dtos.request.UpdateUserRequest;
import com.example.SE2.models.User;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.security.UserDetailImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    @Autowired
    public AdminController(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping("/user-management")
    public String userManagement(Model model) {
        List<User> user = userRepository.findAll();

        model.addAttribute("users", user);
        return "admin/user-management";
    }

    @PostMapping("/filter/is-login")
    public String getUsersFromSessionRegistry(Model model) {
        List<User> onlineUsers =  sessionRegistry.getAllPrincipals().stream()
                .filter(u -> !sessionRegistry.getAllSessions(u, false).isEmpty())
                .map(u -> userRepository.findUserByEmail(((UserDetailImpl) u).getUsername()))
                .collect(Collectors.toList());

        model.addAttribute("users", onlineUsers);
        return "/admin/user-management";
    }

    @GetMapping("/edit/{id}")
    public String update(@PathVariable UUID id, Model model) {
        User user = userRepository.getById(id);
        model.addAttribute("user", user);
        return "admin/update-user";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable("id") String id,
                         @Valid UpdateUserRequest formUser,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            return "admin/update-user";
        }

        User updatedUser = userRepository.getById(UUID.fromString(id));

        if (updatedUser == null) {
            throw new RuntimeException("User not found");
        }

        updatedUser.setId(UUID.fromString(id));
        updatedUser.setFirstName(formUser.getFirstName());
        updatedUser.setLastName(formUser.getLastName());
        updatedUser.setEmail(formUser.getEmail());
        updatedUser.setPassword(passwordEncoder.encode(formUser.getPassword()));

        userRepository.save(updatedUser);
        return "redirect:/admin/user-management";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") String id, Model model) {
        User user = userRepository.getById(UUID.fromString(id));

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        userRepository.delete(user);
        return "redirect:/admin/user-management";
    }


}
