package com.example.SE2.configs;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_WHITELIST = {"/images/**", "/css/**", "/js/**", "/WEB-INF/views/**", "/login", "/register", "/favicon.ico"};


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
