package com.example.SE2.controllers;

import com.example.SE2.dtos.request.UpdateUserRequest;
import com.example.SE2.models.User;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.security.UserDetailImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
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

    //    [GET] /admin/users/list
    @RequestMapping(value = "/users/list", method = RequestMethod.GET)
    public String userManagement(Model model) {
        List<User> user = userRepository.findAll();

        model.addAttribute("users", user);
        return "admin/user-management";
    }

    //    [POST] /admin/users/filter/is-login
    @RequestMapping(value = "/users/filter/is-login", method = RequestMethod.POST)
    public String getUsersFromSessionRegistry(Model model) {
        List<User> onlineUsers = sessionRegistry.getAllPrincipals().stream()
                .filter(u -> !sessionRegistry.getAllSessions(u, false).isEmpty())
                .map(u -> userRepository.findUserByEmail(((UserDetailImpl) u).getUsername()))
                .collect(Collectors.toList());

        model.addAttribute("users", onlineUsers);
        return "/admin/user-management";
    }

    //    [GET] /admin/users/edit/:id
    @RequestMapping(value = "/users/edit/{id}", method = RequestMethod.GET)
    public String update(@PathVariable String id, Model model) {
        User user = userRepository.getById(id);
        model.addAttribute("user", user);
        return "admin/update-user";
    }

    //    [POST] /admin/users/update/:id
    @RequestMapping(value = "/users/update/{id}", method = RequestMethod.POST)
    public String update(@PathVariable("id") String id,
                         @Valid UpdateUserRequest formUser,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            return "admin/update-user";
        }

        User updatedUser = userRepository.getById(id);

        if (updatedUser == null) {
            throw new RuntimeException("User not found");
        }

        updatedUser.setId(id);
        updatedUser.setFirstName(formUser.getFirstName());
        updatedUser.setLastName(formUser.getLastName());
        updatedUser.setEmail(formUser.getEmail());
        updatedUser.setPassword(passwordEncoder.encode(formUser.getPassword()));

        userRepository.save(updatedUser);
        return "redirect:/admin/user-management";
    }

    //    [GET] /admin/users/delete/:id
    @RequestMapping(value = "/users/delete/{id}", method = RequestMethod.DELETE)
    public String deleteUser(@PathVariable("id") String id, Model model) {
        User user = userRepository.getById(id);

        if (user == null) {
            throw new RuntimeException("User not found");
        }


        if (Objects.equals(user.getEmail(), "admin@gmail.com")) {
            throw new RuntimeException("Admin account cannot be deleted");
        }

        userRepository.delete(user);
        return "redirect:/admin/user-management";
    }


}
