package com.linkly.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 대상 URL의 페이지 제목(og:title 우선, 없으면 &lt;title&gt;)을 가져온다.
 * 실패(타임아웃/비HTML/오류/내부주소)면 null 을 돌려 제목 없이 진행한다.
 */
@Component
public class TitleFetcher {

    private static final Logger log = LoggerFactory.getLogger(TitleFetcher.class);
    private static final int MAX_BODY = 200_000; // head 영역만 있으면 충분
    private static final int MAX_TITLE_LEN = 255; // DB 컬럼 길이

    private static final Pattern OG_PROP_FIRST =
            Pattern.compile(
                    "<meta[^>]+property=[\"']og:title[\"'][^>]*content=[\"']([^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_CONTENT_FIRST =
            Pattern.compile(
                    "<meta[^>]+content=[\"']([^\"']*)[\"'][^>]*property=[\"']og:title[\"']",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_TAG =
            Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

    public String fetchTitle(String url) {
        try {
            URI uri = URI.create(url);
            String blockReason = internalBlockReason(uri.getHost());
            if (blockReason != null) {
                log.warn("title 조회 생략({}): {}", url, blockReason);
                return null;
            }
            HttpRequest req =
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(6))
                            .header("User-Agent", "Mozilla/5.0 (compatible; LinklyBot/1.0)")
                            .header("Accept", "text/html,application/xhtml+xml")
                            .GET()
                            .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("title 조회 생략({}): HTTP {}", url, resp.statusCode());
                return null;
            }
            String contentType = resp.headers().firstValue("content-type").orElse("");
            if (!contentType.isEmpty() && !contentType.toLowerCase().contains("html")) {
                log.warn("title 조회 생략({}): content-type={}", url, contentType);
                return null;
            }
            String body = resp.body();
            if (body != null && body.length() > MAX_BODY) {
                body = body.substring(0, MAX_BODY);
            }
            String title = extractTitle(body);
            log.info("title 조회 성공({}): {}", url, title);
            return title;
        } catch (Exception e) {
            log.warn("title 조회 실패({}): {}", url, e.toString());
            return null;
        }
    }

    private String extractTitle(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        String title = firstGroup(OG_PROP_FIRST, html);
        if (title == null) {
            title = firstGroup(OG_CONTENT_FIRST, html);
        }
        if (title == null) {
            title = firstGroup(TITLE_TAG, html);
        }
        if (title == null) {
            return null;
        }
        title = unescapeHtml(title).replaceAll("\\s+", " ").trim();
        if (title.isEmpty()) {
            return null;
        }
        return title.length() > MAX_TITLE_LEN ? title.substring(0, MAX_TITLE_LEN) : title;
    }

    private String firstGroup(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private String unescapeHtml(String s) {
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&nbsp;", " ");
    }

    /** SSRF 방지: 차단 사유 문자열을 반환(허용이면 null). 진단 로그용으로 사유를 구분한다. */
    private String internalBlockReason(String host) {
        if (host == null || host.isBlank()) {
            return "host 없음";
        }
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (addr.isLoopbackAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isAnyLocalAddress()
                        || addr.isLinkLocalAddress()) {
                    return "내부/사설 IP(" + addr.getHostAddress() + ")";
                }
            }
            return null; // 허용
        } catch (Exception e) {
            return "DNS 해석 실패(" + e + ")"; // 해석 실패 시 안전하게 차단
        }
    }
}
