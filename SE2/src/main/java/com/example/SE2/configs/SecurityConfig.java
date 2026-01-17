package com.example.SE2.configs;


import com.example.SE2.security.oauth2.CustomOAuth2User;
import com.example.SE2.security.oauth2.CustomOAuth2UserService;
import com.example.SE2.services.users.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_WHITELIST = {"/images/**", "/css/**", "/js/**", "/WEB-INF/views/**", "/login", "/register", "/favicon.ico", "/oauth/**"};

    private CustomOAuth2UserService oAuth2UserService;
    private UserService userService;

    @Autowired
    public SecurityConfig(CustomOAuth2UserService oAuth2UserService,
                          UserService userService) {
        this.oAuth2UserService = oAuth2UserService;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(PUBLIC_WHITELIST).permitAll()
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                        .anyRequest().authenticated())
                .cors(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
//                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(form ->
                                form
                                        .loginPage("/login")
                                        .usernameParameter("email")
                                        .passwordParameter("password")
//                                .successHandler(successHandler)
                )
                .logout(Customizer.withDefaults())
                .oauth2Login(login -> login.loginPage("/login")
                        .userInfoEndpoint(info ->info.userService(oAuth2UserService))
                        .successHandler(new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                                Authentication authentication) throws IOException, ServletException {

                                CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();

                                userService.processOAuthPostLogin(oauthUser.getEmail());

                                response.sendRedirect("/");
                            }
                        })
                )
                .sessionManagement(session ->
                        session.maximumSessions(10)
                                .sessionRegistry(sessionRegistry()))
                .exceptionHandling(exception -> exception.accessDeniedPage("/access-denied"))
        ;
        return http.build();
    }


    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
