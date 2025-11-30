package com.example.SE2.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {"/images/**", "/css/**", "/js/**", "/WEB-INF/views/**",  "/login", "/register", "/perform-login"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize.requestMatchers(AUTH_WHITELIST)
                .permitAll().anyRequest().authenticated())
                .cors(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
                .formLogin(form ->
                        form.loginPage("/login")
                                .loginProcessingUrl("/perform-login")
                                .permitAll()).logout(Customizer.withDefaults());
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
