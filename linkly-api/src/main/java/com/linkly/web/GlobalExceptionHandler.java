package com.linkly.web;

import com.linkly.service.LinkExpiredException;
import com.linkly.service.LinkNotFoundException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ApiError> notFound(LinkNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(HttpStatus.NOT_FOUND.value(), e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(LinkExpiredException.class)
    public ResponseEntity<ApiError> expired(LinkExpiredException e) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ApiError(HttpStatus.GONE.value(), e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        String msg =
                e.getBindingResult().getFieldErrors().stream()
                        .map(f -> f.getField() + ": " + f.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        if (msg.isBlank()) {
            msg = "잘못된 요청입니다";
        }
        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), msg, Instant.now()));
    }
}
