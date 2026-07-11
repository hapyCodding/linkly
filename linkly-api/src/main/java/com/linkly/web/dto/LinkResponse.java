package com.linkly.web.dto;

import com.linkly.domain.Link;
import java.time.Instant;

/** 링크 응답 (생성/목록 공통). */
public record LinkResponse(
        String code,
        String longUrl,
        String title,
        String shortUrl,
        long clickCount,
        Instant createdAt,
        Instant expiresAt,
        boolean expired) {

    public static LinkResponse of(Link link, String baseUrl) {
        return new LinkResponse(
                link.getCode(),
                link.getLongUrl(),
                link.getTitle(),
                baseUrl + "/x/" + link.getCode(),
                link.getClickCount(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.isExpired());
    }
}
