package com.example.SE2.configs;

import com.cloudinary.Cloudinary;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        String cloudinaryUrl = resolveCloudinaryUrl();
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            throw new IllegalStateException(
                    "CLOUDINARY_URL is not configured. Set it in .env or environment variables.");
        }
        return new Cloudinary(cloudinaryUrl);
    }

    private String resolveCloudinaryUrl() {
        String value = System.getProperty("CLOUDINARY_URL");
        if (value != null && !value.isBlank()) return value;
        value = System.getenv("CLOUDINARY_URL");
        if (value != null && !value.isBlank()) return value;
        String[] candidates = {".", "./SE2", "../SE2", ".."};
        for (String dir : candidates) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(dir)
                        .ignoreIfMissing()
                        .load();
                String fromFile = dotenv.get("CLOUDINARY_URL");
                if (fromFile != null && !fromFile.isBlank()) {
                    return fromFile;
                }
            } catch (Exception ignored) {
                throw new RuntimeException("Failed to load .env from directory: " + dir, ignored);
            }
        }
        return null;
    }
}
