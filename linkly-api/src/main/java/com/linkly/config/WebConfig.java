package com.linkly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** REST API에 대한 CORS 설정 (프론트 dev 서버 허용). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LinklyProperties props;

    public WebConfig(LinklyProperties props) {
        this.props = props;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.getAllowedOriginList().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
