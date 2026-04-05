package com.example.SE2.configs;

import com.example.SE2.models.Genre;
import com.example.SE2.models.User;
import com.example.SE2.repositories.GenreRepository;
import com.example.SE2.security.UserDetailImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Component
public class GlobalModelInterceptor implements HandlerInterceptor {

    @Autowired
    private GenreRepository genreRepository;

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isLoggedIn = auth != null
                    && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal());

            modelAndView.addObject("isLoggedIn", isLoggedIn);
            modelAndView.addObject("loggedInUser", isLoggedIn ? auth.getName() : null);

            if (isLoggedIn && auth.getPrincipal() instanceof UserDetailImpl userDetail) {
                User user = userDetail.getUser();
                modelAndView.addObject("loggedInAvatarUrl", user.getAvatarUrl());
            }
            
            List<Genre> genres = genreRepository.findAll();
            modelAndView.addObject("headerGenres", genres);
        }
    }
}
