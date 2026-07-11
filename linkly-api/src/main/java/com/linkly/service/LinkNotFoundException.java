package com.linkly.service;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String code) {
        super("링크를 찾을 수 없습니다: " + code);
    }
}
