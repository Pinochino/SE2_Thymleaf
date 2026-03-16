package com.example.SE2.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final GlobalModelInterceptor globalModelInterceptor;

    @Autowired
    public WebMvcConfig(GlobalModelInterceptor globalModelInterceptor) {
        this.globalModelInterceptor = globalModelInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(globalModelInterceptor);
    }
}
