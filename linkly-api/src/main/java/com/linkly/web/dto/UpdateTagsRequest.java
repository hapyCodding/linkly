package com.linkly.web.dto;

import java.util.List;

/** 링크 태그 전체 교체 요청. */
public record UpdateTagsRequest(List<String> tags) {
}
