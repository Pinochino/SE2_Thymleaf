package com.example.SE2.controllers;

import com.example.SE2.dtos.request.BookRequest;
import com.example.SE2.dtos.request.CategoryRequest;
import com.example.SE2.dtos.request.UpdateUserRequest;
import com.example.SE2.models.Book;
import com.example.SE2.models.Category;
import com.example.SE2.models.User;
import com.example.SE2.repositories.BookRepository;
import com.example.SE2.repositories.CategoryRepository;
import com.example.SE2.repositories.UserRepository;
import com.example.SE2.security.UserDetailImpl;
import com.example.SE2.services.file.FileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final FileService fileService;

    @Autowired
    public AdminController(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           SessionRegistry sessionRegistry,
                           BookRepository bookRepository,
                           CategoryRepository categoryRepository,
                           FileService fileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.fileService = fileService;
    }

    /**
     * USER
     */

    //    [GET] /admin/users/list
    @RequestMapping(value = "/users/list", method = RequestMethod.GET)
    public String listUser(Model model) {
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
    public String updateUser(@PathVariable String id, Model model) {
        User user = userRepository.getById(id);
        model.addAttribute("user", user);
        return "admin/update-user";
    }

    //    [POST] /admin/users/update/:id
    @RequestMapping(value = "/users/update/{id}", method = RequestMethod.POST)
    public String updateUser(@PathVariable("id") String id,
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


    /**
     * BOOK
     */

    @RequestMapping(value = "/books/list", method = RequestMethod.GET)
    public String listBook(Model model) {
        List<Book> books = bookRepository.findAll();
        model.addAttribute("books", books);
        return "admin/book-management";
    }

    @RequestMapping(value = "/books/create", method = RequestMethod.GET)
    public String createBook(@RequestParam(value = "error", required = false) String error,
                             Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);

        model.addAttribute("bookRequest", new BookRequest());
        return "admin/book-create";
    }

    @RequestMapping(value = "/books/perform-create", method = RequestMethod.POST)
    public String createBook(
            @Valid @ModelAttribute("bookRequest") BookRequest bookRequest,
            BindingResult bindingResult,
            Model model) {

        MultipartFile file = bookRequest.getImageFile();

        String savedPath = null;
        String fileName = null;

        if (file != null && !file.isEmpty()) {
            savedPath = fileService.store(file);
            fileName = Paths.get(savedPath).getFileName().toString();
        }

        if (bindingResult.hasErrors()) {
            return "admin/book-create";
        }

        Book book = bookRepository.findBookByTitle(bookRequest.getTitle());

        if (book != null) {
            throw new RuntimeException("Book have already been created");
        }


        Category category = categoryRepository.getById(bookRequest.getCategoryId());

        if (category == null) {
            throw new RuntimeException("Category does not exist");
        }

        Set<Category> categories = new HashSet<>();
        categories.add(category);

        Book newBook = new Book();
        newBook.setTitle(bookRequest.getTitle());
        newBook.setDescription(bookRequest.getDescription());
        newBook.setAuthor(bookRequest.getAuthor());
        newBook.setImage(fileName);
        newBook.setCategories(categories);
        bookRepository.save(newBook);

        return "redirect:/admin/books/list";
    }

    @RequestMapping(value = "/books/delete/{id}", method = RequestMethod.DELETE)
    public String deleteBook(@PathVariable("id") Long id, Model model) {
        Book book = bookRepository.getById(id);

        if (book == null) {
            throw new RuntimeException("Book not found");
        }

        bookRepository.delete(book);
        return "redirect:/admin/books/list";
    }

    /**
     * CATEGORY
     */

    @RequestMapping(value = "/categories/create", method = RequestMethod.GET)
    public String createCategory(Model model) {
        model.addAttribute("category", new CategoryRequest());
        return "admin/category-create";
    }

    @RequestMapping(value = "categories/perform-create", method = RequestMethod.POST)
    public String createCategory(@Valid @ModelAttribute("categoryRequest") CategoryRequest categoryRequest,
                                 BindingResult bindingResult,
                                 Model model) {
        Category category = categoryRepository.findCategoryByName(categoryRequest.getName());

        if (category != null) {
            throw new RuntimeException("Category have already been created");
        }

        Category newCategory = new Category();
        newCategory.setName(categoryRequest.getName());
        newCategory.setDescription(categoryRequest.getDescription());
        categoryRepository.save(newCategory);

        return "redirect:/admin/categories/list";
    }

    @RequestMapping(value = "/categories/list", method = RequestMethod.GET)
    public String listCategory(Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return "admin/category-management";
    }


}
