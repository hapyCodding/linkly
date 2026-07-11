package com.linkly.web;

import com.linkly.service.ClickAnalyticsService;
import com.linkly.service.LinkService;
import com.linkly.service.QrService;
import com.linkly.web.dto.CreateLinkRequest;
import com.linkly.web.dto.LinkResponse;
import com.linkly.web.dto.StatsResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService linkService;
    private final ClickAnalyticsService analyticsService;
    private final QrService qrService;

    public LinkController(
            LinkService linkService, ClickAnalyticsService analyticsService, QrService qrService) {
        this.linkService = linkService;
        this.analyticsService = analyticsService;
        this.qrService = qrService;
    }

    @PostMapping
    public LinkResponse create(@Valid @RequestBody CreateLinkRequest request) {
        return LinkResponse.of(linkService.create(request), linkService.baseUrl());
    }

    @GetMapping
    public List<LinkResponse> list() {
        String base = linkService.baseUrl();
        return linkService.list().stream().map(link -> LinkResponse.of(link, base)).toList();
    }

    @GetMapping("/{code}")
    public LinkResponse get(@PathVariable String code) {
        return LinkResponse.of(linkService.getByCode(code), linkService.baseUrl());
    }

    @GetMapping("/{code}/stats")
    public StatsResponse stats(@PathVariable String code) {
        return analyticsService.statsFor(code);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        linkService.delete(code);
    }

    @GetMapping(value = "/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String code) {
        // 코드가 실제 존재하는지 검증 후 QR 생성
        linkService.getByCode(code);
        byte[] png = qrService.pngBytes(linkService.baseUrl() + "/x/" + code);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
