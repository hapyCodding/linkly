package com.linkly.service;

public class LinkExpiredException extends RuntimeException {
    public LinkExpiredException(String code) {
        super("만료된 링크입니다: " + code);
    }
}
