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
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final ClickEventRepository clickEventRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final SimpMessagingTemplate messaging;
    private final LinklyProperties props;

    public LinkService(
            LinkRepository linkRepository,
            ClickEventRepository clickEventRepository,
            ShortCodeGenerator shortCodeGenerator,
            SimpMessagingTemplate messaging,
            LinklyProperties props) {
        this.linkRepository = linkRepository;
        this.clickEventRepository = clickEventRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.messaging = messaging;
        this.props = props;
    }

    @Transactional
    public Link create(CreateLinkRequest request) {
        String code = generateUniqueCode();
        Instant expiresAt =
                request.expiresInDays() != null
                        ? Instant.now().plus(request.expiresInDays(), ChronoUnit.DAYS)
                        : null;
        Link link = new Link(code, request.url(), expiresAt);
        return linkRepository.save(link);
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
