package com.linkly.web.dto;

import java.util.List;

/** 대시보드 통계 응답. */
public record StatsResponse(
        String code,
        long totalClicks,
        List<LabelCount> byCountry,
        List<LabelCount> byDevice,
        /** 최근 24시간 시간대별(1h 버킷) 클릭 추이. */
        List<TimeBucket> timeline,
        List<ClickEventDto> recentClicks) {
}
