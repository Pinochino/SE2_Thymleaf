package com.example.SE2.configs;


import com.example.SE2.security.UserDetailServiceImpl;
import com.example.SE2.security.oauth2.CustomOAuth2User;
import com.example.SE2.security.oauth2.CustomOAuth2UserService;
import com.example.SE2.services.users.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
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
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_WHITELIST = {"/images/**",
            "/css/**",
            "/js/**",
            "/WEB-INF/views/**", "/login", "/register", "/favicon.ico", "/oauth/**", "/forgot-password",
            "/reset-password"};

    private final CustomOAuth2UserService oAuth2UserService;
    private final UserService userService;
    private final UserDetailServiceImpl userDetailService;

    @Autowired
    public SecurityConfig(CustomOAuth2UserService oAuth2UserService,
                          UserService userService,
                          UserDetailServiceImpl userDetailService) {
        this.oAuth2UserService = oAuth2UserService;
        this.userService = userService;
        this.userDetailService = userDetailService;
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
                        .userInfoEndpoint(info -> info.userService(oAuth2UserService))
                        .successHandler(new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(@NotNull HttpServletRequest request,
                                                                @NotNull HttpServletResponse response,
                                                                @NotNull Authentication authentication) throws IOException, ServletException {

                                CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();

                                if (oauthUser == null) {
                                    return;
                                }

                                userService.processOAuthPostLogin(oauthUser.getEmail(), oauthUser.getName());

                                response.sendRedirect("/");
                            }
                        })
                )
                .sessionManagement(session ->
                        session.maximumSessions(10)
                                .sessionRegistry(sessionRegistry()))
                .exceptionHandling(exception -> exception.accessDeniedPage("/access-denied"))
                .rememberMe(rememberMe -> rememberMe.rememberMeParameter("remember-me")
                        .key("springRocks")
                        .userDetailsService(userDetailService))
        ;
        return http.build();
    }


    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }


//    @Bean
//    public RememberMeServices rememberMeServices(UserDetailServiceImpl userDetailService) {
//        TokenBasedRememberMeServices.RememberMeTokenAlgorithm encodingAlgorithm = TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256;
//        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices(myKey, userDetailsService, encodingAlgorithm);
//
//        return rememberMe;
//    }
}
