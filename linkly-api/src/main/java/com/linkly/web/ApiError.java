package com.linkly.web;

import java.time.Instant;

public record ApiError(int status, String message, Instant timestamp) {
}
