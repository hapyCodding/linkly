package com.linkly.service;

import com.linkly.config.LinklyProperties;
import com.linkly.domain.ClickEvent;
import com.linkly.domain.Link;
import com.linkly.repository.ClickEventRepository;
import com.linkly.repository.LinkRepository;
import com.linkly.web.dto.ClickEventDto;
import com.linkly.web.dto.CreateLinkRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final ClickEventRepository clickEventRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final TitleFetcher titleFetcher;
    private final SimpMessagingTemplate messaging;
    private final LinklyProperties props;

    public LinkService(
            LinkRepository linkRepository,
            ClickEventRepository clickEventRepository,
            ShortCodeGenerator shortCodeGenerator,
            TitleFetcher titleFetcher,
            SimpMessagingTemplate messaging,
            LinklyProperties props) {
        this.linkRepository = linkRepository;
        this.clickEventRepository = clickEventRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.titleFetcher = titleFetcher;
        this.messaging = messaging;
        this.props = props;
    }

    // @Transactional 없이: 대상 페이지 title 수집(네트워크 I/O)을 DB 트랜잭션 밖에서 처리.
    public Link create(CreateLinkRequest request) {
        String code = generateUniqueCode();
        Instant expiresAt =
                request.expiresInDays() != null
                        ? Instant.now().plus(request.expiresInDays(), ChronoUnit.DAYS)
                        : null;
        Link link = new Link(code, request.url(), expiresAt);
        link.setTitle(titleFetcher.fetchTitle(request.url()));
        link.setTags(normalizeTags(request.tags()));
        return linkRepository.save(link);
    }

    /** 링크의 태그를 전체 교체한다. */
    @Transactional
    public Link updateTags(String code, List<String> tags) {
        Link link =
                linkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        link.setTags(normalizeTags(tags));
        return linkRepository.save(link);
    }

    /** 태그 정규화: 공백 제거, 빈 값 제외, 길이 30 제한, 최대 20개, 중복 제거(순서 유지). */
    private Set<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return new LinkedHashSet<>();
        }
        return tags.stream()
                .filter(t -> t != null)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(t -> t.length() > 30 ? t.substring(0, 30) : t)
                .limit(20)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 링크 메모를 수정한다 (빈 값이면 null). */
    @Transactional
    public Link updateMemo(String code, String memo) {
        Link link =
                linkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        link.setMemo(normalizeMemo(memo));
        return linkRepository.save(link);
    }

    private String normalizeMemo(String memo) {
        if (memo == null) {
            return null;
        }
        String trimmed = memo.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    @Transactional(readOnly = true)
    public List<Link> list() {
        return linkRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public Link getByCode(String code) {
        return linkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
    }

    /**
     * 리다이렉트 핫패스: 대상 URL을 돌려주고, 클릭 이벤트를 적재하며,
     * 실시간 대시보드로 클릭을 푸시한다.
     */
    @Transactional
    public String resolveAndRecord(String code, String country, String device, String referer) {
        Link link =
                linkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        if (link.isExpired()) {
            throw new LinkExpiredException(code);
        }

        linkRepository.incrementClickCount(link.getId());
        ClickEvent event =
                clickEventRepository.save(new ClickEvent(link.getId(), country, device, referer));

        long total = link.getClickCount() + 1;
        ClickEventDto dto = ClickEventDto.of(event, code, total);
        // 링크별 토픽 + 전역 피드 둘 다 푸시
        messaging.convertAndSend("/topic/clicks/" + code, dto);
        messaging.convertAndSend("/topic/clicks", dto);

        return link.getLongUrl();
    }

    /** 링크와 그 클릭 이벤트를 함께 삭제한다. */
    @Transactional
    public void delete(String code) {
        Link link =
                linkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        clickEventRepository.deleteByLinkId(link.getId());
        linkRepository.delete(link);
    }

    public String baseUrl() {
        return props.getBaseUrl();
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 5; i++) {
            String code = shortCodeGenerator.generate();
            if (!linkRepository.existsByCode(code)) {
                return code;
            }
        }
        // 극히 드문 연속 충돌 시 길이를 늘려 재시도
        return shortCodeGenerator.generate(9);
    }
}
