package com.risk.prediction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 托管所有静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果请求的是存在的静态资源（如 .js, .css, .png），直接返回
                        // 如果请求的是后端 API (/api/**)，不处理，交给 Controller
                        // 其他情况（如 Vue 路由路径 /predict, /history），一律返回 index.html
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        } else if (resourcePath.startsWith("api/")) {
                            return null;
                        } else {
                            return location.createRelative("index.html");
                        }
                    }
                });
    }
}
