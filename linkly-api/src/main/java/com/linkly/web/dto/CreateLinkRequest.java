package com.linkly.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** 링크 생성 요청. */
public record CreateLinkRequest(
        @NotBlank(message = "url은 필수입니다")
        @Pattern(
                regexp = "^https?://.+",
                message = "http:// 또는 https:// 로 시작하는 URL이어야 합니다")
        String url,

        /** 만료까지 남은 일수 (선택). */
        @Min(value = 1, message = "만료일은 1 이상이어야 합니다")
        Long expiresInDays,

        /** 정리용 태그 (선택). */
        List<String> tags) {
}
