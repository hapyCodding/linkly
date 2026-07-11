package com.linkly.web.dto;

import com.linkly.domain.ClickEvent;
import java.time.Instant;

/** 클릭 이벤트 DTO (실시간 푸시 + 최근 목록 공통). */
public record ClickEventDto(
        String code,
        Instant clickedAt,
        String country,
        String device,
        String referer,
        long totalClicks) {

    public static ClickEventDto of(ClickEvent event, String code, long totalClicks) {
        return new ClickEventDto(
                code,
                event.getClickedAt(),
                event.getCountry(),
                event.getDevice(),
                event.getReferer(),
                totalClicks);
    }
}
