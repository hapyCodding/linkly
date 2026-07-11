package com.linkly.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 {@code linkly.*} 설정 바인딩. */
@ConfigurationProperties(prefix = "linkly")
public class LinklyProperties {

    /** 단축 링크/QR 생성 기준이 되는 공개 주소 (끝에 / 없이). */
    private String baseUrl = "http://localhost:8080";

    /** CORS 허용 오리진 목록 (콤마 구분). */
    private String corsAllowedOrigins = "http://localhost:5173";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public List<String> getAllowedOriginList() {
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
