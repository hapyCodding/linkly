package com.linkly.web;

import com.linkly.service.LinkService;
import com.linkly.service.RequestMetaExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/** 단축 링크 리다이렉트 진입점: /x/{code} */
@RestController
public class RedirectController {

    private final LinkService linkService;
    private final RequestMetaExtractor meta;

    public RedirectController(LinkService linkService, RequestMetaExtractor meta) {
        this.linkService = linkService;
        this.meta = meta;
    }

    @GetMapping("/x/{code}")
    public RedirectView redirect(@PathVariable String code, HttpServletRequest request) {
        String target =
                linkService.resolveAndRecord(
                        code,
                        meta.country(request),
                        meta.device(request),
                        meta.referer(request));
        RedirectView view = new RedirectView(target);
        view.setStatusCode(HttpStatus.FOUND); // 302
        return view;
    }
}
