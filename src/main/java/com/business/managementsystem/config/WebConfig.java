package com.business.managementsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves uploaded files (product images, etc.) from the local filesystem.
 * Maps /uploads/** URL paths to the configured upload directory.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${smartpos.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve to absolute path so that the file: URI works regardless
        // of the JVM working directory
        Path absPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = absPath.toString().replace("\\", "/");
        if (!location.endsWith("/")) location += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}
