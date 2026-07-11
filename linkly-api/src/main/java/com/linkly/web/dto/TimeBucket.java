package com.linkly.web.dto;

import java.time.Instant;

public record TimeBucket(Instant time, long count) {
}
