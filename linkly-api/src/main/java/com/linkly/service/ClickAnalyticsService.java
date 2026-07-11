package com.linkly.service;

import com.linkly.domain.Link;
import com.linkly.repository.ClickEventRepository;
import com.linkly.repository.LinkRepository;
import com.linkly.web.dto.ClickEventDto;
import com.linkly.web.dto.LabelCount;
import com.linkly.web.dto.StatsResponse;
import com.linkly.web.dto.TimeBucket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickAnalyticsService {

    private final LinkRepository linkRepository;
    private final ClickEventRepository clickEventRepository;

    public ClickAnalyticsService(
            LinkRepository linkRepository, ClickEventRepository clickEventRepository) {
        this.linkRepository = linkRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public StatsResponse statsFor(String code) {
        Link link =
                linkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));

        List<LabelCount> byCountry =
                clickEventRepository.countByCountry(link.getId()).stream()
                        .map(c -> new LabelCount(c.getLabel() == null ? "ZZ" : c.getLabel(), c.getCount()))
                        .toList();
        List<LabelCount> byDevice =
                clickEventRepository.countByDevice(link.getId()).stream()
                        .map(c -> new LabelCount(c.getLabel() == null ? "unknown" : c.getLabel(), c.getCount()))
                        .toList();

        List<ClickEventDto> recent =
                clickEventRepository.findTop20ByLinkIdOrderByClickedAtDesc(link.getId()).stream()
                        .map(e -> ClickEventDto.of(e, code, link.getClickCount()))
                        .toList();

        return new StatsResponse(
                code,
                link.getClickCount(),
                byCountry,
                byDevice,
                hourlyTimeline(link.getId()),
                recent);
    }

    /** 최근 24시간을 1시간 버킷으로 나눈 클릭 추이 (빈 시간대는 0으로 채움). */
    private List<TimeBucket> hourlyTimeline(Long linkId) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant start = now.minus(23, ChronoUnit.HOURS);
        List<Instant> timestamps = clickEventRepository.clickTimestampsSince(linkId, start);

        Map<Instant, Long> counts =
                timestamps.stream()
                        .collect(
                                Collectors.groupingBy(
                                        t -> t.truncatedTo(ChronoUnit.HOURS),
                                        Collectors.counting()));

        List<TimeBucket> result = new ArrayList<>();
        for (int h = 0; h <= 23; h++) {
            Instant bucket = start.plus(h, ChronoUnit.HOURS);
            result.add(new TimeBucket(bucket, counts.getOrDefault(bucket, 0L)));
        }
        return result;
    }
}
