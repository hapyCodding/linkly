package com.linkly.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Component;

/** 클릭 요청 헤더에서 디바이스/국가/리퍼러를 간이 추정한다. */
@Component
public class RequestMetaExtractor {

    private static final List<String> BOT_HINTS =
            List.of("bot", "crawl", "spider", "slurp", "curl", "wget", "python-requests");

    public String device(HttpServletRequest request) {
        String uaRaw = request.getHeader("User-Agent");
        if (uaRaw == null) {
            return "unknown";
        }
        String ua = uaRaw.toLowerCase();
        if (BOT_HINTS.stream().anyMatch(ua::contains)) {
            return "bot";
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "tablet";
        }
        if (ua.contains("mobi") || ua.contains("android") || ua.contains("iphone")) {
            return "mobile";
        }
        if (ua.isBlank()) {
            return "unknown";
        }
        return "desktop";
    }

    /**
     * Accept-Language 의 지역 서브태그로 국가를 추정한다. (예: ko-KR -> KR)
     * 정밀 IP 지오로케이션은 데모 범위 밖이라 언어 힌트로 대체.
     */
    public String country(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header == null) {
            return null;
        }
        String lang = header.split(",")[0].trim();
        String[] parts = lang.split("-");
        if (parts.length < 2) {
            return null;
        }
        String region = parts[1];
        if (region.length() > 2) {
            region = region.substring(0, 2);
        }
        region = region.toUpperCase();
        if (region.length() == 2 && region.chars().allMatch(Character::isLetter)) {
            return region;
        }
        return null;
    }

    public String referer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) {
            return null;
        }
        return referer.length() > 255 ? referer.substring(0, 255) : referer;
    }
}
